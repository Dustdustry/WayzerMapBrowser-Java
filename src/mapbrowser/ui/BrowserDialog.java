package mapbrowser.ui;

import arc.Core;
import arc.files.*;
import arc.flabel.*;
import arc.input.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mapbrowser.*;
import mapbrowser.api.*;
import mapbrowser.api.BackendTypes.*;
import mindustry.Vars;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.maps.*;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.fragments.*;

import java.lang.reflect.*;

public class BrowserDialog extends BaseDialog{
    public static final boolean enableLogin = false;

    private static final Seq<SelectTag> selectTags = Seq.with(
    new SelectTag("@mode", BackendTypes.SiteGameMode.values()),
    new SelectTag("@version", BrowserVars.versionTags),
    new SelectTag("@sort", BrowserVars.sortTags)
    );
    private static final int maxRequest = 200;
    private static final int count = 14;
    private static final int maxPage = (int)Math.ceil(maxRequest / (double)count);

    private static final KeyCode lastPageKeyCode = KeyCode.leftBracket;
    private static final KeyCode nextPageKeyCode = KeyCode.rightBracket;

    private static final float height = 270f;
    private static final float imageSize = 176f;
    private static final float buttonSize = 40f;

    private int columns = 3;
    private float width = 510f;

    private final Seq<SiteMapInfo> mapData = new Seq<>();

    private SiteUser user;
    private TextField field, userField;
    private final Table mapTable = new Table(), searchTable = new Table(), userTable = new Table();
    private final DetailsDialog detailsDialog = new DetailsDialog();

    private String queryText = "";
    private String userTag = "";

    private int page = 0;
    private final Timekeeper pageTimer = new Timekeeper(0.25f);

    public BrowserDialog(){
        super("@wayzer-maps.browser");

        shown(this::rebuild);
        onResize(this::rebuild);
        closeOnBack();

        setupButtons();
        keyDown(lastPageKeyCode, () -> {
            if(Core.scene.getKeyboardFocus() != this) return;
            shiftPage(-1);
        });

        keyDown(nextPageKeyCode, () -> {
            if(Core.scene.getKeyboardFocus() != this) return;
            shiftPage(1);
        });
    }

    private void login(){
        if(!enableLogin) return;

        if(user == null){
            String uuid = Core.settings.getString("uuid", "");
            if(!uuid.isEmpty() && uuid.length() % 4 == 0){
                user = new SiteUser(Version.buildString(), uuid);
            }
        }

        BaseDialog loginDialog = new BaseDialog(Core.bundle.get("wayzer-maps.login-checking"));
        Table cont = loginDialog.cont;

        FLabel flabel = cont.add(new FLabel(Core.bundle.get("wayzer-maps.code-waiting"))).get();
        cont.row();

        loginDialog.show();

        user.login((url, callback) -> {
            loginDialog.hide();
            Vars.ui.showConfirm(Core.bundle.format("wayzer-maps.login-tip", url), () -> {
                if(Core.app.openURI(url)){
                    callback.run();

                    flabel.setText(Core.bundle.get("wayzer-maps.login-waiting"));
                    cont.button(Core.bundle.get("wayzer-maps.open-browser"), () -> {
                        Core.app.openURI(url);
                    }).minWidth(240).padTop(24);

                    loginDialog.show();
                }
            });
        }, token -> {
            cont.clear();
            cont.add(Core.bundle.get("wayzer-maps.login-success")).row();
            cont.button(Core.bundle.get("confirm"), loginDialog::hide).minWidth(180).padTop(24);
            loginDialog.addCloseListener();
        }, (err) -> {
            loginDialog.hide();
            Vars.ui.showException(Core.bundle.get("wayzer-maps.login-faild"), err);
        });

        loginDialog.hidden(() -> {
            userTable.clear();
            setupUserTable(userTable);
        });
    }

    private void shiftPage(int num){
        if(!pageTimer.get()){
            return;
        }

        page = Mathf.clamp(page + num, 0, maxPage);
        pageTimer.reset();
        rebuildMapTable();
    }

