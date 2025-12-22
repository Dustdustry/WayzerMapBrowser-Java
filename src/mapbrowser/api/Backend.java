package mapbrowser.api;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Http.*;
import arc.util.serialization.*;
import mapbrowser.api.BackendTypes.*;
import mapbrowser.ui.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.ui.fragments.*;

import java.io.*;
import java.net.*;

import static mapbrowser.BrowserVars.wayzerApi;

public class Backend{
    private static final IntMap<TextureRegion> previewCache = new IntMap<>();
    public static TextureRegion defaultRegion;

    public static void getMapList(int count, String search, Cons<Seq<SiteMapInfo>> onSuccess, Cons<Throwable> onError){
        Http.get(wayzerApi + "/maps/list?begin=" + count + "&search=" + Strings.encode(search), resp -> {
            String result = resp.getResultAsString();
            Core.app.post(() -> {
                try{
                    Seq<SiteMapInfo> seq = JsonIO.json.fromJson(Seq.class, SiteMapInfo.class, result);
                    onSuccess.get(seq);
                }catch(Exception e){
                    onError.get(e);
                }
            });
        }, onError);
    }

    public static void downloadMap(int thread, Cons<byte[]> cons, Cons<Throwable> onError){
        Http.request(HttpMethod.GET, wayzerApi + "/maps/" + thread + ".msav")
        .timeout(50 * 1000)
        .error(onError)
        .submit(resp -> {
            byte[] result = resp.getResult();
            Core.app.post(() -> cons.get(result));
        });
    }

    public static void getMapDetails(int thread, Cons2<SiteMapDetails, JsonValue> onSuccess, Cons<Throwable> onError){
        String url = wayzerApi + "/maps/" + thread + ".json";
        Http.request(HttpMethod.GET, url)
        .timeout(10 * 1000)
        .error(onError)
        .submit(resp -> {
            String result = resp.getResultAsString();

            Core.app.post(() -> {
                SiteMapDetails details;
                JsonValue value;
                try{
                    value = JsonIO.json.fromJson(null, result);
                    details = JsonIO.json.readValue(SiteMapDetails.class, value);
                }catch(Exception e){
                    onError.get(e);
                    return;
                }
                onSuccess.get(details, value);
            });
        });
    }

    public static TextureRegion fetchPreview(int thread, String url){
        if(defaultRegion == null){
            defaultRegion = ((TextureRegionDrawable)Tex.nomap).getRegion();
        }

        TextureRegion region = previewCache.get(thread, TextureRegion::new);
        if(region.texture == null){
            region.set(defaultRegion);
            Http.get(url, resp -> {
                byte[] result = resp.getResult();
                Core.app.post(() -> {
                    try{
                        Texture fetched = new Texture(new Pixmap(result));
                        fetched.setFilter(TextureFilter.linear);
                        region.set(fetched);
                    }catch(Exception e){
                        Log.err(e);
                    }
                });
            });
        }

        return region;
    }

    public static void downloadImportMap(int thread, String mapName){
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
}
