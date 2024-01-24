package com.example.recommenderApp.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

public interface VolleyCallback {
    void onSuccess(JSONObject responseObject) throws JSONException;
    void onErrorResponse(JSONObject errorObject) throws JSONException;
}