    private void setupButtons(){
        buttons.defaults().padLeft(8);

        buttons.table(t -> {
            t.button(Core.bundle.get("back"), Icon.left, Styles.flatBordert, this::hide)
            .size(210, 64);
        }).growX();

        buttons.table(t -> {
            t.left();

            t.button(Icon.refresh, Styles.squarei, this::rebuildMapTable)
            .size(64).padRight(8).tooltip("@wayzer-maps.refresh", true);

            t.button(Icon.left, Styles.squarei, () -> shiftPage(-1))
            .size(64).tooltip(Core.bundle.format("wayzer-maps.lastPage.hint", "" + lastPageKeyCode));

            t.label(() -> (page + 1) + "/" + (maxPage + 1))
            .labelAlign(Align.center).padRight(8).padLeft(8).width(64);

            t.button(Icon.right, Styles.squarei, () -> shiftPage(1))
            .size(64).tooltip(Core.bundle.format("wayzer-maps.nextPage.hint", "" + nextPageKeyCode));
        }).fillX();

        buttons.table().growX(); // 空的 webTable
    }

    private void rebuild(){
        cont.clear();
        cont.top();

        if(!searchTable.hasChildren()) setupSearchTable(searchTable);
        if(!userTable.hasChildren()) setupUserTable(userTable);

        width = (Vars.mobile ? 510 : 350) / Scl.scl();

        if(!Core.graphics.isPortrait()){
            // 横屏布局
            cont.table(left -> {
                left.top();
                left.defaults().growX();

                left.add(searchTable).row();
                left.defaults().padTop(64);
                left.add(userTable).grow();
            }).pad(16).grow();

            float mapTableWidth = Core.graphics.getWidth() / Scl.scl() * 2 / 3;
            columns = (int)(mapTableWidth / width);

            cont.add(mapTable).width(mapTableWidth).pad(8).fillY();
        }else{
            // 竖屏布局
            cont.table(this::setupSearchTable).growX();
            cont.row();

            float mapTableWidth = Core.graphics.getWidth() / Scl.scl();
            columns = (int)(mapTableWidth / width);

            cont.add(mapTable).pad(8).growX();
        }

        rebuildMapTable();
    }

    private void setQueryText(String text){
        queryText = text;
        field.setText(text);

        try{
            int id = Integer.parseInt(text);
            detailsDialog.safeShow(id);
        }catch(NumberFormatException ignored){
        }

        rebuildMapTable();
    }

    private void setUserTag(String text){
        userTag = text;
        userField.setText(text);
        rebuildMapTable();
    }

    private void setupSearchTable(Table table){
        table.top();
        table.defaults().growX();

        table.table(top -> {
            top.left();
            top.image(Icon.zoom).size(buttonSize);

            field = BrowserUI.deboundTextField(queryText, this::setQueryText);
            field.setMessageText(Core.bundle.get("wayzer-maps.search-map.tip"));

            if(Core.app.isDesktop()){
                Core.scene.setKeyboardFocus(field);
            }

            top.add(field).growX();

            top.button(Icon.cancel, Styles.cleari, () -> setQueryText(""))
            .size(buttonSize);
        }).row();

        table.pane(Styles.noBarPane, panet -> {
            panet.top().right();
            panet.defaults().expandX().left();

            panet.table(userTable -> {
                userTable.top();
                userTable.add(Core.bundle.get("wayzer-maps.tag-@user")).padRight(8);

                TextField field = userField = BrowserUI.deboundTextField(userTag, this::setUserTag);
                field.setMessageText(Core.bundle.get("wayzer-maps.search-user.tip"));

                userTable.add(field).padLeft(24).width(buttonSize * 6);

                userTable.button(Icon.cancel, Styles.cleari, () -> setUserTag(""))
                .size(buttonSize);
            }).row();

            for(SelectTag searchTag : selectTags){
                Table tagsTable = panet.table().padTop(4).get();
                panet.row();

                tagsTable.top();
                tagsTable.add(searchTag.getDescription()).padRight(8);

                tagsTable.table(Tex.whiteui, t -> {
                    t.top();

                    for(int i = 0; i < searchTag.tags.length; i++){
                        Object tag = searchTag.tags[i];
                        String text = searchTag.getTagName(tag);

                        int finalI = i;
                        t.button(text, Styles.flatToggleMenut, () -> {
                            int now = searchTag.selectIndex;
                            searchTag.selectIndex = now == finalI ? -1 : finalI;
                            rebuildMapTable();
                        }).update(b -> b.setChecked(searchTag.selectIndex == finalI))
                        .size(buttonSize * 3, buttonSize).padLeft(4).growY();

                        if((i + 1) % 3 == 0){
                            t.row();
                        }
                    }
                }).color(Pal.gray).padLeft(24).growY();
            }
        }).grow();
    }

