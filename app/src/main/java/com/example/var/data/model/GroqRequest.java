package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * GroqRequest - Groq API'ye gönderilen sohbet tamamlama isteği modeli.
 *
 * OpenAI uyumlu formatta /chat/completions endpoint'ine POST edilir.
 *
 * Örnek JSON:
 * {
 *   "model": "llama-3.3-70b-versatile",
 *   "messages": [
 *     {"role": "system", "content": "Sen bir futbol analisti..."},
 *     {"role": "user", "content": "Bu maçı tahmin et: ..."}
 *   ],
 *   "max_tokens": 1024,
 *   "temperature": 0.7
 * }
 */
public class GroqRequest {

    /**
     * Kullanılacak Groq model adı.
     * llama-3.3-70b-versatile: hızlı ve yetenekli açık kaynak model
     */
    @SerializedName("model")
    private String model;

    /** Konuşma geçmişi: sistem talimatı + kullanıcı mesajı */
    @SerializedName("messages")
    private List<Message> messages;

    /** Üretilecek maksimum token sayısı (cevabın uzunluğunu sınırlar) */
    @SerializedName("max_tokens")
    private int maxTokens;

    /**
     * Yaratıcılık seviyesi (0.0 - 2.0).
     * 0.7: Dengeli - tutarlı ama monoton olmayan yanıtlar
     */
    @SerializedName("temperature")
    private double temperature;

    /**
     * GroqRequest yapıcı metodu.
     *
     * @param model       Kullanılacak model adı
     * @param messages    Sistem ve kullanıcı mesajları listesi
     * @param maxTokens   Maksimum çıktı token sayısı
     * @param temperature Yanıt yaratıcılığı (0.0-2.0)
     */
    public GroqRequest(String model, List<Message> messages, int maxTokens, double temperature) {
        this.model = model;
        this.messages = messages;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    // ===== Getter Metodları =====
    public String getModel() { return model; }
    public List<Message> getMessages() { return messages; }
    public int getMaxTokens() { return maxTokens; }
    public double getTemperature() { return temperature; }

    /**
     * Message - Groq API sohbet mesajını temsil eden iç sınıf.
     *
     * role: "system" (AI kişiliği) veya "user" (kullanıcı mesajı)
     * content: Mesajın içeriği
     */
    public static class Message {

        /** Mesaj sahibinin rolü: "system" veya "user" */
        @SerializedName("role")
        private String role;

        /** Mesajın metin içeriği */
        @SerializedName("content")
        private String content;

        /**
         * @param role    "system" veya "user"
         * @param content Mesaj metni
         */
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
    }
}
