package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * GroqResponse - Groq API'den dönen sohbet tamamlama yanıtı modeli.
 *
 * OpenAI uyumlu format kullanır. Yanıt, choices listesindeki ilk elemanın
 * message.content alanında bulunur.
 *
 * Örnek JSON:
 * {
 *   "id": "chatcmpl-xxx",
 *   "choices": [{
 *     "message": {
 *       "role": "assistant",
 *       "content": "Maç tahmini: ..."
 *     },
 *     "finish_reason": "stop"
 *   }],
 *   "usage": {"prompt_tokens": 100, "completion_tokens": 200}
 * }
 */
public class GroqResponse {

    /** Yanıtın benzersiz ID'si */
    @SerializedName("id")
    private String id;

    /** Üretilen yanıt seçenekleri listesi (genellikle 1 eleman içerir) */
    @SerializedName("choices")
    private List<Choice> choices;

    /** Token kullanım istatistikleri */
    @SerializedName("usage")
    private Usage usage;

    /**
     * İlk seçeneğin mesaj içeriğini döndürür.
     * Kullanıcıya gösterilecek tahmin metni buradan alınır.
     *
     * @return AI tarafından üretilen tahmin metni, null ise boş string
     */
    public String getContent() {
        if (choices != null && !choices.isEmpty()
                && choices.get(0).getMessage() != null) {
            return choices.get(0).getMessage().getContent();
        }
        return "";
    }

    public List<Choice> getChoices() { return choices; }
    public Usage getUsage() { return usage; }

    /**
     * Choice - Bir yanıt seçeneğini temsil eder.
     * Groq genellikle tek bir seçenek döndürür (n=1 varsayılanı).
     */
    public static class Choice {

        /** AI'nın ürettiği yanıt mesajı */
        @SerializedName("message")
        private GroqRequest.Message message;

        /** Üretimin tamamlanma nedeni: "stop" normal, "length" token limitine ulaşıldı */
        @SerializedName("finish_reason")
        private String finishReason;

        public GroqRequest.Message getMessage() { return message; }
        public String getFinishReason() { return finishReason; }
    }

    /**
     * Usage - API'nin tükettiği token sayısını gösterir.
     * Kullanım takibi ve maliyet hesabı için faydalıdır.
     */
    public static class Usage {

        /** Gönderilen prompt için kullanılan token sayısı */
        @SerializedName("prompt_tokens")
        private int promptTokens;

        /** Üretilen yanıt için kullanılan token sayısı */
        @SerializedName("completion_tokens")
        private int completionTokens;

        /** Toplam kullanılan token sayısı */
        @SerializedName("total_tokens")
        private int totalTokens;

        public int getPromptTokens() { return promptTokens; }
        public int getCompletionTokens() { return completionTokens; }
        public int getTotalTokens() { return totalTokens; }
    }
}
