package com.example.var.data.remote;

import com.example.var.data.model.GroqRequest;
import com.example.var.data.model.GroqResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * GroqApiService - Groq yapay zeka API'sine bağlanan Retrofit arayüzü.
 *
 * Groq, yüksek hızlı LLM çıkarımı sağlayan bir API servisidir.
 * OpenAI uyumlu bir API formatı kullandığı için Retrofit ile kolayca entegre edilir.
 *
 * Base URL: https://api.groq.com/openai/v1/
 *
 * Kullanılan Model: llama-3.3-70b-versatile
 * Bu model, futbol analizi ve tahmin için yeterli kapasiteye sahiptir.
 *
 * Kullanım:
 * - MatchDetailFragment'taki "Tahmin Al" FAB butonuna tıklandığında çağrılır
 * - Yalnızca giriş yapmış kullanıcılar erişebilir
 * - Maç verilerini (takımlar, H2H, istatistikler) içeren prompt gönderilir
 */
public interface GroqApiService {

    /**
     * Groq LLM API'sine sohbet tamamlama isteği gönderir.
     *
     * @param authorization "Bearer {API_KEY}" formatında kimlik doğrulama başlığı
     * @param request       Model adı, mesaj listesi ve maksimum token içeren istek nesnesi
     * @return Groq'tan dönen yapay zeka yanıtı
     */
    @POST("chat/completions")
    Call<GroqResponse> getChatCompletion(
            @Header("Authorization") String authorization,
            @Body GroqRequest request
    );
}
