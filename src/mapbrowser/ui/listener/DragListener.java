package mapbrowser.ui.listener;

import arc.scene.*;
import arc.scene.event.*;

public class DragListener extends ClickListener{
    private final Element handle;
    private final boolean save;

    public DragListener(Element handle, boolean save){
        this.handle = handle;
        this.save = save;
    }
}
