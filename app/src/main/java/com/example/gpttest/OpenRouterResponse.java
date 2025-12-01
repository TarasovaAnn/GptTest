package com.example.gpttest;

public class OpenRouterResponse {
    public Choices[] choices;

    public static class Choices {
        public Message message;
    }

    public static class Message {
        public String content;
    }
}