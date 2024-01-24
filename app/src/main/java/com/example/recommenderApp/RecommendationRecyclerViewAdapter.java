package com.example.recommenderApp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.example.recommenderApp.DataClasses.Recommendation;
import com.example.recommenderApp.NetworkUtils.DataSingleton;
import com.example.recommenderApp.NetworkUtils.VolleyCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

public class RecommendationRecyclerViewAdapter extends RecyclerView.Adapter<RecommendationRecyclerViewAdapter.ViewHolder> {

    private List<Recommendation> recommendations;
    private final TextView noRecommendationText;
    private final DataSingleton dataSingleton = DataSingleton.getInstance();
    private final Context mContext;

    public RecommendationRecyclerViewAdapter(List<Recommendation> recommendations,TextView noRecommendationText, Context mContext) {
        this.recommendations = recommendations;
        this.noRecommendationText = noRecommendationText;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recommendation_item,parent,false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recommendation recommendation = recommendations.get(position);
        holder.recommendationText.setText(recommendation.getRecommendation());
        holder.likeButton.setOnClickListener(createOnClickListener(recommendation,3, holder.likeButton, holder.dontKnowButton ,holder.dislikeButton));
        holder.dontKnowButton.setOnClickListener(createOnClickListener(recommendation,2,holder.likeButton, holder.dontKnowButton, holder.dislikeButton));
        holder.dislikeButton.setOnClickListener(createOnClickListener(recommendation,1, holder.likeButton, holder.dontKnowButton, holder.dislikeButton));
        if(recommendation.getRecommendationType().equals("app")){
            holder.recommendationImage.setImageResource(R.mipmap.ic_launcher);
        }
        //Set the percentage of similar users that use the app of this recommendation if it exists
        if(recommendation.getPercentOfSimilarUsersWithApp()==null){
            holder.recommendationPercentText.setText("");
        }
        else{
            String percent = recommendation.getPercentOfSimilarUsersWithApp() + "%";
            String text = String.format(mContext.getResources().getString(R.string.similar_users_percent_usage), percent);
            holder.recommendationPercentText.setText(text);
        }
        //If recommendation has been rated, change the background color and disable the button of their rating
        if(recommendation.getUserLiked()!=null) {
            if (recommendation.getUserLiked()==3) {
                holder.likeButton.setEnabled(false);
                holder.likeButton.setBackgroundResource(R.color.Gray);
                holder.dontKnowButton.setEnabled(true);
                holder.dontKnowButton.setBackgroundResource(R.color.white);
                holder.dislikeButton.setEnabled(true);
                holder.dislikeButton.setBackgroundResource(R.color.white);
            } else if(recommendation.getUserLiked()==1){
                holder.dislikeButton.setEnabled(false);
                holder.dislikeButton.setBackgroundResource(R.color.Gray);
                holder.dontKnowButton.setEnabled(true);
                holder.dontKnowButton.setBackgroundResource(R.color.white);
                holder.likeButton.setEnabled(true);
                holder.likeButton.setBackgroundResource(R.color.white);
            }
            else{
                holder.dislikeButton.setEnabled(true);
                holder.dislikeButton.setBackgroundResource(R.color.white);
                holder.dontKnowButton.setEnabled(false);
                holder.dontKnowButton.setBackgroundResource(R.color.Gray);
                holder.likeButton.setEnabled(true);
                holder.likeButton.setBackgroundResource(R.color.white);
            }
        }
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }
    //Returns on click listener for each rating button and recommendation that gets added to the UI
    private View.OnClickListener createOnClickListener(Recommendation recommendation,Integer rating,ImageButton likeButton,ImageButton dontKnow,ImageButton dislikeButton){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("rating",rating);
                    jsonObject.put("id",recommendation.getId());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                dataSingleton.authorizedJsonRequest("api/data/updateRating", jsonObject, Request.Method.PATCH, new VolleyCallback() {
                    @Override
                    public void onSuccess(JSONObject responseObject) throws JSONException {
                        Log.i("Update Recommendation Status",responseObject.getString("Status"));
                        if(recommendation.getUserLiked()==null){
                            int position = recommendations.indexOf(recommendation);
                            recommendations.remove(recommendation);
                            dataSingleton.getMainAppRecyclerView().notifyItemRemoved(position);
                            recommendation.setUserLiked(rating);
                            Objects.requireNonNull(dataSingleton.getAllRecommendations().stream().filter(recommendation1 -> recommendation1.getId().equals(recommendation.getId())).findFirst().orElse(null)).setUserLiked(rating);
                            if(dataSingleton.getHistoryRecyclerView()!=null){
                                dataSingleton.getHistoryRecyclerView().recommendations.add(recommendation);
                                dataSingleton.getHistoryRecyclerView().notifyItemInserted(dataSingleton.getHistoryRecyclerView().recommendations.size() - 1);
                            }
                            if(dataSingleton.getMainAppRecyclerView().getItemCount()==0 && noRecommendationText!=null){
                                noRecommendationText.setVisibility(View.VISIBLE);
                                noRecommendationText.setEnabled(true);
                            }
                        }
                        else{
                            recommendation.setUserLiked(rating);
                            updateImageButtons(recommendation,likeButton,dontKnow,dislikeButton);
                        }
                    }

                    @Override
                    public void onErrorResponse(JSONObject errorObject) throws JSONException {
                        Log.e("Update Recommendation Rating Error",errorObject.getString("Error"));
                    }
                });
            }
        };
    }
    //Runs when the user changes their rating of a recommendation to show their new rating and hide the old one
    private void updateImageButtons(Recommendation recommendation,ImageButton likeButton,ImageButton dontKnow,ImageButton dislikeButton){
        if (recommendation.getUserLiked()==3) {
            likeButton.setEnabled(false);
            likeButton.setBackgroundResource(R.color.Gray);
            dontKnow.setEnabled(true);
            dontKnow.setBackgroundResource(R.color.white);
            dislikeButton.setEnabled(true);
            dislikeButton.setBackgroundResource(R.color.white);
        } else if(recommendation.getUserLiked()==1){
            dislikeButton.setEnabled(false);
            dislikeButton.setBackgroundResource(R.color.Gray);
            dontKnow.setEnabled(true);
            dontKnow.setBackgroundResource(R.color.white);
            likeButton.setEnabled(true);
            likeButton.setBackgroundResource(R.color.white);
        }
        else{
            dislikeButton.setEnabled(true);
            dislikeButton.setBackgroundResource(R.color.white);
            dontKnow.setEnabled(false);
            dontKnow.setBackgroundResource(R.color.Gray);
            likeButton.setEnabled(true);
            likeButton.setBackgroundResource(R.color.white);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private ImageView recommendationImage;
        private TextView recommendationText,recommendationPercentText;
        private ImageButton likeButton,dislikeButton,dontKnowButton;
        private RelativeLayout relativeLayout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recommendationImage = itemView.findViewById(R.id.recommendationImage);
            recommendationText = itemView.findViewById(R.id.recommendationText);
            recommendationPercentText = itemView.findViewById(R.id.recommendationPercent);
            likeButton = itemView.findViewById(R.id.recommendationLike);
            dislikeButton = itemView.findViewById(R.id.recommendationDislike);
            dontKnowButton = itemView.findViewById(R.id.recommendationMaybe);
            relativeLayout = itemView.findViewById(R.id.recommendationLayout);
        }
    }
}

