package lol.pyr.znpcsplus.storage;

import lol.pyr.znpcsplus.npc.NpcEntryImpl;
import lol.pyr.znpcsplus.npc.modeled.ModeledNpcEntryImpl;

import java.util.Collection;

public interface NpcStorage {
    Collection<NpcEntryImpl> loadNpcs();
    Collection<ModeledNpcEntryImpl> loadModeledNpcs();
    void saveNpcs(Collection<NpcEntryImpl> npcs);
    void saveModeledNpcs(Collection<ModeledNpcEntryImpl> modeledNpcs);
}
