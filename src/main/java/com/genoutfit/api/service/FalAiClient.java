package com.genoutfit.api.service;

import ai.fal.client.FalClient;
import ai.fal.client.SubscribeOptions;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class FalAiClient {
    @Autowired
    private FalClient falClient;

    private  OkHttpClient okHttpClient;

    @Value("${fal.api.key}")
    private String falApiKey;

    public JsonObject submitToFalApi(String endpoint, Map<String, Object> input, String webhookUrl) throws IOException {
        // Construct URL with fal_webhook parameter
        String url = String.format("https://queue.fal.run/%s", endpoint);
        if (webhookUrl != null) {
            url += String.format("?fal_webhook=%s", webhookUrl);
        }

        // Create JSON request body
        String jsonBody = new Gson().toJson(input);

        // Build request using OkHttp
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Key " + falApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        MediaType.parse("application/json"),
                        jsonBody
                ))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                log.error("HTTP Error: {} - {}", response.code(), errorBody);
                throw new RuntimeException("HTTP Error: " + response.code() + " - " + errorBody);
            }

            String responseBody = response.body() != null ? response.body().string() : "{}";
            return JsonParser.parseString(responseBody).getAsJsonObject();
        }
    }

    private String extractImageUrl(Object result) {
        // Extract URL from Fal.ai response
        if (result instanceof JsonObject) {
            JsonObject jsonResult = (JsonObject) result;
            return jsonResult.get("images")
                    .getAsJsonArray()
                    .get(0)
                    .getAsJsonObject()
                    .get("url")
                    .getAsString();
        }
        throw new RuntimeException("Invalid response format from Fal.ai");
    }
}
