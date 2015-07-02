package org.healtheheartstudy.network;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * RequestManager is a wrapper to simplify Volley's request queue.
 */
public class RequestManager {

    private static RequestQueue requestQueue;

    public static void init(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public static RequestQueue getRequestQueue() {
        if (requestQueue != null) return requestQueue;
        else throw new IllegalStateException("Not initialized");
    }

}
