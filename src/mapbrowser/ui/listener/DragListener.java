package mapbrowser.ui.listener;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;

public class DragListener extends ClickListener {
    private static final float alignmentRange = 5f;
    private static final Seq<DragListener> allListeners = new Seq<>();
    private static final Seq<Element> alignmentElements = new Seq<>();
    private static final FloatSeq horizontalLines = new FloatSeq();
    private static final FloatSeq verticalLines = new FloatSeq();

    private static Element background;
    private static Element target;

    private final Element element;
    private final boolean save;

    private float lastSceneWidth = Core.scene.getWidth();
    private float lastSceneHeight = Core.scene.getHeight();
    private float startX, startY;

    static {
        Events.on(ClientLoadEvent.class, e -> setAlignElems());
        Events.on(ResizeEvent.class, e -> {
            setAlignElems();
            float w = Core.scene.getWidth();
            float h = Core.scene.getHeight();
            allListeners.each(l -> l.resized(w, h));
        });
    }

    public DragListener(Element element){
        this(element, true, true);
    }

    public DragListener(Element element, boolean save, boolean alignable) {
        this.element = element;
        this.save = save;

        if (save) readPosition();
        if (alignable) alignmentElements.add(element);

        allListeners.add(this);
    }

    private static void setAlignElems() {
        if (Vars.ui == null) return;

        var hud = Vars.ui.hudfrag;
        var group = Vars.ui.hudGroup;

        // 尝试反射获取 mainStack (放置面板)
        Element mainStack = null;
        try {
            mainStack = (Element) Reflect.get(hud.blockfrag, "mainStack");
        } catch (Exception ignored) {}

        alignmentElements.clear();
        alignmentElements.addAll(
        mainStack,
        group.find("waves"),
        group.find("minimap"),
        group.find("position")
        );

        // 核心资源显示
        Element coreInfo = group.find("coreinfo");
        if(coreInfo instanceof Table t && t.getChildren().size > 1){
            var table2 = t.getChildren().get(1);
            if(table2 instanceof Group g && g.getChildren().size > 0){
                alignmentElements.add(g.getChildren().get(0));
            }
        }
    }

    public void resized(float width, float height) {
        if (save) readPosition();

        float newX = width * element.x / lastSceneWidth;
        float newY = height * element.y / lastSceneHeight;

        element.setPosition(newX, newY);
        element.keepInStage();

        lastSceneWidth = width;
        lastSceneHeight = height;
    }

    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
        enableEdit(element);
        startX = x;
        startY = y;
        return true;
    }

    @Override
    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
        if (save) savePosition();
        disableEdit();
    }

    @Override
    public void touchDragged(InputEvent event, float dragX, float dragY, int pointer) {
        horizontalLines.clear();
        verticalLines.clear();

        element.moveBy(dragX - startX, dragY - startY);
        updateDragAlign();
        element.keepInStage();
    }

    private void updateDragAlign() {
        float tx = element.x, ty = element.y;
        float tw = element.getWidth(), th = element.getHeight();

        float tright = tx + tw;
        float ttop = ty + th;

        for (Element other : alignmentElements) {
            if (element == other || !other.visible) continue;

            Vec2 v1 = other.localToStageCoordinates(Tmp.v1.set(0, 0));
            float left = v1.x, right = v1.x + other.getWidth();
            float bottom = v1.y, top = v1.y + other.getHeight();

            // X轴对齐
            if (Math.abs(tx - left) <= alignmentRange) {
                element.x = left; verticalLines.add(left);
            } else if (Math.abs(tright - left) <= alignmentRange) {
                element.x = left - tw; verticalLines.add(left);
            } else if (Math.abs(tx - right) <= alignmentRange) {
                element.x = right; verticalLines.add(right);
            } else if (Math.abs(tright - right) <= alignmentRange) {
                element.x = right - tw; verticalLines.add(right);
            }

            // Y轴对齐
            if (Math.abs(ttop - bottom) <= alignmentRange) {
                element.y = bottom - th; horizontalLines.add(bottom);
            } else if (Math.abs(ty - bottom) <= alignmentRange) {
                element.y = bottom; horizontalLines.add(bottom);
            } else if (Math.abs(ttop - top) <= alignmentRange) {
                element.y = top - th; horizontalLines.add(top);
            } else if (Math.abs(ty - top) <= alignmentRange) {
                element.y = top; horizontalLines.add(top);
            }
        }
    }

    private void savePosition() {
        if (element.name == null) return;
        Core.settings.put("ui-" + element.name + "-lastPosition-x", element.x);
        Core.settings.put("ui-" + element.name + "-lastPosition-y", element.y);
    }

    private void readPosition() {
        if (element.name == null) return;
        float x = Core.settings.getFloat("ui-" + element.name + "-lastPosition-x", element.x);
        float y = Core.settings.getFloat("ui-" + element.name + "-lastPosition-y", element.y);
        element.setPosition(x, y);
        element.keepInStage();
    }

    private static void enableEdit(Element e) {
        if (background == null) initBackground();
        target = e;
        Core.scene.add(background);
    }

    private static void disableEdit() {
        target = null;
        if (background != null) background.remove();
        horizontalLines.clear();
        verticalLines.clear();
    }

    private static void initBackground() {
        background = new Element() {
            @Override
            public void draw() {
                Draw.color(Color.black, 0.2f);
                Fill.rect(x + width / 2, y + height / 2, width, height);

                Draw.color(Pal.accent, 0.8f);
                for(float ly : horizontalLines.items){
                    Lines.line(0, ly, width, ly);
                }

                Lines.stroke(1.2f);
                for(float lx : verticalLines.items){
                    Lines.line(lx, 0, lx, height);
                }

                Draw.color(Color.sky);
                for (Element e : alignmentElements) {
                    if (e == target || !e.visible) continue;
                    Vec2 v = e.localToStageCoordinates(Tmp.v1.set(0, 0));
                    Lines.rect(v.x, v.y, e.getWidth(), e.getHeight());
                }

                Lines.stroke(1.5f);

                // 绘制当前目标边框
                if (target != null) {
                    Draw.color(Pal.accent, 0.9f);
                    Lines.rect(target.x, target.y, target.getWidth(), target.getHeight());
                }
                Draw.reset();
            }
        };
        background.touchable = Touchable.disabled;
        background.setFillParent(true);
    }
}