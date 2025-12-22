package mapbrowser.ui;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mapbrowser.*;
import mapbrowser.api.*;
import mapbrowser.api.BackendTypes.*;
import mapbrowser.api.BackendTypes.SiteMapDetails.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.editor.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import java.lang.reflect.*;
import java.util.*;

public class DetailsDialog extends BaseDialog{
    private SiteMapDetails details;
    private JsonValue jsonData;
    private WaveGraph waveGraph;

    public DetailsDialog(){
        super("@wayzer-maps.details");

        resized(this::rebuildDialog);
        addCloseButton();
    }

    public void show(int thread){
        cont.clear();

        BrowserUI.setLoadingText(cont);
        show();

        Backend.getMapDetails(thread, (details, value) -> {
            this.details = details;
            this.jsonData = value;
            rebuildDialog();
        }, e -> {
            BrowserUI.setLoadFailedText(cont);
            Vars.ui.showException(e);
            Log.err(e);
        });
    }

    public void safeShow(int thread){
        Backend.getMapDetails(thread, (details, value) -> {
            this.details = details;
            this.jsonData = value;
            rebuildDialog();
            show();
        }, null);
    }

    private void rebuildDialog(){
        cont.clear();

        float infoWidth = Core.scene.getWidth() * (Core.graphics.isPortrait() ? 0.8f : 0.2f);
        Table body = cont.table().get();

        body.top();

        body.table(front -> {
            front.defaults().growX();

            front.table(Tex.button, topTable -> {
                topTable.table(Styles.grayPanel, this::setupImageTable).fill();
                topTable.table(Styles.grayPanel, this::setupInfoTable).width(infoWidth).growY().padLeft(16);
            });

            front.row();

            front.table(Tex.button, bottomTable -> {
                bottomTable.top();
                bottomTable.defaults().padTop(8).growX();

                bottomTable.table(Styles.grayPanel, this::setupRulesTable).row();
            }).padTop(16);
        });

        if(Core.graphics.isPortrait()){
            body.row();
        }

        if(details.tags.rules.waves){
            Cell<?> containerCell;
            final Cell<?>[] waveTableCell = new Cell<?>[1];
            containerCell = body.table(Tex.button, container -> {
                waveTableCell[0] = container.table(Styles.grayPanel, this::setupWaveTable).grow();
            }).grow();

            if(Core.graphics.isPortrait()){
                waveTableCell[0].height(infoWidth * 0.8f);
                containerCell.padTop(16);
            }else{
                waveTableCell[0].width(infoWidth * 0.8f);
                containerCell.padLeft(16);
            }
        }
    }

    private void setupImageTable(Table table){
        TextureRegion region = Backend.fetchPreview(details.thread, details.preview);
        BorderImage image = new BorderImage(region, 1);
        table.add(image).scaling(Scaling.fit).size(256 / Scl.scl()).tooltip("@view-image", true);
        image.clicked(() -> {
            BaseDialog dialog = new BaseDialog("@view-image");
            dialog.cont.image(image.getDrawable()).scaling(Scaling.fit);
            dialog.addCloseButton();
            dialog.show();
        });
    }

    private void setupInfoTable(Table table){
        table.left().top();
        table.defaults().padTop(8).growX();

        SiteUserInfo user = details.user;
        String mode = details.mode;
        int thread = details.thread;
        SiteMapTags tags = details.tags;
        String name = tags.name;
        String author = tags.author;
        String description = tags.description;
        int width = tags.width;
        int height = tags.height;
        Seq<String> mods = tags.mods;

        table.add(name).align(Align.left).padLeft(8f).wrap().growX().row();

        addText(table, "map-uploader", user.name, t -> {
            t.table(buttons -> {
                buttons.defaults().size(32).pad(4);

                buttons.button(Icon.copy, Styles.cleari, () -> {
                    BrowserUI.setClipboard(user.gid);
                }).size(24).tooltip(Core.bundle.format("wayzer-maps.copy-gid", user.gid), true);
            }).expand().right().row();
        });
        addText(table, "map-author", author, t -> {});

        addText(table, "map-thread", String.valueOf(thread), t -> {
            t.table(buttons -> {
                buttons.defaults().size(32).pad(4);

                buttons.button(Icon.copy, Styles.cleari, () -> {
                    BrowserUI.setClipboard(String.valueOf(thread));
                });

                buttons.button(Icon.download, Styles.clearNonei, () -> {
                    Vars.ui.showConfirm("@confirm", Core.bundle.format("wayzer-maps.map-download.confirm", name), () -> {
                        Backend.downloadImportMap(thread, name);
                    });
                }).tooltip(Core.bundle.format("wayzer-maps.map-download.hint", name), true);
            }).expandX().right().row();
        });

        addText(table, "map-mode", mode);
        addText(table, "map-size", width + "x" + height);

        if(!mods.isEmpty()){
            addText(table, "map-mods", "", modsTable -> {
                modsTable.pane(Styles.noBarPane, tagsTable -> {
                    tagsTable.left();
                    tagsTable.background(Tex.pane);
                    tagsTable.defaults().pad(4);

                    for(String tag : mods){
                        tagsTable.table(Tex.whiteui, t -> {
                            String text = tag.replaceAll("§.*", "");
                            t.add(text).minWidth(Scl.scl(96)).labelAlign(Align.center).pad(8);
                        }).color(Pal.gray);
                    }
                }).scrollY(false).pad(4).padLeft(8).padRight(8).fillY();
            });
        }

        if(description != null){
            addText(table, "map-description", "", descriptionTable -> {
                descriptionTable.pane(Styles.noBarPane, t -> {
                    t.top();
                    t.background(Tex.pane);
                    t.add(description).wrap().growX();
                }).scrollX(false).maxHeight(128).pad(8).grow();
            });
        }
    }

