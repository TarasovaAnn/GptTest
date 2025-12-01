package com.example.gpttest;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    TextView welcomeTextView;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    private OpenRouterService routerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);
        recyclerView.setHasFixedSize(false);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            ViewCompat.onApplyWindowInsets(v, insets);
            return insets;
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://openrouter.ai/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        routerService = retrofit.create(OpenRouterService.class);

        // Send button
        sendButton.setOnClickListener(v -> {
            String question = messageEditText.getText().toString().trim();
            if (!question.isEmpty()) {
                addToChat(question, Message.SENT_BY_ME);
                messageEditText.setText("");
                welcomeTextView.setVisibility(View.GONE);
                callRouterAPI(question);
            }
        });
        messageEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick();
                return true;
            }
            return false;
        });
    }

    void addToChat(String message, String sentBy) {
        int position = messageList.size();
        messageList.add(new Message(message, sentBy));
        messageAdapter.notifyItemInserted(position);
        recyclerView.smoothScrollToPosition(position);
    }

    void callRouterAPI(String question) {
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", question);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "tngtech/tng-r1t-chimera:free");
        body.put("messages", Arrays.asList(userMessage));
        body.put("max_tokens", 2000);
        body.put("temperature", 0.3);

        routerService.chat(body).enqueue(new Callback<OpenRouterResponse>() {
            @Override
            public void onResponse(Call<OpenRouterResponse> call, Response<OpenRouterResponse> response) {
                Log.d("ROUTER_API", "Code: " + response.code());

                if (response.isSuccessful() && response.body() != null
                        && response.body().choices != null && response.body().choices.length > 0) {
                    String botResponse = "";
                    if (response.body().choices[0].message != null) {
                        botResponse = response.body().choices[0].message.content;
                    }
                    if (botResponse == null || botResponse.isEmpty()) {
                        botResponse = "Интересно! 😊";
                    }
                    addToChat(botResponse, Message.SENT_BY_BOT);
                } else {
                    String errorMsg = getErrorMessage(response.code(), response.message());
                    addToChat(errorMsg, Message.SENT_BY_BOT);
                }
            }

            @Override
            public void onFailure(Call<OpenRouterResponse> call, Throwable t) {
                Log.e("ROUTER_API", "Failure: " + t.getMessage(), t);
                addToChat("Ошибка сети: " + t.getMessage(), Message.SENT_BY_BOT);
            }
        });
    }

    private String getErrorMessage(int code, String message) {
        if (code == 401 || code == 403) {
            return "Токен неверный.";
        } else if (code == 402) {
            return "Баланс 0. Новый ключ.";
        } else if (code == 429) {
            return "Лимит (429). Подождите 30 сек.";
        } else if (code == 404) {
            return "Модель не найдена.";
        } else {
            return "OpenRouter (" + code + "): " + message;
        }
    }
}