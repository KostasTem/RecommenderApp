package com.example.recommenderApp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recommenderApp.DataClasses.Recommendation;
import com.example.recommenderApp.NetworkUtils.DataSingleton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RecommendationHistoryFragment extends Fragment {
    private DataSingleton ioObject = DataSingleton.getInstance();
    private List<Recommendation> ratedRecommendations = new ArrayList<>();
    private RecyclerView recyclerView;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_recommendations, container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ratedRecommendations = ioObject.getAllRecommendations().stream().filter(recommendation -> recommendation.getUserLiked()!=null).collect(Collectors.toList());
        createUI();
    }

    private void createUI(){
        recyclerView = getView().findViewById(R.id.recommendation_history_recycler);
        RecommendationRecyclerViewAdapter adapter = new RecommendationRecyclerViewAdapter(ratedRecommendations,null,getView().getContext().getApplicationContext());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(ioObject.getContext()));
        ioObject.setHistoryRecyclerView(adapter);
    }
}