    private void setupUserTable(Table table){
        if(!enableLogin) return;

        boolean logged = user != null && user.logged();
        table.top();

        if(!logged){
            table.button(Core.bundle.get("wayzer-maps.login"), this::login)
            .minWidth(180);
            return;
        }

        table.add("已登陆到资源站");
        table.row();

        table.defaults().minWidth(80f).pad(16f);
        table.button("上传地图", () -> {
            MapSelector.select("选择上传的地图", (map, hideSelector) -> {
                Vars.ui.showConfirm("是否要上传地图：" + map.name(), () -> {
                    hideSelector.run();

                    Vars.ui.loadfrag.show("Uploading");
                    user.postMap(map, thread -> {
                        Vars.ui.loadfrag.hide();
                        BrowserUI.infoToast("上传成功");
                        detailsDialog.safeShow(thread);
                    }, err -> Vars.ui.showException("上传失败", err));
                });
            });
        });

        table.button("退出登录", () -> {
            user.logout();
            table.clearChildren();
            setupUserTable(table);
        });

        user.info((info) -> {
            table.add("你好" + info.name);
        }, err -> {
            Log.err(err);
        });
    }

    private void rebuildMapTable(){
        mapTable.clear();
        mapTable.center();

        BrowserUI.setLoadingText(mapTable);
        Backend.getMapList(page * count, buildSearchText(), seq -> {
            mapData.set(seq);
            setupBrowser(mapTable);
        }, err -> {
            BrowserUI.setLoadFailedText(mapTable);
            Vars.ui.showException(err);
        });
    }

    private void setupBrowser(Table table){
        if(mapData.isEmpty()){
            BrowserUI.setNoResultText(table);
            return;
        }

        table.clear();
        table.pane(Styles.noBarPane, tp -> {
            tp.top();
            int len = Math.min(count, mapData.size);

            for(int i = 0; i < len; i++){
                SiteMapInfo data = mapData.get(i);

                tp.table(Styles.grayPanel, t -> {
                    setupMap(t, data);
                }).size(width, height).padLeft(8).padTop(8);

                if((i + 1) % columns == 0) tp.row();
            }
        }).scrollX(false).grow();
    }

