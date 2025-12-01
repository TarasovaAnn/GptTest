package com.example.gpttest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface OpenRouterService {
    @Headers({
            "Authorization: Bearer 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",  // ← ВСТАВЬТЕ TOKEN!
            "Content-Type: application/json",
            "HTTP-Referer: http://localhost",  // Опционально: ваш домен
            "X-Title: Android Chat"  // Опционально
    })
    @POST("api/v1/chat/completions")
    Call<OpenRouterResponse> chat(@Body Map<String, Object> body);
}