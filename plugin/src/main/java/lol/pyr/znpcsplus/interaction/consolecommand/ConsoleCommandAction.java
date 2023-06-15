package lol.pyr.znpcsplus.interaction.consolecommand;

import lol.pyr.znpcsplus.api.interaction.InteractionAction;
import lol.pyr.znpcsplus.api.interaction.InteractionType;
import lol.pyr.znpcsplus.scheduling.TaskScheduler;
import lol.pyr.znpcsplus.util.PapiUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConsoleCommandAction extends InteractionAction {
    private final TaskScheduler scheduler;
    private final String command;

    public ConsoleCommandAction(TaskScheduler scheduler, String command, InteractionType interactionType, long delay) {
        super(delay, interactionType);
        this.scheduler = scheduler;
        this.command = command;
    }

    @Override
    public void run(Player player) {
        String cmd = command.replace("{player}", player.getName()).replace("{uuid}", player.getUniqueId().toString());
        scheduler.runSyncGlobal(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PapiUtil.set(player, cmd)));
    }

    @Override
    public Component getInfo(String id, int index, String label) {
        return Component.text(index + ") ", NamedTextColor.GOLD)
                .append(Component.text("[EDIT]", NamedTextColor.DARK_GREEN)
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.text("Click to edit this action", NamedTextColor.GRAY)))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                "/" + label + " action edit " + id + " " + index + " consolecommand " + " " + getInteractionType().name() + " " + getCooldown()/1000 + " " + command))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("[DELETE]", NamedTextColor.RED)
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.text("Click to delete this action", NamedTextColor.GRAY)))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                "/" + label + " action delete " + id + " " + index)))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("Console Command: ", NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.text("Click Type: " + getInteractionType().name() + " Cooldown: " + getCooldown()/1000, NamedTextColor.GREEN))))
                .append(Component.text(command, NamedTextColor.WHITE)));
    }

    public String getCommand() {
        return command;
    }
}
