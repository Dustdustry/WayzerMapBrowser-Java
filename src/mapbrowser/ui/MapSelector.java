package mapbrowser.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.maps.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class MapSelector{
    public static void select(String title, Cons2<Map, Runnable> cons){
        // Thanks anuke :(
        CustomGameDialog dialog = new CustomGameDialog();
        Reflect.set(dialog, "dialog", new MapPlayDialog(){
            @Override
            public void show(Map map){
                cons.get(map, dialog::hide);
            }
        });

        dialog.title.setText(title);
        dialog.show();
    }
}
