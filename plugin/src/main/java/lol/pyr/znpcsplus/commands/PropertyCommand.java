package lol.pyr.znpcsplus.commands;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lol.pyr.director.adventure.command.CommandContext;
import lol.pyr.director.adventure.command.CommandHandler;
import lol.pyr.director.common.command.CommandExecutionException;
import lol.pyr.znpcsplus.api.entity.EntityProperty;
import lol.pyr.znpcsplus.api.npc.Npc;
import lol.pyr.znpcsplus.entity.EntityPropertyImpl;
import lol.pyr.znpcsplus.npc.ModeledNpcImpl;
import lol.pyr.znpcsplus.npc.NpcEntryImpl;
import lol.pyr.znpcsplus.npc.NpcRegistryImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.List;

public class PropertyCommand implements CommandHandler {
    private final NpcRegistryImpl npcRegistry;

    public PropertyCommand(NpcRegistryImpl npcRegistry) {
        this.npcRegistry = npcRegistry;
    }

    @Override
    public void run(CommandContext context) throws CommandExecutionException {
        context.setUsage(context.getLabel() + " property <id> <property> <value>");
        NpcEntryImpl entry = context.parse(NpcEntryImpl.class);
        Npc npc = entry.getNpc();
//        if (!(npc instanceof NpcImpl)) {
//            context.halt(Component.text("NPC " + entry.getId() + " cannot be have properties set", NamedTextColor.RED));
//        }
        EntityPropertyImpl<?> property = context.parse(EntityPropertyImpl.class);
        if (!(npc instanceof ModeledNpcImpl) && !npc.getType().getAllowedProperties().contains(property)) context.halt(Component.text("Property " + property.getName() + " not allowed for npc type " + npc.getType().getName(), NamedTextColor.RED));
        Class<?> type = property.getType();
        Object value;
        String valueName;
        if (type == ItemStack.class) {
            org.bukkit.inventory.ItemStack bukkitStack = context.ensureSenderIsPlayer().getInventory().getItemInHand();
            if (bukkitStack.getAmount() == 0) {
                value = null;
                valueName = "EMPTY";
            } else {
                value = SpigotConversionUtil.fromBukkitItemStack(bukkitStack);
                valueName = bukkitStack.toString();
            }
        }
        else if (type == NamedTextColor.class && context.argSize() < 1 && npc.getProperty(property) != null) {
            value = null;
            valueName = "NONE";
        }
        else {
            value = context.parse(type);
            valueName = String.valueOf(value);
        }

        npc.UNSAFE_setProperty(property, value);
        context.send(Component.text("Set property " + property.getName() + " for NPC " + entry.getId() + " to " + valueName, NamedTextColor.GREEN));
    }

    @Override
    public List<String> suggest(CommandContext context) throws CommandExecutionException {
        if (context.argSize() == 1) return context.suggestCollection(npcRegistry.getModifiableIds());
        if (context.argSize() == 2) return context.suggestStream(context.suggestionParse(0, NpcEntryImpl.class)
                    .getNpc().getType().getAllowedProperties().stream().map(EntityProperty::getName));
        if (context.argSize() == 3) {
            EntityPropertyImpl<?> property = context.suggestionParse(1, EntityPropertyImpl.class);
            Class<?> type = property.getType();
            if (type == Boolean.class) return context.suggestLiteral("true", "false");
            if (type == NamedTextColor.class) return context.suggestCollection(NamedTextColor.NAMES.keys());
        }
        return Collections.emptyList();
    }
}
