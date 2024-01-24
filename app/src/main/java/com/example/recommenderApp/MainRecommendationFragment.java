package com.example.recommenderApp;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.example.recommenderApp.DataClasses.Recommendation;
import com.example.recommenderApp.NetworkUtils.DataSingleton;
import com.example.recommenderApp.NetworkUtils.VolleyCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainRecommendationFragment extends Fragment {
    private final DataSingleton ioObject = DataSingleton.getInstance();
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Recommendation> unratedRecommendations = new ArrayList<>();
    private TextView noRecommendationsText;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_recommendations, container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        getRecommendations();
        noRecommendationsText = getView().findViewById(R.id.noRecommendationsText);
        swipeRefreshLayout = getView().findViewById(R.id.refreshRecommendation);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(!ioObject.isBlockRequests()) {
                    swipeRefreshLayout.setRefreshing(true);
                    getNewRecommendations(view);
                }
            }
        });
        if(ioObject.getRecOnActivityLaunch()) {
            getNewRecommendations(view);
            ioObject.setGetRecOnActivityLaunch(false);
        }
    }
    //Get all recommendations for this user and store them in the DataSingleton
    private void getRecommendations(){
        String url = "api/data/getAllRecommendations/" + ioObject.getUserID();
        ioObject.authorizedJsonRequest(url, null, Request.Method.GET, new VolleyCallback() {
            @Override
            public void onSuccess(JSONObject responseObject) throws JSONException {
                JSONArray jsonArray = responseObject.getJSONArray("Recommendations");
                List<Recommendation> recommendationList = new ArrayList<>();
                for(int i=0;i<jsonArray.length();i++){
                    JSONObject temp = jsonArray.getJSONObject(i);
                    Long id = temp.getLong("id");
                    String recommendationText = temp.getString("recommendation");
                    String recommendationType = temp.getString("recommendationType");
                    Integer userLiked = null;
                    if(!temp.getString("userRating").equals("null")) {
                        userLiked = temp.getInt("userRating");
                    }
                    Integer percentOfSimilarUsersWithApp = null;
                    if(!temp.getString("percentOfSimilarUsersWithApp").equals("null")){
                        percentOfSimilarUsersWithApp = temp.getInt("percentOfSimilarUsersWithApp");
                    }
                    Recommendation recommendation = new Recommendation(id, recommendationText, recommendationType, userLiked,percentOfSimilarUsersWithApp);
                    recommendationList.add(recommendation);
                }
                ioObject.setAllRecommendations(recommendationList);
                unratedRecommendations = ioObject.getAllRecommendations().stream().filter(recommendation -> recommendation.getUserLiked()==null).collect(Collectors.toList());
                createUI();
            }

            @Override
            public void onErrorResponse(JSONObject errorObject) throws JSONException {
                Log.e("Unable to fetch recommendations: ",errorObject.getString("Error"));
                Toast.makeText(getContext().getApplicationContext(), getString(R.string.get_all_recs_error), Toast.LENGTH_SHORT).show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        requireActivity().finishAndRemoveTask();
                    }
                }, 3000);
            }
        });
    }
    //Get new recommendation if one exists
    private void getNewRecommendations(View view){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userID", ioObject.getUserID());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ioObject.authorizedJsonRequest("api/data/getNewRecommendation", jsonObject, Request.Method.POST, new VolleyCallback() {
            @Override
            public void onSuccess(JSONObject responseObject) throws JSONException {
                String status = responseObject.getString("Status");
                if(!status.equals("None")) {
                    JSONObject recommendationObject = responseObject.getJSONObject("Recommendation");
                    Long id = recommendationObject.getLong("id");
                    String recommendationText = recommendationObject.getString("recommendation");
                    String recommendationType = recommendationObject.getString("recommendationType");
                    Integer percentOfSimilarUsersWithApp = recommendationObject.getInt("percentOfSimilarUsersWithApp");
                    Recommendation recommendation = new Recommendation(id, recommendationText, recommendationType, null,percentOfSimilarUsersWithApp);
                    ioObject.getAllRecommendations().add(recommendation);
                    unratedRecommendations.add(recommendation);
                    ioObject.getMainAppRecyclerView().notifyItemInserted(unratedRecommendations.size() - 1);
                }
                else{
                    Toast.makeText(view.getContext().getApplicationContext(), getString(R.string.no_recommendations_toast), Toast.LENGTH_SHORT).show();
                }
                updateNoRecommendationsVisibility();
                swipeRefreshLayout.setRefreshing(false);
            }
            @Override
            public void onErrorResponse(JSONObject errorObject) throws JSONException {
                swipeRefreshLayout.setRefreshing(false);
                if(errorObject.has("Error"))
                    Log.e("Error fetching new recommendations: ", errorObject.getString("Error"));
            }
        });
    }
    private void createUI(){
        RecyclerView recyclerView = getView().findViewById(R.id.mainAppRecycler);
        RecommendationRecyclerViewAdapter adapter = new RecommendationRecyclerViewAdapter(unratedRecommendations,noRecommendationsText,getView().getContext().getApplicationContext());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(ioObject.getContext()));
        ioObject.setMainAppRecyclerView(adapter);
        updateNoRecommendationsVisibility();
    }
    private void updateNoRecommendationsVisibility(){
        if(unratedRecommendations.size()>0){
            noRecommendationsText.setVisibility(View.INVISIBLE);
            noRecommendationsText.setEnabled(false);
        }
        else{
            noRecommendationsText.setVisibility(View.VISIBLE);
            noRecommendationsText.setEnabled(true);
        }
    }

}
