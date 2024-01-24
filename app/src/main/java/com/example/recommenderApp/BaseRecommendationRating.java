package com.example.recommenderApp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.example.recommenderApp.DataClasses.Recommendation;
import com.example.recommenderApp.NetworkUtils.DataSingleton;
import com.example.recommenderApp.NetworkUtils.VolleyCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class BaseRecommendationRating extends AppCompatActivity {
    private Integer count;
    private final List<Recommendation> baseRecommendations = Arrays.asList(new Recommendation(null,"Youtube","App",null,null),
                                                                     new Recommendation(null,"Twitter","App",null,null),
                                                                     new Recommendation(null,"Facebook","App",null,null),
                                                                     new Recommendation(null,"Twitch","App",null,null),
                                                                     new Recommendation(null,"Yahoo Finance","App",null,null),
                                                                     new Recommendation(null,"Spotify","App",null,null),
                                                                     new Recommendation(null,"Reddit","App",null,null),
                                                                     new Recommendation(null,"Steam","App",null,null),
                                                                     new Recommendation(null,"Netflix","App",null,null),
                                                                     new Recommendation(null,"Microsoft Teams","App",null,null)
                                                                     );
    private final List<Integer> baseRecommendationImages = Arrays.asList(R.drawable.youtube,
                                                                    R.drawable.twitter,
                                                                    R.drawable.facebook,
                                                                    R.drawable.twitch,
                                                                    R.drawable.yahoo_finance,
                                                                    R.drawable.spotify,
                                                                    R.drawable.reddit,
                                                                    R.drawable.steam,
                                                                    R.drawable.netflix,
                                                                    R.drawable.microsoft_teams);
    private final DataSingleton ioObject = DataSingleton.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_recommendation_rating);
        count = getIntent().getExtras().getInt("Count");
        ImageButton like = findViewById(R.id.baseRecommendationLike);
        ImageButton dislike = findViewById(R.id.baseRecommendationDislike);
        ImageButton maybe = findViewById(R.id.baseRecommendationMaybe);
        TextView countText = findViewById(R.id.baseRecommendationCountText);
        TextView nameText = findViewById(R.id.baseRecommendationAppName);
        ImageView imageView = findViewById(R.id.baseRecommendationImage);

        nameText.setText(baseRecommendations.get(count).getRecommendation());
        imageView.setImageResource(baseRecommendationImages.get(count));
        countText.setText((count+1) + "/" + baseRecommendations.size());
        like.setOnClickListener(createOnClickListener(3));
        maybe.setOnClickListener(createOnClickListener(2));
        dislike.setOnClickListener(createOnClickListener(1));
        if(getIntent().getExtras().containsKey("First")){
            startActivity(new Intent(BaseRecommendationRating.this,InformationActivity.class));
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
    //Dynamic on click listener that gets created on activity launch to rate one of the Base applications
    private View.OnClickListener createOnClickListener(int rating){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("userID",ioObject.getUserID());
                    jsonObject.put("appName",baseRecommendations.get(count).getRecommendation());
                    jsonObject.put("rating",rating);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Update the rating of the current recommendation on the server and show next recommendation on success
                ioObject.authorizedJsonRequest("api/data/insertInitialRatings", jsonObject, Request.Method.PATCH, new VolleyCallback() {
                    @Override
                    public void onSuccess(JSONObject responseObject) throws JSONException {
                        String status = responseObject.getString("Status");
                        Intent i;
                        if(count<baseRecommendations.size()-1) {
                            if (status.equals("Error")) {
                                Toast.makeText(getApplicationContext(), "Error saving rating. Please try again.", Toast.LENGTH_SHORT).show();
                                i = new Intent(BaseRecommendationRating.this, BaseRecommendationRating.class);
                                i.putExtra("Count", count);
                            } else {
                                i = new Intent(BaseRecommendationRating.this, BaseRecommendationRating.class);
                                i.putExtra("Count", count + 1);
                            }
                        }
                        else{
                            i = new Intent(BaseRecommendationRating.this,MainApp.class);
                            i.putExtra("newUser","1");
                        }
                        startActivity(i);
                        overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
                        finish();
                    }

                    @Override
                    public void onErrorResponse(JSONObject errorObject) throws JSONException {
                        Log.e("Error inserting base recommendation ratings",errorObject.getString("Error"));
                    }
                });
            }
        };
    }
}