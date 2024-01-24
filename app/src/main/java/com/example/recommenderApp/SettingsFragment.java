package com.example.recommenderApp;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.android.volley.Request;
import com.example.recommenderApp.NetworkUtils.DataSingleton;
import com.example.recommenderApp.NetworkUtils.VolleyCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        SharedPreferences sharedPreferences = getContext().getApplicationContext().getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        boolean syncStatus = sharedPreferences.getString("Synced", "").equals("yes");
        DataSingleton ioObject = DataSingleton.getInstance();
        SwitchPreferenceCompat syncPreference = findPreference("sync");
        syncPreference.setChecked(syncStatus);
        //When user changes sync data setting, setting request to API to change this setting on the server
        syncPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean setting = (boolean) newValue;
                JSONObject jsonObject = new JSONObject();
                if(setting){
                    try {
                        sharedPreferences.edit().putString("Synced","yes").apply();
                        jsonObject.put("userID",ioObject.getUserID());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    sharedPreferences.edit().putString("Synced","no").apply();
                }
                ioObject.authorizedJsonRequest("api/user/syncDataWithUser", jsonObject, Request.Method.POST, new VolleyCallback() {
                    @Override
                    public void onSuccess(JSONObject responseObject) throws JSONException {
                        System.out.println(responseObject.getString("Status"));
                        if(responseObject.getString("Status").split(" ")[1].equals("Unsynced")) {
                            Toast.makeText(getView().getContext().getApplicationContext(), getString(R.string.unsync_data_success), Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(getView().getContext().getApplicationContext(), getString(R.string.sync_data_success), Toast.LENGTH_SHORT).show();
                        }
                        Log.i("Sync data status: ",responseObject.getString("Status"));
                    }

                    @Override
                    public void onErrorResponse(JSONObject errorObject) throws JSONException {
                        Toast.makeText(getView().getContext().getApplicationContext(), getString(R.string.sync_data_error), Toast.LENGTH_SHORT).show();
                        Log.e("Sync data error: ", errorObject.getString("Error"));
                    }
                });
                return true;
            }
        });
    }
}