    private void addText(Table table, String tag, String text){
        addText(table, tag, text, null);
    }

    private void addText(Table table, String tag, String text, Cons<Table> cons){
        table.table(t -> {
            t.top().left();
            t.add(Core.bundle.format("wayzer-maps." + tag, text)).align(Align.left).padLeft(8);

            if(cons != null) cons.get(t);
        });
        table.row();
    }

    private void setupRulesTable(Table table){
        if(!jsonData.has("tags")) return;

        SiteMapRules rules = details.tags.rules;
        JsonValue rulesData = jsonData.get("tags").get("rules");

        if(rulesData == null) return;

        addTitle(table, "Rules");
        table.defaults().growX();
        Table body = new Table();
        table.pane(Styles.noBarPane, body).scrollX(false).grow();

        body.defaults().growX();
        body.table(base -> {
            int i = 0;

            for(Field f : BrowserVars.booleanRules){
                if(!rulesData.has(f.getName())) continue;

                boolean value = Reflect.get(rules, f);
                String valueText = Core.bundle.get("wayzer-maps.rules." + (value ? "enable" : "disable"));
                String ruleText = Core.bundle.get("rules." + f.getName().toLowerCase());

                base.table(ruleTable -> {
                    ruleTable.left();
                    ruleTable.add(ruleText).align(Align.left).color(Pal.lightishGray).padLeft(4);
                    ruleTable.add(valueText).align(Align.left).padLeft(16);
                }).pad(4).growX();

                if(++i % 2 == 0) base.row();
            }

            for(Field f : BrowserVars.multiplierRuleFields){
                if(!rulesData.has(f.getName())) continue;

                float value = Reflect.get(rules, f);
                String valueText = "" + ((int)value * 1000 + 0.5) / 1000.0;
                String ruleText = Core.bundle.get("rules." + f.getName().toLowerCase(), Strings.camelize(f.getName()));

                base.table(ruleTable -> {
                    ruleTable.left();
                    ruleTable.add(ruleText).align(Align.left).color(Pal.lightishGray).padLeft(4);
                    ruleTable.add(valueText).align(Align.left).padLeft(16);
                }).pad(4).growX();

                if(++i % 2 == 0) base.row();
            }

            { // radius
                String valueText = Strings.autoFixed(rules.enemyCoreBuildRadius / Vars.tilesize, 1);
                String ruleText = Core.bundle.get("rules.enemycorebuildradius");

                base.table(ruleTable -> {
                    ruleTable.left();
                    ruleTable.add(ruleText).align(Align.left).color(Pal.lightishGray).padLeft(4);
                    ruleTable.add(valueText).align(Align.left).padLeft(16);
                }).pad(4).growX();
            }
        });

        body.row();

        body.table(contentTable -> {
            contentTable.top();

            // miss kotlin...
            Cons3<String, ContentType, SiteMapContents> addContents = (text, type, siteContents) -> {
                if(siteContents == null) return;
                Seq<String> contentNames = siteContents.values;
                if(contentNames == null || contentNames.isEmpty()) return;

                contentTable.table(Tex.pane, t -> {
                    t.left().top();
                    t.add(text).color(Pal.lightishGray).padLeft(4).padRight(4);
                    t.pane(Styles.noBarPane, pane -> {
                        pane.left();
                        Table iconTable = pane.table().get();

                        int i = 0;
                        for(Content c : Vars.content.getBy(type)){
                            if(!(c instanceof UnlockableContent uc) || !contentNames.contains(uc.name)) continue;

                            iconTable.button(new TextureRegionDrawable(uc.uiIcon), Styles.cleari, 28, () -> Vars.ui.content.show(uc))
                            .size(32).padLeft(8).tooltip(uc.localizedName);

                            if(++i % 12 == 0){
                                iconTable.row();
                            }
                        }
                    }).scrollX(false).maxHeight(32*4.5f).growX();
                }).growX().padTop(4);

                contentTable.row();
            };

            SiteMapContents bannedBlocks = rules.bannedBlocks,
            bannedUnits = rules.bannedUnits,
            revealedBlocks = rules.revealedBlocks;

            addContents.get("@bannedblocks", ContentType.block, bannedBlocks);
            addContents.get("@bannedunits", ContentType.unit, bannedUnits);
            addContents.get("@wayzer-maps.revealedblocks", ContentType.block, revealedBlocks);
        });
    }

    private void setupWaveTable(Table waves){
        if(waveGraph == null) waveGraph = new WaveGraph();
        Seq<SpawnGroup> spawns = details.tags.rules.spawns;
        if(spawns == null || spawns.isEmpty()) return;

        waveGraph.groups.set(spawns);
        waveGraph.rebuild();

        waves.left();
        addTitle(waves, "Waves");
        waves.add(waveGraph).grow();
    }

    private static void addTitle(Table table, String title){
        table.table(Tex.whiteui, t -> {
            t.add(title);
        }).color(Pal.gray).padBottom(8).growX().row();
    }
}