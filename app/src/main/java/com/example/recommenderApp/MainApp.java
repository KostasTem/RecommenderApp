package com.example.recommenderApp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.example.recommenderApp.NetworkUtils.DataSingleton;
import com.example.recommenderApp.NetworkUtils.VolleyCallback;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

public class MainApp extends AppCompatActivity {
    private final DataSingleton ioObject = DataSingleton.getInstance();
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView username, email;
    private ImageView userImage;
    private boolean newUser;
    private final List<String> vendors = Arrays.asList("Samsung","samsung","Xiaomi","xiaomi","Realme","realme","Nokia","nokia","Motorola","motorola","Galaxy","Google");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_app);
        newUser = getIntent().hasExtra("newUser");
        //Create Navigation Drawer and initialize all dynamic fields
        navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setCheckedItem(R.id.main_app_activity);
        username = navigationView.getHeaderView(0).findViewById(R.id.drawerUsername);
        email = navigationView.getHeaderView(0).findViewById(R.id.drawerEmail);
        userImage = navigationView.getHeaderView(0).findViewById(R.id.drawerImage);
        getUserData();
        sendApplicationList();
        //Listener for drawer changes
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.main_app_activity) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MainRecommendationFragment()).commit();
                    setTitle(getString(R.string.recommendations));
                } else if (id == R.id.recommendation_history_activity) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new RecommendationHistoryFragment()).commit();
                    setTitle(getString(R.string.recommendation_history));
                } else if (id == R.id.settings_activity) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,new SettingsFragment()).commit();
                    setTitle(getString(R.string.settings));
                } else if (id == R.id.logout_button) {
                    ioObject.logout(false);
                    startActivity(new Intent(MainApp.this, LoginActivity.class));
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
        if (ioObject.getContext() == null) {
            ioObject.setContext(getApplicationContext());
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            moveTaskToBack(true);
        }
    }

    //Sends user installed app names and specific settings to server
    private void sendApplicationList() {
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        List<String> apps = new ArrayList<>();
        for (PackageInfo pi : packageInfoList) {
            if (packageManager.getLaunchIntentForPackage(pi.packageName) != null) {
                ApplicationInfo ai = null;
                try {
                    ai = packageManager.getApplicationInfo(pi.packageName, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                //String applicationName = (String) (ai != null ? packageManager.getApplicationLabel(ai) : "(unknown)");
                Locale en = new Locale("en");
                String applicationName = ai != null ? getActive(ai.packageName, en) : "(unknown)";
                boolean cont = false;
                for(String vendor:vendors){
                    if(applicationName.contains(vendor)) {
                        cont = true;
                        break;
                    }
                }
                if(cont)
                    continue;
                apps.add(applicationName);
            }
        }
        String accessSettings = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        String bluetooth = Settings.Global.getString(getApplicationContext().getContentResolver(), Settings.Global.BLUETOOTH_ON);
        String adbEnabled = Settings.Global.getString(getApplicationContext().getContentResolver(), Settings.Global.ADB_ENABLED);
        String usbStorage = Settings.Global.getString(getApplicationContext().getContentResolver(), Settings.Global.USB_MASS_STORAGE_ENABLED);
        JSONArray appsList = new JSONArray(apps);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userID",ioObject.getUserID());
            jsonObject.put("appList",appsList);
           if(accessSettings!=null)
                jsonObject.put("ACCESSIBILITY_ENABLED",accessSettings.equals("1") ? 3 : 1);
            else
                jsonObject.put("ACCESSIBILITY_ENABLED",null);
            if(bluetooth!=null)
                jsonObject.put("BLUETOOTH_ON",bluetooth.equals("1") ? 3 : 1);
            else
                jsonObject.put("BLUETOOTH_ON",null);
            if(adbEnabled!=null)
                jsonObject.put("ADB_ENABLED",adbEnabled.equals("1") ? 3 : 1);
            else
                jsonObject.put("ADB_ENABLED",null);
            if(usbStorage!=null)
                jsonObject.put("USB_MASS_STORAGE_ENABLED",usbStorage.equals("1") ? 3 : 1);
            else
                jsonObject.put("USB_MASS_STORAGE_ENABLED",null);
            KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
            jsonObject.put("isDeviceSecure",keyguardManager.isDeviceSecure() ? 3 : 1);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                jsonObject.put("isDataEnabled",telephonyManager.isDataEnabled() ? 3 : 1);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ioObject.authorizedJsonRequest("api/data/updateApps", jsonObject, Request.Method.POST, new VolleyCallback() {
            @Override
            public void onSuccess(JSONObject responseObject) throws JSONException {
                Log.i("Upload User Data: ","Success");
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MainRecommendationFragment()).commit();
            }

            @Override
            public void onErrorResponse(JSONObject errorObject) throws JSONException {
                Log.e("Error updating user apps", errorObject.getString("Error"));
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MainRecommendationFragment()).commit();
                if(newUser){
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.get_all_recs_error), Toast.LENGTH_SHORT).show();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finishAndRemoveTask();
                        }
                    }, 3000);
                }
            }
        });
    }
    //Get user information for display in drawer
    private void getUserData() {
        ioObject.authorizedJsonRequest("api/user/getUserData", null, Request.Method.GET, new VolleyCallback() {
            @Override
            public void onSuccess(JSONObject responseObject) throws JSONException {
                String image = responseObject.getString("image");
                String mail = responseObject.getString("email");
                String uname = responseObject.getString("username");
                String userID = responseObject.getString("userID");
                if(!image.equals("null")) {
                    byte[] imageBytes = Base64.getDecoder().decode(image);
                    Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    userImage.setImageBitmap(imageBitmap);
                }
                username.setText(uname);
                email.setText(mail);
                if(ioObject.getUserID()==null) {
                    if (!userID.equals("null")) {
                        ioObject.setUserID(userID);
                    }
                    else{
                        ioObject.setUserID(getApplicationContext().getSharedPreferences("sharedPrefs", MODE_PRIVATE).getString(uname, ""));
                    }
                }
            }

            @Override
            public void onErrorResponse(JSONObject errorObject) throws JSONException {
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.user_information_fetch_error), Toast.LENGTH_SHORT).show();
                Log.e("Error getting user data: ",errorObject.getString("Error"));
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finishAndRemoveTask();
                    }
                }, 3000);
            }
        });
    }
    private void setLocale(Locale locale, String packageName){
        try{
            Context myAppContext = getApplicationContext();
            Context otherAppContext = myAppContext.createPackageContext(packageName, CONTEXT_IGNORE_SECURITY);
            Locale.setDefault(locale);
            Configuration appConfig = new Configuration();
            appConfig.locale = locale;
            otherAppContext.getResources().updateConfiguration(appConfig, getApplicationContext().getResources().getDisplayMetrics());
        }catch(PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
    }

    private String getActive(String packageName,Locale locale){
        try{
            PackageManager pm = getApplicationContext().getPackageManager();
            setLocale(locale, packageName);
            PackageInfo appInfo = pm.getPackageInfo(packageName, 0);
            return appInfo.applicationInfo.loadLabel(pm).toString();
        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        return "???";
    }
}