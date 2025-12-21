package mapbrowser;

import arc.*;
import arc.input.*;
import arc.util.*;
import mapbrowser.ui.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import mindustry.mod.Mods.*;
import mindustry.type.*;
import mindustry.ui.dialogs.*;

public class Main extends Mod{

    public Main(){
        Events.on(ClientLoadEvent.class, e -> {
            LoadedMod mod = Vars.mods.getMod(Main.class);
            mod.meta.description = mod.root.child("description").readString();

            BrowserVars.init();

//            Log.info("Use X: @", BrowserVars.clientX);
            BrowserButton.init();
        });
    }
}
