package lol.pyr.znpcsplus.commands;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import lol.pyr.director.adventure.command.CommandContext;
import lol.pyr.director.adventure.command.CommandHandler;
import lol.pyr.director.common.command.CommandExecutionException;
import lol.pyr.znpcsplus.api.skin.SkinDescriptor;
import lol.pyr.znpcsplus.entity.EntityPropertyRegistryImpl;
import lol.pyr.znpcsplus.npc.NpcEntryImpl;
import lol.pyr.znpcsplus.npc.NpcImpl;
import lol.pyr.znpcsplus.npc.NpcRegistryImpl;
import lol.pyr.znpcsplus.npc.NpcTypeRegistryImpl;
import lol.pyr.znpcsplus.skin.cache.MojangSkinCache;
import lol.pyr.znpcsplus.skin.descriptor.FetchingDescriptor;
import lol.pyr.znpcsplus.skin.descriptor.MirrorDescriptor;
import lol.pyr.znpcsplus.skin.descriptor.PrefetchedDescriptor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SkinCommand implements CommandHandler {
    private final MojangSkinCache skinCache;
    private final NpcRegistryImpl npcRegistry;
    private final NpcTypeRegistryImpl typeRegistry;
    private final EntityPropertyRegistryImpl propertyRegistry;

    public SkinCommand(MojangSkinCache skinCache, NpcRegistryImpl npcRegistry, NpcTypeRegistryImpl typeRegistry, EntityPropertyRegistryImpl propertyRegistry) {
        this.skinCache = skinCache;
        this.npcRegistry = npcRegistry;
        this.typeRegistry = typeRegistry;
        this.propertyRegistry = propertyRegistry;
    }

    @Override
    public void run(CommandContext context) throws CommandExecutionException {
        context.setUsage(context.getLabel() + " skin <id> <type> [value]");
        NpcImpl npc = context.parse(NpcEntryImpl.class).getNpc();
        if (npc.getType() != typeRegistry.getByEntityType(EntityTypes.PLAYER)) context.halt(Component.text("The NPC must be a player to have a skin", NamedTextColor.RED));
        String type = context.popString();

        if (type.equalsIgnoreCase("mirror")) {
            npc.setProperty(propertyRegistry.getByName("skin", SkinDescriptor.class), new MirrorDescriptor(skinCache));
            npc.respawn();
            context.halt(Component.text("The NPC's skin will now mirror the player that it's being displayed to", NamedTextColor.GREEN));
        } else if (type.equalsIgnoreCase("static")) {
            context.ensureArgsNotEmpty();
            String name = context.dumpAllArgs();
            context.send(Component.text("Fetching skin \"" + name + "\"...", NamedTextColor.GREEN));
            PrefetchedDescriptor.forPlayer(skinCache, name).thenAccept(skin -> {
                if (skin.getSkin() == null) {
                    context.send(Component.text("Failed to fetch skin, are you sure the player name is valid?", NamedTextColor.RED));
                    return;
                }
                npc.setProperty(propertyRegistry.getByName("skin", SkinDescriptor.class), skin);
                npc.respawn();
                context.send(Component.text("The NPC's skin has been set to \"" + name + "\"", NamedTextColor.GREEN));
            });
            return;
        } else if (type.equalsIgnoreCase("dynamic")) {
            context.ensureArgsNotEmpty();
            String name = context.dumpAllArgs();
            npc.setProperty(propertyRegistry.getByName("skin", SkinDescriptor.class), new FetchingDescriptor(skinCache, name));
            npc.respawn();
            context.halt(Component.text("The NPC's skin will now be resolved per-player from \"" + name + "\""));
        } else if (type.equalsIgnoreCase("url")) {
            context.ensureArgsNotEmpty();
            String variant = context.popString().toLowerCase();
            if (!variant.equalsIgnoreCase("slim") && !variant.equalsIgnoreCase("classic")) {
                context.send(Component.text("Invalid skin variant! Please use one of the following: slim, classic", NamedTextColor.RED));
                return;
            }
            String urlString = context.dumpAllArgs();
            try {
                URL url = new URL(urlString);
                context.send(Component.text("Fetching skin from url \"" + urlString + "\"...", NamedTextColor.GREEN));
                PrefetchedDescriptor.fromUrl(skinCache, url , variant).thenAccept(skin -> {
                    if (skin.getSkin() == null) {
                        context.send(Component.text("Failed to fetch skin, are you sure the url is valid?", NamedTextColor.RED));
                        return;
                    }
                    npc.setProperty(propertyRegistry.getByName("skin", SkinDescriptor.class), skin);
                    npc.respawn();
                    context.send(Component.text("The NPC's skin has been set.", NamedTextColor.GREEN));
                });
            } catch (MalformedURLException e) {
                context.send(Component.text("Invalid url!", NamedTextColor.RED));
            }
            return;
        } else if (type.equalsIgnoreCase("file")) {
            context.ensureArgsNotEmpty();
            String path = context.dumpAllArgs();
            context.send(Component.text("Fetching skin from file \"" + path + "\"...", NamedTextColor.GREEN));
            PrefetchedDescriptor.fromFile(skinCache, path).exceptionally(e -> {
                if (e instanceof FileNotFoundException || e.getCause() instanceof FileNotFoundException) {
                    context.send(Component.text("A file at the specified path could not be found!", NamedTextColor.RED));
                } else {
                    context.send(Component.text("An error occurred while fetching the skin from file! Check the console for more details.", NamedTextColor.RED));
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
                return null;
            }).thenAccept(skin -> {
                if (skin == null) return;
                if (skin.getSkin() == null) {
                    context.send(Component.text("Failed to fetch skin, are you sure the file path is valid?", NamedTextColor.RED));
                    return;
                }
                npc.setProperty(propertyRegistry.getByName("skin", SkinDescriptor.class), skin);
                npc.respawn();
                context.send(Component.text("The NPC's skin has been set.", NamedTextColor.GREEN));
            });
            return;
        }
        context.send(Component.text("Unknown skin type! Please use one of the following: mirror, static, dynamic, url"));
    }

    @Override
    public List<String> suggest(CommandContext context) throws CommandExecutionException {
        if (context.argSize() == 1) return context.suggestCollection(npcRegistry.getModifiableIds());
        if (context.argSize() == 2) return context.suggestLiteral("mirror", "static", "dynamic", "url", "file");
        if (context.matchSuggestion("*", "static")) return context.suggestPlayers();
        if (context.argSize() == 3 && context.matchSuggestion("*", "url")) {
            return context.suggestLiteral("slim", "classic");
        }
        if (context.argSize() == 3 && context.matchSuggestion("*", "file")) {
            if (skinCache.getSkinsFolder().exists()) {
                File[] files = skinCache.getSkinsFolder().listFiles();
                if (files != null) {
                    return Arrays.stream(files).map(File::getName).collect(Collectors.toList());
                }
            }
        }
        return Collections.emptyList();
    }
}
