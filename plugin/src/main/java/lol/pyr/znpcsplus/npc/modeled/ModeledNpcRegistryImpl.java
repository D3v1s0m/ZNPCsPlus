package lol.pyr.znpcsplus.npc.modeled;

import lol.pyr.znpcsplus.api.npc.NpcEntry;
import lol.pyr.znpcsplus.api.npc.NpcRegistry;
import lol.pyr.znpcsplus.api.npc.NpcType;
import lol.pyr.znpcsplus.config.ConfigManager;
import lol.pyr.znpcsplus.packets.PacketFactory;
import lol.pyr.znpcsplus.scheduling.TaskScheduler;
import lol.pyr.znpcsplus.storage.NpcStorage;
import lol.pyr.znpcsplus.util.NpcLocation;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.World;

import java.util.*;
import java.util.stream.Collectors;

public class ModeledNpcRegistryImpl implements NpcRegistry {
    private final NpcStorage storage;
    private final PacketFactory packetFactory;
    private final ConfigManager configManager;
    private final LegacyComponentSerializer textSerializer;

    private final List<ModeledNpcEntryImpl> modeledNpcList = new ArrayList<>();
    private final Map<String, ModeledNpcEntryImpl> modeledNpcIdLookupMap = new HashMap<>();
    private final Map<UUID, ModeledNpcEntryImpl> modeledNpcUuidLookupMap = new HashMap<>();

    public ModeledNpcRegistryImpl(ConfigManager configManager, NpcStorage storage, PacketFactory packetFactory, TaskScheduler scheduler, LegacyComponentSerializer textSerializer) {
        this.textSerializer = textSerializer;
        this.storage = storage;
        this.packetFactory = packetFactory;
        this.configManager = configManager;

        if (configManager.getConfig().autoSaveEnabled()) {
            long delay = configManager.getConfig().autoSaveInterval() * 20L;
            scheduler.runDelayedTimerAsync(this::save, delay, delay);
        }
    }

    public void save() {
        storage.saveModeledNpcs(modeledNpcList.stream().filter(ModeledNpcEntryImpl::isSave).collect(Collectors.toList()));
    }

    @Override
    public Collection<? extends NpcEntry> getAll() {
        return null;
    }

    @Override
    public Collection<String> getAllIds() {
        return null;
    }

    @Override
    public Collection<? extends NpcEntry> getAllPlayerMade() {
        return null;
    }

    @Override
    public Collection<String> getAllPlayerMadeIds() {
        return null;
    }

    @Override
    public NpcEntry create(String id, World world, NpcType type, NpcLocation location) {
        return null;
    }

    @Override
    public NpcEntry getById(String id) {
        return null;
    }

    @Override
    public NpcEntry getByUuid(UUID uuid) {
        return null;
    }

    @Override
    public void delete(String id) {

    }
}
