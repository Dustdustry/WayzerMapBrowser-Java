package mapbrowser.api;

import arc.*;
import arc.func.*;
import arc.util.*;
import arc.util.Http.*;
import mapbrowser.api.BackendTypes.SiteMapDetails.*;
import mapbrowser.utils.*;
import mindustry.io.*;
import mindustry.maps.*;

import static arc.util.Http.HttpMethod.*;
import static arc.util.Http.HttpMethod.POST;
import static mapbrowser.BrowserVars.*;

public class SiteUser{
    private final String userAgent;

    private String token = null;
    private SiteUserInfo info = null;

    private boolean fetchingInfo;

    public SiteUser(String version, String uuid){
        this.userAgent = "Mindustry" + "/" + version + " " + "UUID" + "/" + uuid;
    }

    public void logout(){
        token = null;
        info = null;
    }

    public boolean logged(){
        return token != null;
    }

    public void login(Cons2<String, Runnable> loginHandler, Cons<String> onFinish, Cons<Throwable> onError){
        request(POST, wayzerApi + "/users/tokenRequest")
        .error(err -> Core.app.post(() -> onError.get(err))) // too ugly
        .submit((resp) -> {
            String code = resp.getResultAsString();
            String loginUrl = resourceSite + "/user/requestToken?code=" + code;

            Core.app.post(() -> {
                loginHandler.get(loginUrl, () -> {
                    request(GET, wayzerApi + "/users/tokenRequest/" + code + "/result")
                    .timeout(50 * 1000)
                    .error(onError)
                    .submit((tokenResponse) -> {
                        token = tokenResponse.getResultAsString();
                        Core.app.post(() -> onFinish.get(token));
                    });
                });
            });
        });
    }

    public void info(Cons<SiteUserInfo> infoHandler, Cons<Throwable> onError){
        if(info != null || fetchingInfo){
            infoHandler.get(info);
            return;
        }

        fetchingInfo = true;

        request(GET, wayzerApi + "/users/info")
        .error(e -> {
            fetchingInfo = false;
            Core.app.post(() -> onError.get(e));
        })
        .submit((resp) -> {
            fetchingInfo = false;
            String result = resp.getResultAsString();
            Core.app.post(() -> {
                info = JsonIO.json.fromJson(SiteUserInfo.class, result);
                infoHandler.get(info);

                Log.info(result);
            });
        });
    }

    public void postMap(Map map, Cons<Integer> onFinish, Cons<Throwable> onError){
        FormData formData = new FormData();
        formData.append("file", map.file);

        request(POST, wayzerApi + "/maps")
        .header("Content-Type", formData.getContentType())
        .content(formData.getStream())
        .timeout(20 * 1000)
        .error(err -> Core.app.post(() -> onError.get(err)))
        .submit((result) -> {
            int thread = Integer.parseInt(result.getResultAsString());
            Core.app.post(() -> onFinish.get(thread));
        });
    }

    public void putMap(int thread, Map map, Runnable onFinish, Cons<Throwable> onError){
        FormData formData = new FormData();
        formData.append("file", map.file);

        request(PUT, wayzerApi + "/maps" + "/" + thread)
        .header("Content-Type", formData.getContentType())
        .content(formData.getStream())
        .timeout(20 * 1000)
        .error(err -> Core.app.post(() -> onError.get(err)))
        .submit((result) -> Core.app.post(onFinish));
    }

    public void deleteMap(int thread, Runnable onFinish, Cons<Throwable> onError){
        request(DELETE, wayzerApi + "/maps/" + thread)
        .error(err -> Core.app.post(() -> onError.get(err)))
        .submit((result) -> {
            Core.app.post(() -> {
                if(onFinish != null) onFinish.run();
            });
        });
    }

    private HttpRequest request(HttpMethod method, String url){
        return Http.request(method, url)
        .header("Authorization", "Bearer " + (token != null ? token : ""))
        .header("User-Agent", userAgent);
    }
}