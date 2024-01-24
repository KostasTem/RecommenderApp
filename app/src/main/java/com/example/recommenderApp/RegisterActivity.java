package com.example.recommenderApp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.example.recommenderApp.NetworkUtils.DataSingleton;
import com.example.recommenderApp.NetworkUtils.VolleyCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {
    EditText email, username, password, confirmPassword;
    ImageView imageView;
    DataSingleton ioObject = DataSingleton.getInstance();
    ActivityResultLauncher<Intent> imageSelectionLauncher;
    SharedPreferences sharedPreferences;
    String image;
    String imageName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Register");
        setContentView(R.layout.activity_register);
        sharedPreferences = getApplicationContext().getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        email = findViewById(R.id.newEmail);
        username = findViewById(R.id.newUsername);
        username.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(20)});
        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(i2>20){
                    Toast.makeText(getApplicationContext(),getApplicationContext().getResources().getString(R.string.username_character_limit_toast),Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(i2>=20){
                    Toast.makeText(getApplicationContext(),getApplicationContext().getResources().getString(R.string.password_char_limit),Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
        password = findViewById(R.id.newPassword);
        password.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(20)});
        password.addTextChangedListener(textWatcher);
        confirmPassword = findViewById(R.id.newConfirmPassword);
        confirmPassword.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(20)});
        confirmPassword.addTextChangedListener(textWatcher);
        imageView = findViewById(R.id.userImage);
        ioObject.setContext(getApplicationContext());
        //Launches activity for setting an images for the user profile
        imageSelectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Intent data = result.getData();
                            Uri filePath = data.getData();
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                                Bitmap lastBitmap = null;
                                lastBitmap = bitmap;
                                image = getStringImage(lastBitmap);
                                imageView.setImageBitmap(lastBitmap);
                                Cursor returnCursor =
                                        getContentResolver().query(filePath, null, null, null, null);
                                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                returnCursor.moveToFirst();
                                imageName =returnCursor.getString(nameIndex);
                                returnCursor.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    public void register_user(View view) throws JSONException {
        if(!password.getText().toString().equals(confirmPassword.getText().toString())){
            Toast.makeText(getApplicationContext(),getApplicationContext().getResources().getString(R.string.passwords_dont_match_toast),Toast.LENGTH_LONG).show();
        }
        else {
            if(!Patterns.EMAIL_ADDRESS.matcher(email.getText().toString().trim()).matches()){
                Toast.makeText(getApplicationContext(),getApplicationContext().getResources().getString(R.string.use_proper_email),Toast.LENGTH_SHORT).show();
                return;
            }
            if(password.getText().toString().length()<6){
                Toast.makeText(getApplicationContext(),getApplicationContext().getResources().getString(R.string.password_length),Toast.LENGTH_SHORT).show();
                return;
            }
            if(username.getText().toString().length()==0){
                Toast.makeText(getApplicationContext(), getString(R.string.register_username_error), Toast.LENGTH_SHORT).show();
                return;
            }
            JSONObject register_data = new JSONObject();
            register_data.put("Email", email.getText().toString());
            register_data.put("Username", username.getText().toString());
            register_data.put("Password", password.getText().toString());
            register_data.put("image",image);
            register_data.put("imageName", imageName);
            String uname = username.getText().toString();
            ioObject.unauthorizedJsonRequest("api/user/register",
                    register_data,
                    Request.Method.POST,
                    new VolleyCallback() {
                        @Override
                        public void onSuccess(JSONObject responseObject) throws JSONException {
                            Toast.makeText(RegisterActivity.this, getString(R.string.registration_success), Toast.LENGTH_LONG).show();
                            if(!sharedPreferences.contains(uname)){
                                String uid = UUID.randomUUID().toString();
                                ioObject.registerUserID(uid,uname);
                            }
                        }
                        @Override
                        public void onErrorResponse(JSONObject errorObject) throws JSONException {
                            if(errorObject.getString("Error").contains("username")) {
                                Toast.makeText(RegisterActivity.this, getApplicationContext().getResources().getString(R.string.registration_error_username), Toast.LENGTH_LONG).show();
                            }
                            else if(errorObject.getString("Error").contains("email")){
                                Toast.makeText(RegisterActivity.this, getString(R.string.registration_error_email), Toast.LENGTH_SHORT).show();
                            }
                            Log.e("Error registering: ",errorObject.getString("Error"));
                        }
                    });
            clearFields();
        }
    }

    public void openLoginAct(View view){
        clearFields();
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
    }

    private void clearFields(){
        username.setText("");
        password.setText("");
        confirmPassword.setText("");
        email.setText("");
        imageView.setImageResource(R.drawable.ic_baseline_person);
    }
    public void selectImage(View view) {
//        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
//        chooseFile.setType("*/*");
//        chooseFile = Intent.createChooser(chooseFile, "Choose a file");
//        startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageIntent.setTypeAndNormalize("image/*");
        pickImageIntent.putExtra("aspectX", 1);
        pickImageIntent.putExtra("aspectY", 1);
        pickImageIntent.putExtra("scale", true);
        pickImageIntent.putExtra("outputFormat",
                Bitmap.CompressFormat.JPEG.toString());
        imageSelectionLauncher.launch(pickImageIntent);
    }
    public String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);

    }


}