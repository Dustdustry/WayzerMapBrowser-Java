package mapbrowser.ui;

import arc.*;
import arc.flabel.*;
import arc.func.*;
import arc.math.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.util.*;
import mindustry.*;
import mindustry.graphics.*;
import mindustry.ui.*;

public class BrowserUI{
    public static TextField deboundTextField(String text, Cons<String> changed){
        return deboundTextField(text, changed, 0.5f);
    }

    public static TextField deboundTextField(String text, Cons<String> changed, float timeSeconds){
        if(Vars.mobile && !Core.input.useKeyboard()){
            return Elem.newField(text, changed);
        }

        return new DeboundTextField(text, timeSeconds, changed);
    }

    public static void infoToast(String text){
        infoToast(text, 1.5f);
    }

    public static void infoToast(String text, float duration){
        Table t = new Table(Styles.black3);
        t.touchable = Touchable.disabled;
        t.margin(16).add(text).style(Styles.outlineLabel).labelAlign(Align.center);

        t.update(t::toFront);

        t.pack();

        float y = Core.scene.getHeight() / 2;
        t.actions(
        Actions.moveToAligned(0, y, Align.right),
        Actions.moveToAligned(0, y, Align.left, 0.8f, Interp.pow4Out),
        Actions.delay(duration),
        Actions.parallel(
        Actions.moveToAligned(0, y, Align.right, 0.8f, Interp.pow4Out),
        Actions.fadeOut(0.8f, Interp.fade)
        ),
        Actions.remove()
        );

        t.act(0.1f);
        Core.scene.add(t);
    }

    public static void setLoadingText(Table table) {
        table.clearChildren();

        FLabel label = new FLabel(Core.bundle.get("wayzer-maps.loading"));
        table.add(label).color(Pal.lightishGray).fontScale(1.3f);
    }

    public static void setLoadFailedText(Table table) {
        table.clearChildren();

        FLabel label = new FLabel(Core.bundle.get("wayzer-maps.faild"));
        table.add(label).color(Pal.lightishGray).fontScale(1.3f).expand();
    }

    public static void setNoResultText(Table table) {
        table.clearChildren();

        FLabel label = new FLabel(Core.bundle.get("wayzer-maps.noResult"));
        table.add(label).color(Pal.lightishGray).fontScale(1.3f);
    }

    public static void setClipboard(String text){
        Core.app.setClipboardText(text);
        infoToast(Core.bundle.format("copy.hint", text), 4);
    }

    public static class DeboundTextField extends TextField{
        private boolean keeping;
        private final Timekeeper keeper;

        public DeboundTextField(String text, float seconds, Cons<String> deboundCons){
            setText(text);
            keeper = new Timekeeper(seconds);

            changed(() -> {
                keeping = true;
                keeper.reset();
            });

            addAction(Actions.forever(Actions.run(() -> {
                if(keeping && keeper.get()){
                    keeping = false;
                    deboundCons.get(getText());
                }
            })));
        }
    }
}
