package lol.pyr.znpcsplus.commands;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import lol.pyr.director.adventure.command.CommandContext;
import lol.pyr.director.adventure.command.CommandHandler;
import lol.pyr.director.common.command.CommandExecutionException;
import lol.pyr.znpcsplus.api.npc.Npc;
import lol.pyr.znpcsplus.api.skin.SkinDescriptor;
import lol.pyr.znpcsplus.entity.EntityPropertyRegistryImpl;
import lol.pyr.znpcsplus.npc.NpcEntryImpl;
import lol.pyr.znpcsplus.npc.NpcRegistryImpl;
import lol.pyr.znpcsplus.npc.NpcTypeRegistryImpl;
import lol.pyr.znpcsplus.skin.cache.SkinCache;
import lol.pyr.znpcsplus.skin.descriptor.FetchingDescriptor;
import lol.pyr.znpcsplus.skin.descriptor.MirrorDescriptor;
import lol.pyr.znpcsplus.skin.descriptor.PrefetchedDescriptor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.List;

public class SkinCommand implements CommandHandler {
    private final SkinCache skinCache;
    private final NpcRegistryImpl npcRegistry;
    private final NpcTypeRegistryImpl typeRegistry;
    private final EntityPropertyRegistryImpl propertyRegistry;

    public SkinCommand(SkinCache skinCache, NpcRegistryImpl npcRegistry, NpcTypeRegistryImpl typeRegistry, EntityPropertyRegistryImpl propertyRegistry) {
        this.skinCache = skinCache;
        this.npcRegistry = npcRegistry;
        this.typeRegistry = typeRegistry;
        this.propertyRegistry = propertyRegistry;
    }

    @Override
    public void run(CommandContext context) throws CommandExecutionException {
        context.setUsage(context.getLabel() + " skin <id> <type> [value]");
        Npc npc = context.parse(NpcEntryImpl.class).getNpc();
        if (npc.getType() != typeRegistry.getByEntityType(EntityTypes.PLAYER)) context.halt(Component.text("The NPC must be a player to have a skin", NamedTextColor.RED));
        String type = context.popString();

        if (type.equalsIgnoreCase("mirror")) {
            npc.setProperty(propertyRegistry.getByName("skin", SkinDescriptor.class), new MirrorDescriptor(skinCache));
            npc.respawn();
            context.halt(Component.text("The NPC's skin will now mirror the player that it's being displayed to", NamedTextColor.GREEN));
        }

        if (type.equalsIgnoreCase("static")) {
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
                context.send(Component.text("The NPC's skin has been set to \"" + name + "\""));
            });
            return;
        }

        if (type.equalsIgnoreCase("dynamic")) {
            context.ensureArgsNotEmpty();
            String name = context.dumpAllArgs();
            npc.setProperty(propertyRegistry.getByName("skin", SkinDescriptor.class), new FetchingDescriptor(skinCache, name));
            npc.respawn();
            context.halt(Component.text("The NPC's skin will now be resolved per-player from \"" + name + "\""));
        }
        context.send(Component.text("Unknown skin type! Please use one of the following: mirror, static, dynamic"));
    }

    @Override
    public List<String> suggest(CommandContext context) throws CommandExecutionException {
        if (context.argSize() == 1) return context.suggestCollection(npcRegistry.getModifiableIds());
        if (context.argSize() == 2) return context.suggestLiteral("mirror", "static", "dynamic");
        if (context.matchSuggestion("*", "static")) return context.suggestPlayers();
        return Collections.emptyList();
    }
}