    private void setupMap(Table table, SiteMapInfo mapData){
        table.top().left();
        table.defaults().growX();

        String name = mapData.name;
        int thread = mapData.id;

        table.table(top -> {
            top.add(name).style(Styles.outlineLabel).color(Pal.accent)
            .align(Align.left).ellipsis(true).wrap().growX().tooltip(name);

            float imageSize = buttonSize * 0.7f;
            top.button(Icon.downloadSmall, Styles.clearNonei, imageSize, () -> {
                Vars.ui.showConfirm("@confirm", Core.bundle.format("wayzer-maps.map-download.confirm", name), () -> {
                    downloadMap(thread, name);
                });
            }).size(buttonSize)
            .tooltip(Core.bundle.format("wayzer-maps.map-download.hint", name), true);

            top.button(Core.atlas.drawable("wayzer-maps-vote"), Styles.clearNonei, imageSize, () -> {
                Vars.ui.showConfirm(Core.bundle.format("wayzer-maps.map-vote.confirm", name), () -> {
                    Call.sendChatMessage("/vote map " + thread);
                    Call.sendChatMessage("1");
                });
            }).size(buttonSize).padLeft(4).disabled(!Vars.net.client())
            .tooltip(Core.bundle.format("wayzer-maps.map-vote.hint", name), true);

            top.button(Icon.info, Styles.clearNonei, imageSize, () -> {
                detailsDialog.show(thread);
            }).size(buttonSize).padLeft(4)
            .tooltip("@wayzer-maps.map-info.hint");
        });

        table.row();

        table.table(bottom -> {
            bottom.top().left();

            bottom.table(Tex.pane, imageTable -> {
                imageTable.image(Backend.fetchPreview(thread, mapData.preview)).scaling(Scaling.fit).grow();
            }).height(imageSize).growX().tooltip(mapData.desc);
        }).margin(4);

        table.row();

        table.pane(Styles.noBarPane, tagsTable -> {
            tagsTable.left();
            tagsTable.background(Tex.pane);
            tagsTable.defaults().pad(4);

            for(String tag : mapData.tags){
                tagsTable.table(Tex.whiteui, t -> {
                    String text = tag.replaceAll("§.*", "");
                    t.add(text).minWidth(Scl.scl(96)).labelAlign(Align.center).pad(8);
                }).color(Pal.gray);
            }
        }).scrollY(false).pad(4).padLeft(8).padRight(8).fillY();

        table.invalidateHierarchy();
    }

    private String buildSearchText(){
        if(selectTags.isEmpty() && userTag == null) return queryText;
        int i = 0;
        StringBuilder applied = new StringBuilder();

        if(!queryText.isEmpty()){
            applied.append(queryText);
            i++;
        }

        for(SelectTag selectTag : selectTags){
            if(!selectTag.selected()) continue;
            if(i++ != 0) applied.append("+");
            applied.append(selectTag.getSearchText());
        }

        if(!userTag.isEmpty()){
            if(i != 0) applied.append("+");
            applied.append("@user:").append(userTag);
        }

        return applied.toString();
    }

    private static void downloadMap(int thread, String mapName){
        LoadingFragment loadfrag = Vars.ui.loadfrag;
        loadfrag.show("@download");
        loadfrag.setProgress(0f);

        Fi tmp = Vars.tmpDirectory.child(mapName + "." + Vars.mapExtension);
        Backend.downloadMap(thread, data -> {
            loadfrag.setProgress(1f); // animate in hiding
            loadfrag.hide();
            tmp.writeBytes(data);

            Map conflict = Vars.maps.all().find(m -> m.name().equals(mapName));
            if(conflict != null){
                Vars.ui.showConfirm("@confirm", Core.bundle.format("editor.overwrite.confirm", mapName), () -> {
                    Vars.maps.tryCatchMapError(() -> {
                        Vars.maps.removeMap(conflict);
                        Vars.maps.importMap(tmp);
                    });
                    tmp.delete();
                    BrowserUI.infoToast(Core.bundle.format("wayzer-maps.map-download.successed", mapName), 5);
                });
            }else{
                Vars.maps.tryCatchMapError(() -> {
                    Vars.maps.importMap(tmp);
                });
                tmp.delete();
                BrowserUI.infoToast(Core.bundle.format("wayzer-maps.map-download.successed", mapName), 5);
            }
        }, err -> {
            loadfrag.hide();
            Vars.ui.showException(err);
        });
    }

    public static class SelectTag{
        public String name;
        public Object[] tags;
        public int selectIndex = -1;

        public SelectTag(String name, Object[] tags){
            this.name = name;
            this.tags = tags;
        }

        public boolean selected(){
            return selectIndex != -1;
        }

        public void select(int index){
            selectIndex = Mathf.clamp(index, -1, tags.length - 1);
        }

        public String getSearchText(){
            if(selectIndex != -1) return name + ":" + tags[selectIndex];
            return "";
        }

        public String getTagName(Object tag){
            return Core.bundle.get("wayzer-maps.tag-" + name + "-" + tag, tag.toString());
        }

        public String getDescription(){
            return Core.bundle.get("wayzer-maps.tag-" + name, name);
        }
    }
}