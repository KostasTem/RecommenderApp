package com.example.recommenderApp.NetworkUtils;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.example.recommenderApp.DataClasses.Recommendation;
import com.example.recommenderApp.LoginActivity;
import com.example.recommenderApp.R;
import com.example.recommenderApp.RecommendationRecyclerViewAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataSingleton {
    private static DataSingleton instance = null;
    private Context context;
    private String accessToken,refreshToken;
    private RecommendationRecyclerViewAdapter mainAppRecyclerView;
    private RecommendationRecyclerViewAdapter historyRecyclerView;
    private List<Recommendation> allRecommendations;
    private String userID;
    private boolean blockRequests = false;
    private boolean getRecOnActivityLaunch = true;
    private final String defaultURL = "https://10.0.2.2:8443/";
    //private final String defaultURL = "https://192.168.0.110:8443/";
    //private final String defaultURL = "https://192.168.0.140:8443/";

    private DataSingleton(){

    }
    public static DataSingleton getInstance(){
        if(instance == null){
            instance = new DataSingleton();
        }
        return instance;
    }
    //Request for un protected API endpoints
    public void unauthorizedJsonRequest(String path, JSONObject jsonObject, int method, final VolleyCallback callback){
        //If no internet connection, block request
        if(blockRequests){
            Toast.makeText(context, context.getResources().getString(R.string.no_network_connection), Toast.LENGTH_SHORT).show();
            return;
        }

        String url = defaultURL + path;
        JsonObjectRequest request = new JsonObjectRequest(method, url, jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    callback.onSuccess(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Here 2");
                NetworkResponse response = error.networkResponse;
                //If no response, return;
                if(response==null){
                    Toast.makeText(context, context.getResources().getString(R.string.server_connection_error), Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    String res = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "utf-8"));
                    JSONObject object = new JSONObject(res);
                    callback.onErrorResponse(object);
                } catch (JSONException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
        RequestQueueSingleton.getInstance(context).addToRequestQueue(request);
    }
    //Request for protected API endpoints
    public void authorizedJsonRequest(String path, JSONObject jsonObject, int method, final VolleyCallback callback){
        if(blockRequests){
            Toast.makeText(context, context.getResources().getString(R.string.no_network_connection), Toast.LENGTH_SHORT).show();
            return;
        }
        String url = defaultURL + path;
        JsonObjectRequest request = new JsonObjectRequest(method, url, jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    callback.onSuccess(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response = error.networkResponse;
                if(response==null){
                    Toast.makeText(context, context.getResources().getString(R.string.server_connection_error), Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    String res = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "utf-8"));
                    JSONObject object = new JSONObject(res);
                    //Handle expired tokens
                    if(object.getString("Error").contains("expired")){
                        //Log user out if refresh token has expired
                        if(path.contains("refreshToken")){
                            logout(true);
                        }
                        //Refresh access token if it has expired
                        else {
                            refreshToken(path,jsonObject,method,callback);
                        }
                    }
                    //Pass any other errors to callback
                    else {
                        callback.onErrorResponse(object);
                    }
                } catch (UnsupportedEncodingException | JSONException e) {
                    e.printStackTrace();
                }
            }
        })
        {
            @Override
            public Map<String, String> getHeaders(){
                Map<String, String>  params = new HashMap<>();
                if(!path.contains("refreshToken"))params.put("Authorization", "Bearer " + accessToken);
                else params.put("Authorization","Bearer " + refreshToken);
                return params;
            }
        };
        RequestQueueSingleton.getInstance(context).addToRequestQueue(request);
    }
    //Login string request with username and password
    public void login(final VolleyCallback callback, String username, String password){
        if(blockRequests){
            Toast.makeText(context, context.getResources().getString(R.string.no_network_connection), Toast.LENGTH_SHORT).show();
            return;
        }
        String url = defaultURL + "api/user/login";
        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject tempObject = new JSONObject(response);
                    callback.onSuccess(tempObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response = error.networkResponse;
                if(response==null){
                    Toast.makeText(context, context.getResources().getString(R.string.server_connection_error), Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    String res = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "utf-8"));
                    JSONObject object = new JSONObject(res);
                    callback.onErrorResponse(object);
                } catch (JSONException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        })
        {
            @Override
            public String getBodyContentType() {return "application/x-www-form-urlencoded; charset=UTF-8"; }
            @Override
            protected Map<String, String> getParams(){
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                return params;
            }
        };
        RequestQueueSingleton.getInstance(context).addToRequestQueue(request);
    }
    //Refresh token request. Runs when server responds with "Token expired" when trying to use access token
    //If this also returns "Token expired" then user gets logged out and has to input their credentials again.
    protected void refreshToken(String path, JSONObject jsonObject,int method, VolleyCallback callback){
        authorizedJsonRequest("api/user/refreshToken", null, Request.Method.POST, new VolleyCallback() {
            @Override
            public void onSuccess(JSONObject resultObject) throws JSONException {
                accessToken = resultObject.getString("access_token");
                authorizedJsonRequest(path, jsonObject, method, callback);
            }
            @Override
            public void onErrorResponse(JSONObject errorObject) {
                try {
                    Toast.makeText(context, errorObject.getString("Error"), Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    //Register a user data object with the API so a new user, or one that has changed
    //devices with syncing their data, can use the app anonymously
    public void registerUserID(String uid,String username){
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userID",uid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        unauthorizedJsonRequest("api/data/insertNewUser", jsonObject, Request.Method.POST, new VolleyCallback() {
            @Override
            public void onSuccess(JSONObject responseObject) throws JSONException {
                Log.i("Insert new user request: ","Success");
                sharedPreferences.edit().putString(username,uid).apply();
                setUserID(uid);
            }

            @Override
            public void onErrorResponse(JSONObject errorObject) throws JSONException {
                if(errorObject.getString("Error").equals("User already exists")){
                    String uid = UUID.randomUUID().toString();
                    registerUserID(uid,username);
                }
            }
        });
    }
    //Remove user data and return to login activity
    public void logout(boolean local){
        if(local) {
            context.startActivity(new Intent(context, LoginActivity.class));
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("access_token");
        editor.remove("refresh_token");
        editor.remove("Synced");
        editor.commit();
        this.accessToken = null;
        this.refreshToken = null;
        this.historyRecyclerView = null;
        this.mainAppRecyclerView = null;
        this.getRecOnActivityLaunch = true;
        this.allRecommendations = null;
        this.userID = null;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    public void setContext(Context context) {
        this.context = context;
    }
    public void setUserID(String userID){
        this.userID = userID;
    }
    public void setAllRecommendations(List<Recommendation> recommendations){
        this.allRecommendations = recommendations;
    }
    public void setMainAppRecyclerView(RecommendationRecyclerViewAdapter main){
        this.mainAppRecyclerView = main;
    }

    public boolean getRecOnActivityLaunch() {
        return getRecOnActivityLaunch;
    }

    public void setGetRecOnActivityLaunch(boolean getRecOnActivityLaunch) {
        this.getRecOnActivityLaunch = getRecOnActivityLaunch;
    }

    public void setHistoryRecyclerView(RecommendationRecyclerViewAdapter history){
        this.historyRecyclerView = history;
    }
    public void setBlockRequests(boolean blockRequests){
        this.blockRequests = blockRequests;
    }
    public boolean isBlockRequests(){return blockRequests;}
    public String getUserID(){
        return userID;
    }
    public List<Recommendation> getAllRecommendations(){
        return allRecommendations;
    }
    public Context getContext(){return this.context;}
    public RecommendationRecyclerViewAdapter getMainAppRecyclerView(){
        return mainAppRecyclerView;
    }
    public RecommendationRecyclerViewAdapter getHistoryRecyclerView(){
        return historyRecyclerView;
    }
}
