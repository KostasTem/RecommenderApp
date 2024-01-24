package com.example.recommenderApp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.example.recommenderApp.NetworkUtils.DataSingleton;
import com.example.recommenderApp.NetworkUtils.VolleyCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class RequestPasswordResetActivity extends AppCompatActivity {
    DataSingleton ioObject = DataSingleton.getInstance();
    EditText email;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_password_reset);
        email = findViewById(R.id.passwordResetEmail);
    }

    public void requestReset(View view) throws JSONException {
        if(!Patterns.EMAIL_ADDRESS.matcher(email.getText().toString().trim()).matches()){
            Toast.makeText(getApplicationContext(),getApplicationContext().getResources().getString(R.string.use_proper_email),Toast.LENGTH_SHORT).show();
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email",email.getText().toString());
        ioObject.unauthorizedJsonRequest("api/user/sendResetEmail", jsonObject, Request.Method.POST, new VolleyCallback() {
            @Override
            public void onSuccess(JSONObject responseObject) throws JSONException {
                Toast.makeText(RequestPasswordResetActivity.this, getApplicationContext().getResources().getString(R.string.password_reset_success), Toast.LENGTH_LONG).show();
                Log.i("Password reset status: ",responseObject.getString("Status"));
            }

            @Override
            public void onErrorResponse(JSONObject errorObject) throws JSONException {
                Toast.makeText(RequestPasswordResetActivity.this, getApplicationContext().getResources().getString(R.string.password_reset_error), Toast.LENGTH_LONG).show();
                Log.e("Password reset error: ", errorObject.getString("Error"));
            }
        });
    }

}