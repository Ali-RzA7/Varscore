package com.example.var.data.remote;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * GroqRetrofitClient - Groq API için Singleton Retrofit istemcisi.
 *
 * iSportsAPI'den farklı bir base URL ve kimlik doğrulama mekanizması
 * kullandığı için ayrı bir Retrofit instance yönetilir.
 *
 * Groq API Bilgileri:
 * - Base URL: https://api.groq.com/openai/v1/
 * - Kimlik doğrulama: Header'da "Authorization: Bearer {API_KEY}"
 * - Timeout: 60 saniye (LLM yanıtı daha uzun sürebilir)
 *
 * API anahtarı: local.properties'ten BuildConfig.GROQ_API_KEY olarak okunur
 */
public class GroqRetrofitClient {

    /** Groq API base URL'si */
    private static final String BASE_URL = "https://api.groq.com/openai/v1/";

    /** Thread-safe Singleton pattern için volatile instance */
    private static volatile GroqRetrofitClient instance;

    /** Retrofit instance referansı */
    private final Retrofit retrofit;

    /** Özel constructor - dışarıdan oluşturmayı engeller */
    private GroqRetrofitClient() {
        // HTTP isteklerini loglayan interceptor (debug için)
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // OkHttp istemcisi: LLM yanıtları için 60 saniyelik timeout
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)   // LLM yanıtı zaman alabilir
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        // Retrofit örneğini Groq URL'si ile oluştur
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * Thread-safe Singleton erişimi sağlar.
     * İlk çağrıda oluşturulur, sonraki çağrılarda aynı instance döner.
     *
     * @return GroqRetrofitClient tekil örneği
     */
    public static GroqRetrofitClient getInstance() {
        if (instance == null) {
            synchronized (GroqRetrofitClient.class) {
                if (instance == null) {
                    instance = new GroqRetrofitClient();
                }
            }
        }
        return instance;
    }

    /**
     * GroqApiService Retrofit arayüzünü oluşturur ve döndürür.
     *
     * @return Groq API çağrıları için hazır servis arayüzü
     */
    public GroqApiService getGroqService() {
        return retrofit.create(GroqApiService.class);
    }
}
