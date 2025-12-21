package mapbrowser;

import arc.struct.*;
import mapbrowser.api.BackendTypes.*;
import mindustry.*;

import java.lang.reflect.*;
import java.util.*;

public class BrowserVars{
    // magic
    public static final Seq<Field> multiplierRuleFields = new Seq<>();
    public static final Seq<Field> booleanRules = new Seq<>();

    public static final String[] versionTags = {"3", "4", "5", "7", "8", "9"};
    public static final String[] sortTags = {"updateTime", "createTime", "download", "rating", "like"};

    public static final String resourceSite = "https://www.mindustry.top";
    public static final String wayzerApi = "https://api.mindustry.top";

    public static boolean clientX;

    public static void init(){
        Field[] fields = SiteMapRules.class.getFields();
        multiplierRuleFields.addAll(fields)
        .retainAll(field -> field.getType() == float.class && field.getName().endsWith("Multiplier"));
        booleanRules.addAll(fields)
        .retainAll(field -> field.getType() == boolean.class);

        clientX = Vars.mods.locateMod("mindustryx") != null;
    }
}
