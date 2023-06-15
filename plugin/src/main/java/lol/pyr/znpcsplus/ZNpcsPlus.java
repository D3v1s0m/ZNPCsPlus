package lol.pyr.znpcsplus;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lol.pyr.director.adventure.command.CommandContext;
import lol.pyr.director.adventure.command.CommandManager;
import lol.pyr.director.adventure.command.MultiCommand;
import lol.pyr.director.adventure.parse.primitive.BooleanParser;
import lol.pyr.director.adventure.parse.primitive.DoubleParser;
import lol.pyr.director.adventure.parse.primitive.IntegerParser;
import lol.pyr.director.common.message.Message;
import lol.pyr.znpcsplus.api.NpcApiProvider;
import lol.pyr.znpcsplus.api.interaction.InteractionType;
import lol.pyr.znpcsplus.api.npc.Npc;
import lol.pyr.znpcsplus.commands.*;
import lol.pyr.znpcsplus.commands.action.ActionAddCommand;
import lol.pyr.znpcsplus.commands.action.ActionDeleteCommand;
import lol.pyr.znpcsplus.commands.action.ActionEditCommand;
import lol.pyr.znpcsplus.commands.action.ActionListCommand;
import lol.pyr.znpcsplus.commands.hologram.*;
import lol.pyr.znpcsplus.commands.storage.LoadAllCommand;
import lol.pyr.znpcsplus.commands.storage.SaveAllCommand;
import lol.pyr.znpcsplus.config.ConfigManager;
import lol.pyr.znpcsplus.entity.EntityPropertyImpl;
import lol.pyr.znpcsplus.entity.EntityPropertyRegistryImpl;
import lol.pyr.znpcsplus.hologram.HologramImpl;
import lol.pyr.znpcsplus.interaction.ActionRegistry;
import lol.pyr.znpcsplus.interaction.InteractionPacketListener;
import lol.pyr.znpcsplus.metadata.*;
import lol.pyr.znpcsplus.npc.*;
import lol.pyr.znpcsplus.packets.*;
import lol.pyr.znpcsplus.parsers.*;
import lol.pyr.znpcsplus.scheduling.FoliaScheduler;
import lol.pyr.znpcsplus.scheduling.SpigotScheduler;
import lol.pyr.znpcsplus.scheduling.TaskScheduler;
import lol.pyr.znpcsplus.skin.cache.SkinCache;
import lol.pyr.znpcsplus.skin.cache.SkinCacheCleanTask;
import lol.pyr.znpcsplus.tasks.NpcProcessorTask;
import lol.pyr.znpcsplus.updater.UpdateChecker;
import lol.pyr.znpcsplus.updater.UpdateNotificationListener;
import lol.pyr.znpcsplus.user.UserListener;
import lol.pyr.znpcsplus.user.UserManager;
import lol.pyr.znpcsplus.util.*;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ZNpcsPlus extends JavaPlugin {
    private static final int PLUGIN_ID = 18244;

    private PacketEventsAPI<Plugin> packetEvents;
    private final LegacyComponentSerializer textSerializer = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors().build();

    private final List<Runnable> shutdownTasks = new ArrayList<>();
    private boolean enabled = false;

    @Override
    public void onLoad() {
        packetEvents = SpigotPacketEventsBuilder.build(this);
        PacketEvents.setAPI(packetEvents);
        packetEvents.getSettings().checkForUpdates(false);
        packetEvents.load();
    }

    private void log(String str) {
        Bukkit.getConsoleSender().sendMessage(str);
    }

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        log(ChatColor.YELLOW + "  ___       __   __  __");
        log(ChatColor.YELLOW + "   _/ |\\ | |__) |   (__` " + ChatColor.GOLD + "__|__   " + ChatColor.YELLOW + getDescription().getName() + " " + ChatColor.GOLD + "v" + getDescription().getVersion());
        log(ChatColor.YELLOW + "  /__ | \\| |    |__ .__) " + ChatColor.GOLD + "  |     " + ChatColor.GRAY + "Maintained with " + ChatColor.RED + "\u2764 " + ChatColor.GRAY + " by Pyr#6969");
        log("");

        PluginManager pluginManager = Bukkit.getPluginManager();

        if (pluginManager.isPluginEnabled("ServersNPC")) log(ChatColor.DARK_RED + " * Old version of znpcs detected! The plugin might not work correctly!");
        long before = System.currentTimeMillis();

        log(ChatColor.WHITE + " * Initializing libraries...");
        packetEvents.init();
        BukkitAudiences adventure = BukkitAudiences.create(this);

        log(ChatColor.WHITE + " * Initializing components...");
        TaskScheduler scheduler = FoliaUtil.isFolia() ? new FoliaScheduler(this) : new SpigotScheduler(this);
        MetadataFactory metadataFactory = setupMetadataFactory();
        ConfigManager configManager = new ConfigManager(getDataFolder());
        SkinCache skinCache = new SkinCache(configManager);
        EntityPropertyRegistryImpl propertyRegistry = new EntityPropertyRegistryImpl(skinCache);
        PacketFactory packetFactory = setupPacketFactory(scheduler, metadataFactory, propertyRegistry);
        BungeeConnector bungeeConnector = new BungeeConnector(this);
        ActionRegistry actionRegistry = new ActionRegistry();
        NpcTypeRegistryImpl typeRegistry = new NpcTypeRegistryImpl();
        NpcRegistryImpl npcRegistry = new NpcRegistryImpl(configManager, this, packetFactory, actionRegistry, scheduler, typeRegistry, propertyRegistry, textSerializer);
        UserManager userManager = new UserManager();

        log(ChatColor.WHITE + " * Registerring components...");
        typeRegistry.registerDefault(packetEvents, propertyRegistry);
        actionRegistry.registerTypes(scheduler, adventure, bungeeConnector, textSerializer);
        packetEvents.getEventManager().registerListener(new InteractionPacketListener(userManager, npcRegistry), PacketListenerPriority.MONITOR);
        new Metrics(this, PLUGIN_ID);
        pluginManager.registerEvents(new UserListener(userManager), this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        registerCommands(npcRegistry, skinCache, adventure, actionRegistry, typeRegistry, propertyRegistry);

        log(ChatColor.WHITE + " * Starting tasks...");
        if (configManager.getConfig().checkForUpdates()) {
            UpdateChecker updateChecker = new UpdateChecker(this.getDescription());
            scheduler.runDelayedTimerAsync(updateChecker, 5L, 6000L);
            pluginManager.registerEvents(new UpdateNotificationListener(this, adventure, updateChecker), this);
        }

        scheduler.runDelayedTimerAsync(new NpcProcessorTask(npcRegistry, configManager, propertyRegistry), 60L, 3L);
        scheduler.runDelayedTimerAsync(new SkinCacheCleanTask(skinCache), 1200, 1200);

        log(ChatColor.WHITE + " * Loading data...");
        npcRegistry.reload();

        shutdownTasks.add(scheduler::cancelAll);
        shutdownTasks.add(userManager::shutdown);
        shutdownTasks.add(adventure::close);
        if (configManager.getConfig().autoSaveEnabled()) shutdownTasks.add(npcRegistry::save);

        NpcApiProvider.register(this, new ZNpcsPlusApi(npcRegistry, typeRegistry, propertyRegistry));
        enabled = true;
        log(ChatColor.WHITE + " * Loading complete! (" + (System.currentTimeMillis() - before) + "ms)");
        log("");

        if (configManager.getConfig().debugEnabled()) {
            World world = Bukkit.getWorld("world");
            if (world == null) world = Bukkit.getWorlds().get(0);
            int i = 0;
            for (NpcTypeImpl type : typeRegistry.getAll()) {
                NpcEntryImpl entry = npcRegistry.create("debug_npc_" + i, world, type, new NpcLocation(i * 3, 200, 0, 0, 0));
                entry.setProcessed(true);
                Npc npc = entry.getNpc();
                ((HologramImpl) npc.getHologram()).addLineComponent(Component.text("Hello, World!"));
                npc.setProperty(propertyRegistry.getByName("look", Boolean.class), true);
                i++;
            }
        }
    }

    @Override
    public void onDisable() {
        if (!enabled) return;
        NpcApiProvider.unregister();
        for (Runnable runnable : shutdownTasks) runnable.run();
    }

    private PacketFactory setupPacketFactory(TaskScheduler scheduler, MetadataFactory metadataFactory, EntityPropertyRegistryImpl propertyRegistry) {
        HashMap<ServerVersion, LazyLoader<? extends PacketFactory>> versions = new HashMap<>();
        versions.put(ServerVersion.V_1_8, LazyLoader.of(() -> new V1_8PacketFactory(scheduler, metadataFactory, packetEvents, propertyRegistry)));
        versions.put(ServerVersion.V_1_9, LazyLoader.of(() -> new V1_9PacketFactory(scheduler, metadataFactory, packetEvents, propertyRegistry)));
        versions.put(ServerVersion.V_1_10, LazyLoader.of(() -> new V1_10PacketFactory(scheduler, metadataFactory, packetEvents, propertyRegistry)));
        versions.put(ServerVersion.V_1_14, LazyLoader.of(() -> new V1_14PacketFactory(scheduler, metadataFactory, packetEvents, propertyRegistry)));
        versions.put(ServerVersion.V_1_19, LazyLoader.of(() -> new V1_19PacketFactory(scheduler, metadataFactory, packetEvents, propertyRegistry)));

        ServerVersion version = packetEvents.getServerManager().getVersion();
        if (versions.containsKey(version)) return versions.get(version).get();
        for (ServerVersion v : ServerVersion.reversedValues()) {
            if (v.isNewerThan(version)) continue;
            if (!versions.containsKey(v)) continue;
            return versions.get(v).get();
        }
        throw new RuntimeException("Unsupported version!");
    }

    private MetadataFactory setupMetadataFactory() {
        HashMap<ServerVersion, LazyLoader<? extends MetadataFactory>> versions = new HashMap<>();
        versions.put(ServerVersion.V_1_8, LazyLoader.of(V1_8MetadataFactory::new));
        versions.put(ServerVersion.V_1_9, LazyLoader.of(V1_9MetadataFactory::new));
        versions.put(ServerVersion.V_1_10, LazyLoader.of(V1_10MetadataFactory::new));
        versions.put(ServerVersion.V_1_13, LazyLoader.of(V1_13MetadataFactory::new));
        versions.put(ServerVersion.V_1_14, LazyLoader.of(V1_14MetadataFactory::new));
        versions.put(ServerVersion.V_1_16, LazyLoader.of(V1_16MetadataFactory::new));
        versions.put(ServerVersion.V_1_17, LazyLoader.of(V1_17MetadataFactory::new));

        ServerVersion version = packetEvents.getServerManager().getVersion();
        if (versions.containsKey(version)) return versions.get(version).get();
        for (ServerVersion v : ServerVersion.reversedValues()) {
            if (v.isNewerThan(version)) continue;
            if (!versions.containsKey(v)) continue;
            return versions.get(v).get();
        }
        throw new RuntimeException("Unsupported version!");
    }


    private void registerCommands(NpcRegistryImpl npcRegistry, SkinCache skinCache, BukkitAudiences adventure, ActionRegistry actionRegistry, NpcTypeRegistryImpl typeRegistry, EntityPropertyRegistryImpl propertyRegistry) {
        Message<CommandContext> incorrectUsageMessage = context -> context.send(Component.text("Incorrect usage: /" + context.getUsage(), NamedTextColor.RED));
        CommandManager manager = new CommandManager(this, adventure, incorrectUsageMessage);

        manager.registerParser(NpcTypeImpl.class, new NpcTypeParser(incorrectUsageMessage, typeRegistry));
        manager.registerParser(NpcEntryImpl.class, new NpcEntryParser(npcRegistry, incorrectUsageMessage));
        manager.registerParser(EntityPropertyImpl.class, new EntityPropertyParser(incorrectUsageMessage, propertyRegistry));
        manager.registerParser(Integer.class, new IntegerParser(incorrectUsageMessage));
        manager.registerParser(Double.class, new DoubleParser(incorrectUsageMessage));
        manager.registerParser(Boolean.class, new BooleanParser(incorrectUsageMessage));
        manager.registerParser(NamedTextColor.class, new NamedTextColorParser(incorrectUsageMessage));
        manager.registerParser(InteractionType.class, new InteractionTypeParser(incorrectUsageMessage));

        manager.registerCommand("npc", new MultiCommand(loadHelpMessage("root"))
                .addSubcommand("create", new CreateCommand(npcRegistry, typeRegistry))
                .addSubcommand("skin", new SkinCommand(skinCache, npcRegistry, typeRegistry, propertyRegistry))
                .addSubcommand("delete", new DeleteCommand(npcRegistry, adventure))
                .addSubcommand("move", new MoveCommand(npcRegistry))
                .addSubcommand("property", new PropertyCommand(npcRegistry))
                .addSubcommand("teleport", new TeleportCommand(npcRegistry))
                .addSubcommand("list", new ListCommand(npcRegistry))
                .addSubcommand("near", new NearCommand(npcRegistry))
                .addSubcommand("type", new TypeCommand(npcRegistry, typeRegistry))
                .addSubcommand("tocustommodel", new ToCustomModelCommand(npcRegistry, adventure))
                .addSubcommand("storage", new MultiCommand(loadHelpMessage("storage"))
                        .addSubcommand("save", new SaveAllCommand(npcRegistry))
                        .addSubcommand("reload", new LoadAllCommand(npcRegistry)))
                .addSubcommand("holo", new MultiCommand(loadHelpMessage("holo"))
                        .addSubcommand("add", new HoloAddCommand(npcRegistry, textSerializer))
                        .addSubcommand("delete", new HoloDeleteCommand(npcRegistry))
                        .addSubcommand("info", new HoloInfoCommand(npcRegistry))
                        .addSubcommand("insert", new HoloInsertCommand(npcRegistry, textSerializer))
                        .addSubcommand("set", new HoloSetCommand(npcRegistry, textSerializer))
                        .addSubcommand("offset", new HoloOffsetCommand(npcRegistry)))
                .addSubcommand("action", new MultiCommand(loadHelpMessage("action"))
                        .addSubcommand("add", new ActionAddCommand(npcRegistry, actionRegistry))
                        .addSubcommand("delete", new ActionDeleteCommand(npcRegistry))
                        .addSubcommand("edit", new ActionEditCommand(npcRegistry, actionRegistry))
                        .addSubcommand("list", new ActionListCommand(npcRegistry)))
        );
    }

    private Message<CommandContext> loadHelpMessage(String name) {
        Reader reader = getTextResource("help-messages/" + name + ".txt");
        if (reader == null) throw new RuntimeException(name + ".txt is missing from the help-messages folder in the ZNPCsPlus jar!");
        Component component = MiniMessage.miniMessage().deserialize(FileUtil.dumpReaderAsString(reader));
        return context -> context.send(component);
    }
}
