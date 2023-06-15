package lol.pyr.znpcsplus.commands.hologram;

import lol.pyr.director.adventure.command.CommandContext;
import lol.pyr.director.adventure.command.CommandHandler;
import lol.pyr.director.common.command.CommandExecutionException;
import lol.pyr.znpcsplus.hologram.HologramImpl;
import lol.pyr.znpcsplus.npc.NpcEntryImpl;
import lol.pyr.znpcsplus.npc.NpcRegistryImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class HoloDeleteCommand implements CommandHandler {
    private final NpcRegistryImpl npcRegistry;

    public HoloDeleteCommand(NpcRegistryImpl npcRegistry) {
        this.npcRegistry = npcRegistry;
    }

    @Override
    public void run(CommandContext context) throws CommandExecutionException {
        context.setUsage(context.getLabel() + " holo delete <id> <line>");
        HologramImpl hologram = (HologramImpl) context.parse(NpcEntryImpl.class).getNpc().getHologram();
        int line = context.parse(Integer.class);
        if (line < 0 || line >= hologram.getLines().size()) context.halt(Component.text("Invalid line number!", NamedTextColor.RED));
        hologram.removeLine(line);
        context.send(Component.text("NPC line removed.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> suggest(CommandContext context) throws CommandExecutionException {
        if (context.argSize() == 1) return context.suggestCollection(npcRegistry.getModifiableIds());
        if (context.argSize() == 2) return context.suggestStream(Stream.iterate(0, n -> n + 1)
                .limit(((HologramImpl) context.suggestionParse(0, NpcEntryImpl.class).getNpc().getHologram()).getLines().size())
                .map(String::valueOf));
        return Collections.emptyList();
    }
}
