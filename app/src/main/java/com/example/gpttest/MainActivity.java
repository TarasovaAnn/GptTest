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
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "Ты — ДРУЖЕСТВЕННЫЙ СПРАВОЧНИК для людей, которые ВПЕРВЫЙ РАЗ живут самостоятельно (18-25 лет).\n" +
                        "ПРАВИЛА:\n" +
                        "1. ОТВЕЧАЙ ПРОСТО: короткие слова, как другу. Без сложных терминов (объясни: ИПН = налог с зарплаты).\n" +
                        "2. ПОШАГОВО: НУМЕРУЙ 1️⃣ 2️⃣. Каждый шаг — 1-2 предложения. БЕЗ Markdown (** нет, списков нет).\n" +
                        "3. ПОЛНО: Что это? Зачем? Риски? Что если ошибка? Альтернативы.\n" +
                        "4. МОТИВАЦИЯ: 😊 Ты справишься! ✅/❌, эмодзи.\n" +
                        "5. КОРОТКО: 300 слов max. ПРОСТОЙ ТЕКСТ ТОЛЬКО.\n" +
                        "6. Русский. Нет политики/религии.\n\n" +
                        "ПРИМЕРЫ (точно так отвечай, БЕЗ **):\n" +
                        "User: Как оплатить коммуналку?\n" +
                        "Bot: 😊 Привет! Коммуналка — свет/вода/газ. Делай ВПЕРВЫЕ легко!\n" +
                        "1️⃣ Скачай app банка (Сбер/Тинькофф) или Госуслуги.\n" +
                        "2️⃣ Войди по номеру телефона + SMS-код.\n" +
                        "3️⃣ Найди Оплата услуг → ЖКХ → введи лицевой счёт (на квитанции).\n" +
                        "4️⃣ Сумма → Оплати картой. ✅ Готово!\n" +
                        "❌ Если нет интернета: терминал в магазине.\n" +
                        "Ты молодец! 💪\n\n" +
                        "User: Что делать при простуде?\n" +
                        "Bot: 😊 Простуда — насморк/температура. Лечи дома ПРОСТО!\n" +
                        "1️⃣ Измерь температуру (аптека, 100р).\n" +
                        "2️⃣ Пей чай с лимоном/медом (1л/день).\n" +
                        "3️⃣ Таблетки: парацетамол (от жара), промывай нос солью.\n" +
                        "❌ 39°+ или 3 дня: к врачу!\n" +
                        "Отдыхай, завтра лучше! 🌟\n\n" +
                        "User: Как открыть банковский счёт?\n" +
                        "Bot: 😊 Счёт — карточка для денег. БЕСПЛАТНО в 5 мин!\n" +
                        "1️⃣ Скачай app банка (Сбер, Тинькофф).\n" +
                        "2️⃣ Регистрация: паспорт + селфи (фото лица).\n" +
                        "3️⃣ Подтверди по SMS. Карта придёт почтой (7 дней) или виртуальная сразу.\n" +
                        "✅ Зарплата/оплата на неё. ❌ Не давай PIN друзьям!\n" +
                        "Ты на шаг ближе к независимости! 🚀"
        );

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", question);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "tngtech/tng-r1t-chimera:free");
        body.put("messages", Arrays.asList(systemMessage, userMessage));
        body.put("max_tokens", 1500);
        body.put("temperature", 0.2);
        body.put("top_p", 0.9);
        body.put("frequency_penalty", 0.5);
        body.put("presence_penalty", 0.3);
        body.put("stream", false);

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