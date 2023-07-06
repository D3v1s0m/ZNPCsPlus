package lol.pyr.znpcsplus.npc.modeled;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.state.ModelState;
import com.ticxo.modelengine.api.entity.Dummy;
import com.ticxo.modelengine.api.model.ActiveModel;
import lol.pyr.znpcsplus.api.entity.EntityProperty;
import lol.pyr.znpcsplus.api.hologram.Hologram;
import lol.pyr.znpcsplus.api.npc.Npc;
import lol.pyr.znpcsplus.config.ConfigManager;
import lol.pyr.znpcsplus.hologram.HologramImpl;
import lol.pyr.znpcsplus.interaction.InteractionAction;
import lol.pyr.znpcsplus.packets.PacketFactory;
import lol.pyr.znpcsplus.util.NpcLocation;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ModeledNpcImpl extends Dummy implements Npc {
    private final PacketFactory packetFactory;
    private String worldName;
    private NpcLocation location;
    private ActiveModel model;
    private ModelState state = ModelState.IDLE;
    private boolean enabled = true;
    private final HologramImpl hologram;
    private final UUID uuid;

    private final List<InteractionAction> actions = new ArrayList<>();

    protected ModeledNpcImpl(UUID uuid, ConfigManager configManager, LegacyComponentSerializer textSerializer, World world, ActiveModel model, NpcLocation location, PacketFactory packetFactory) {
        this(uuid, configManager, packetFactory, textSerializer, world.getName(), model, location);
    }

    public ModeledNpcImpl(UUID uuid, ConfigManager configManager, PacketFactory packetFactory, LegacyComponentSerializer textSerializer, String world, ActiveModel model, NpcLocation location) {
        super(ModelEngineAPI.getEntityHandler().getEntityCounter().incrementAndGet(), uuid);
        this.packetFactory = packetFactory;
        this.worldName = world;
        this.location = location;
        this.uuid = uuid;
        this.model = model;
        hologram = new HologramImpl(configManager, packetFactory, textSerializer, location.withY(location.getY()));
    }

    public void setModel(ActiveModel model) {
        UNSAFE_hideAll();
        this.model = model;
        UNSAFE_showAll();
    }

    public ActiveModel getModel() {
        return model;
    }

    public void setState(ModelState state) {
        this.state = state;
    }

    public ModelState getState() {
        return state;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setNpcLocation(NpcLocation location) {
        this.location = location;
    }

    public NpcLocation getNpcLocation() {
        return location;
    }

    public Location getBukkitLocation() {
        return location.toBukkitLocation(getWorld());
    }

    public void setWorld(World world) {
        this.worldName = world.getName();
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public String getWorldName() {
        return worldName;
    }

    @Override
    public <T> T getProperty(EntityProperty<T> key) {
        return null;
    }

    @Override
    public boolean hasProperty(EntityProperty<?> key) {
        return false;
    }

    @Override
    public <T> void setProperty(EntityProperty<T> key, T value) {

    }

    @Override
    public Hologram getHologram() {
        return hologram;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    protected void UNSAFE_show(Player player) {
        ModelEngineAPI.getModeledEntity(uuid).showToPlayer(player);
    }

    protected void UNSAFE_hide(Player player) {
        ModelEngineAPI.getModeledEntity(uuid).hideFromPlayer(player);
    }

    private void UNSAFE_hideAll() {
        Set<Player> players = ModelEngineAPI.getModeledEntity(uuid).getRangeManager().getPlayerInRange();
        for (Player player : players) {
            UNSAFE_hide(player);
        }
    }

    private void UNSAFE_showAll() {
        Set<Player> players = ModelEngineAPI.getModeledEntity(uuid).getRangeManager().getPlayerInRange();
        for (Player player : players) {
            UNSAFE_show(player);
        }
    }
}
