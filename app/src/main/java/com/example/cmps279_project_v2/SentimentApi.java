package com.example.cmps279_project_v2;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SentimentApi {
    @POST("/analyze")
    Call<List<SentimentResponse>> analyzeSentiment(@Body SentimentRequest request);
}


