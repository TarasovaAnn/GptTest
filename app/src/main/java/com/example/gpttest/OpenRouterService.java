package com.example.gpttest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface OpenRouterService {
    @Headers({
            "Authorization: Bearer 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",
            "HTTP-Referer: http://localhost",
            "X-Title: Android Chat"
    })
    @POST("api/v1/chat/completions")
    Call<OpenRouterResponse> chat(@Body Map<String, Object> body);
}