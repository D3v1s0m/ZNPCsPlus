package lol.pyr.znpcsplus.interaction.message;

import lol.pyr.znpcsplus.api.interaction.InteractionAction;
import lol.pyr.znpcsplus.api.interaction.InteractionType;
import lol.pyr.znpcsplus.util.PapiUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class MessageAction extends InteractionAction {
    private final BukkitAudiences adventure;
    private final String message;
    private final LegacyComponentSerializer textSerializer;

    public MessageAction(BukkitAudiences adventure, String message, InteractionType interactionType, LegacyComponentSerializer textSerializer, long delay) {
        super(delay, interactionType);
        this.adventure = adventure;
        this.message = message;
        this.textSerializer = textSerializer;
    }

    @Override
    public void run(Player player) {
        String msg = message.replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString());
        adventure.player(player).sendMessage(textSerializer.deserialize(PapiUtil.set(player, msg)));
    }

    @Override
    public Component getInfo(String id, int index, String label) {
        return Component.text(index + ") ", NamedTextColor.GOLD)
                .append(Component.text("[EDIT]", NamedTextColor.DARK_GREEN)
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.text("Click to edit this action", NamedTextColor.GRAY)))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                "/" + label + " action edit " + id + " " + index + " message " + getInteractionType().name() + " " + getCooldown()/1000 + " " + message))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("[DELETE]", NamedTextColor.RED)
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.text("Click to delete this action", NamedTextColor.GRAY)))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                "/" + label + " action delete " + id + " " + index)))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("Message: ", NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.text("Click Type: " + getInteractionType().name() + " Cooldown: " + getCooldown()/1000, NamedTextColor.GREEN))))
                .append(Component.text(message, NamedTextColor.WHITE)));
    }

    public String getMessage() {
        return message;
    }
}
