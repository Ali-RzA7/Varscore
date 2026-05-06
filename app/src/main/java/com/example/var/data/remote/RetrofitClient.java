package com.example.var.data.remote;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * RetrofitClient - Retrofit singleton istemcisi.
 * Uygulamanın tüm ağ istekleri bu merkezi istemci üzerinden yapılır.
 *
 * Özellikler:
 * - Thread-safe Singleton pattern (Double-Checked Locking)
 * - OkHttp loglama interceptor'ı (debug modda HTTP isteklerini loglar)
 * - Bağlantı zaman aşımı ayarları (30 saniye)
 * - Gson dönüştürücü (JSON -> Java nesnesi)
 */
public class RetrofitClient {

    /** iSportsAPI base URL'i */
    private static final String BASE_URL = "http://api.isportsapi.com/sport/football/";

    /** Singleton Retrofit instance */
    private static volatile RetrofitClient instance;

    /** Retrofit nesnesi */
    private final Retrofit retrofit;

    /**
     * Private constructor - Singleton pattern.
     * Retrofit ve OkHttp istemcisini yapılandırır.
     */
    private RetrofitClient() {
        // HTTP isteklerini loglamak için interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // OkHttp istemcisi - zaman aşımı ve loglama ayarları
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0")
                        .build()))
                .build();

        // Retrofit yapılandırması
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * Singleton instance'ı döndürür.
     * Double-Checked Locking ile thread-safe erişim sağlar.
     *
     * @return RetrofitClient singleton instance
     */
    public static RetrofitClient getInstance() {
        if (instance == null) {
            synchronized (RetrofitClient.class) {
                if (instance == null) {
                    instance = new RetrofitClient();
                }
            }
        }
        return instance;
    }

    /**
     * FootballApiService arayüzünün implementasyonunu döndürür.
     * Bu metotla API çağrıları yapılabilir.
     *
     * @return FootballApiService implementasyonu
     */
    public FootballApiService getApiService() {
        return retrofit.create(FootballApiService.class);
    }
}
