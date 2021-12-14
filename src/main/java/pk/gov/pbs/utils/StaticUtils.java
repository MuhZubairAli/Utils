package pk.gov.pbs.utils;

import android.content.Context;
import android.os.Handler;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class StaticUtils {
    private static Handler handler;
    public static Handler getHandler(){
        synchronized (StaticUtils.class){
            if (handler == null)
                handler= new Handler();
            return handler;
        }
    }

    private static Gson gson;
    public static Gson getGson(boolean prettify, boolean replace) {
        synchronized (StaticUtils.class){
            if (gson == null || replace) {
                gson = prettify ? new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .setPrettyPrinting()
                        .create()
                        : new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .create();
            }
            return gson;
        }
    }

    public static Gson getGson(boolean replace) {
        return getGson(false, replace);
    }

    public static Gson getGson() {
        return getGson(false, false);
    }

    private static RequestQueue webRequestQueue;
    public static RequestQueue getVolleyWebQueue(Context context) {
        synchronized (StaticUtils.class) {
            if (webRequestQueue == null)
                webRequestQueue = Volley.newRequestQueue(context);
            return webRequestQueue;
        }
    }
}
