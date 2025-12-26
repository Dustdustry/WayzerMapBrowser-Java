package mapbrowser.ui;

import arc.*;
import arc.input.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mapbrowser.*;
import mapbrowser.ui.listener.DragListener;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustryX.features.ui.*;

public class BrowserButton{
    private static Table table;
    private static Label label;
    private static BrowserDialog browser;

    public static void init(){
        Vars.ui.settings.graphics.checkPref("wayzer-maps-show", true);

        browser = new BrowserDialog();

        setupTable();

        if(BrowserVars.clientX){
            OverlayUI.INSTANCE.registerWindow("wayzer-maps", table);
        }else{
            Core.scene.add(table);
            // 初始位置
            table.setPosition(0, Core.scene.getHeight() / 2, Align.left);
            label.addListener(new DragListener(table));
        }
    }

    private static void setupTable(){
        table = new Table();
        table.name = "wayzer-maps";
        table.background(Tex.pane);
        table.touchable = Touchable.childrenOnly;
        table.visibility = () -> Vars.ui.hudfrag.shown && !Vars.ui.minimapfrag.shown() && Core.settings.getBool("wayzer-maps-show");

        table.defaults().expandX();

        label = table.add("WayzerMaps").get();
        table.row();

        // 添加快捷键提示
        if(Core.app.isDesktop()){
            table.add(Core.bundle.format("wayzer-maps.open-keycode-hint", KeyCode.w, KeyCode.z)).row();
        }

        TextureRegionDrawable icon = new TextureRegionDrawable(UnitTypes.oct.uiIcon);
        table.button(icon, Styles.clearNonei, 64, () -> browser.show()).height(64).padTop(8);

        if(Core.app.isDesktop()){
            table.update(() -> {
                if(!Core.scene.hasField() && keyValid()){
                    browser.show();
                }
            });
        }

        table.pack();
    }

    private static boolean keyValid(){
        Input input = Core.input;
        return (input.keyDown(KeyCode.w) && input.keyRelease(KeyCode.z)) ||
        (input.keyDown(KeyCode.z) && input.keyRelease(KeyCode.w));
    }
}
