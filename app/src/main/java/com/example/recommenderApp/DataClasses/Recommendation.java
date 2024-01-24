package com.example.recommenderApp.DataClasses;

public class Recommendation {
    private Long id;
    private String recommendation;
    private String recommendationType;
    private Integer userLiked = null;
    private Integer percentOfSimilarUsersWithApp;

    public Recommendation(Long id,String recommendation, String recommendationType, Integer userLiked, Integer percentOfSimilarUsersWithApp) {
        this.id = id;
        this.recommendation = recommendation;
        this.recommendationType = recommendationType;
        this.userLiked = userLiked;
        this.percentOfSimilarUsersWithApp = percentOfSimilarUsersWithApp;
    }

    public Recommendation() {
    }

    public Long getId(){
        return id;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(String recommendationType) {
        this.recommendationType = recommendationType;
    }

    public Integer getUserLiked() {
        return userLiked;
    }

    public void setUserLiked(Integer userLiked) {
        this.userLiked = userLiked;
    }

    public Integer getPercentOfSimilarUsersWithApp(){
        return percentOfSimilarUsersWithApp;
    }

    public void setPercentOfSimilarUsersWithApp(Integer percentOfSimilarUsersWithApp){
        this.percentOfSimilarUsersWithApp = percentOfSimilarUsersWithApp;
    }
}
