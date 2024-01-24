package com.example.recommenderApp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.example.recommenderApp.NetworkUtils.DataSingleton;
import com.example.recommenderApp.NetworkUtils.VolleyCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class LoginActivity extends AppCompatActivity {
    private EditText username, password;
    private final DataSingleton ioObject = DataSingleton.getInstance();
    private SharedPreferences sharedPreferences;
    private final int READ_PHONE_STATE_CODE=213;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getApplicationContext().getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        ioObject.setContext(getApplicationContext());
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //Check whether there's an internet connection to allow or block requests.
        //Also set a listener for changes in network state
        ioObject.setBlockRequests(!(netInfo != null && netInfo.isConnected()));
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(@NonNull Network network) {
                ioObject.setBlockRequests(false);
            }

            @Override
            public void onLost(@NonNull Network network) {
                ioObject.setBlockRequests(true);
            }
        };
        cm.registerDefaultNetworkCallback(networkCallback);
        //Enter directly into main interface if user is already logged in
        //Also get username by decoding the access_token, so we can get their user id from shared preferences.
        if(sharedPreferences.contains("access_token")) {
            ioObject.setAccessToken(sharedPreferences.getString("access_token","null"));
            ioObject.setRefreshToken(sharedPreferences.getString("refresh_token","null"));
            String username = decoded(sharedPreferences.getString("access_token",""));
            ioObject.setUserID(sharedPreferences.getString(username,""));
            openNextActivity(new Intent(LoginActivity.this,MainApp.class));
        }
        setContentView(R.layout.activity_main);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        //Ask for READ_PHONE_STATE permission to access isDataEnabled setting only on the first launch of the application
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED && !sharedPreferences.contains("Perm213Asked")) {
            ActivityCompat.requestPermissions(LoginActivity.this,new String[]{Manifest.permission.READ_PHONE_STATE}, READ_PHONE_STATE_CODE);
        }
    }

    //Runs when user clicks login
    public void login(View view) throws JSONException {
        String uname = username.getText().toString();
        ioObject.login(new VolleyCallback() {
            //On successful login, store the access token and refresh token in Shared Preferences
            //Then check if they have their user id synced to their account
            //Also check if user has filled the initial recommendation ratings and change which activity opens next accordingly
            @Override
            public void onSuccess(JSONObject responseObject) throws JSONException {
                Intent i = new Intent(LoginActivity.this,MainApp.class);
                ioObject.setAccessToken(responseObject.getString("access_token"));
                ioObject.setRefreshToken(responseObject.getString("refresh_token"));
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("access_token",responseObject.getString("access_token"));
                editor.putString("refresh_token",responseObject.getString("refresh_token"));
                editor.apply();
                //Get user id from server if user has synced it, else get it from shared preferences or create it if it doesn't exist
                String userID = responseObject.getString("userID");
                //If a registered user with a synced account logs in
                if(!userID.equals("null")){
                    sharedPreferences.edit().putString("Synced","yes").apply();
                    ioObject.setUserID(userID);
                    if(!sharedPreferences.contains(uname)){
                        sharedPreferences.edit().putString(uname,userID).apply();
                    }
                }
                //If a registered user without synced data logs in
                else{
                    sharedPreferences.edit().putString("Synced","no").apply();
                    if(!sharedPreferences.contains(uname)){
                        String uid = UUID.randomUUID().toString();
                        ioObject.registerUserID(uid,uname);
                    }
                }
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        openNextActivity(i);
                    }
                }, 2000);
            }
            @Override
            public void onErrorResponse(JSONObject errorObject) throws JSONException {
                Toast.makeText(LoginActivity.this,getApplicationContext().getResources().getString(R.string.login_error),Toast.LENGTH_LONG).show();
                Log.e("Error In Login: ", errorObject.getString("Error"));

            }
        }, username.getText().toString(), password.getText().toString());
        clearFields();
    }

    public void forgotPassword(View view){
        Intent webIntent = new Intent(LoginActivity.this,RequestPasswordResetActivity.class);
        startActivity(webIntent);
    }

    public void register(View view){
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        clearFields();
    }

    private void clearFields(){
        username.setText("");
        password.setText("");
    }
    //Open next activity based on wether the user has rated all the initial recommendations.
    //If they haven't continue where they stopped, if they have open MainApp activity
    private void openNextActivity(Intent i){
        String url = "api/data/getRecommendationCount/" + ioObject.getUserID();
        ioObject.authorizedJsonRequest(url, null, Request.Method.GET, new VolleyCallback() {
            @Override
            public void onSuccess(JSONObject responseObject) throws JSONException {
                Integer count = responseObject.getInt("count");
                if(count>=9){
                    startActivity(i);
                }
                else{
                    Intent n = new Intent(LoginActivity.this,BaseRecommendationRating.class);
                    n.putExtra("Count",count);
                    n.putExtra("First","y");
                    startActivity(n);
                }
            }
            @Override
            public void onErrorResponse(JSONObject errorObject) throws JSONException {
                Log.e("Error getting recommendations amount:", errorObject.getString("Error"));
            }
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==READ_PHONE_STATE_CODE){
            sharedPreferences.edit().putString("Perm213Asked","True").apply();
        }
    }
    private String decoded(String JWTEncoded){
        try {
            String[] split = JWTEncoded.split("\\.");
            JSONObject body = new JSONObject(getJson(split[1]));
            return body.getString("sub");
        } catch (UnsupportedEncodingException | JSONException e) {
            //Error
        }
        return null;
    }

    private static String getJson(String strEncoded) throws UnsupportedEncodingException{
        byte[] decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}