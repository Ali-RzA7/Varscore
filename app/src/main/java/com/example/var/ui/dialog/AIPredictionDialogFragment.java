package com.example.var.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.var.BuildConfig;
import com.example.var.R;
import com.example.var.data.model.GroqRequest;
import com.example.var.data.model.GroqResponse;
import com.example.var.data.model.MatchModel;
import com.example.var.data.remote.GroqRetrofitClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.noties.markwon.Markwon;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AIPredictionDialogFragment - Yapay Zeka Maç Tahmin Dialog'u.
 *
 * MatchDetailFragment'taki "Tahmin Al" FAB butonuna tıklandığında açılır.
 * Yalnızca giriş yapmış kullanıcılar erişebilir.
 *
 * Çalışma Akışı:
 * 1. Dialog açılır ve yükleniyor durumu gösterilir
 * 2. Maç verileri (takım adları, lig, skor) Groq API'ye prompt olarak gönderilir
 * 3. LLM (llama-3.3-70b-versatile) tahmin ve analiz üretir
 * 4. Yanıt dialog'da gösterilir
 *
 * Parametre:
 * - ARG_MATCH: Analiz edilecek maç verisi (Bundle üzerinden MatchModel)
 *
 * Groq API:
 * - Base URL: https://api.groq.com/openai/v1/
 * - Model: llama-3.3-70b-versatile
 * - API Key: BuildConfig.GROQ_API_KEY (local.properties'ten BuildConfig üzerinden)
 */
public class AIPredictionDialogFragment extends DialogFragment {

    /** Bundle argüman anahtarı (maç ID ve bilgileri) */
    private static final String ARG_HOME_TEAM = "home_team";
    private static final String ARG_AWAY_TEAM = "away_team";
    private static final String ARG_LEAGUE_NAME = "league_name";
    private static final String ARG_HOME_SCORE = "home_score";
    private static final String ARG_AWAY_SCORE = "away_score";
    private static final String ARG_STATUS = "status";

    /** Groq LLM model adı */
    private static final String GROQ_MODEL = "llama-3.3-70b-versatile";

    /** Maksimum üretilecek token sayısı */
    private static final int MAX_TOKENS = 1024;

    /** View referansları */
    private View loadingContainer;
    private View resultContainer;
    private View errorContainer;
    private android.widget.TextView tvPrediction;
    private android.widget.TextView tvError;

    /**
     * Fragment oluşturma fabrika metodu.
     *
     * @param match Tahmin yapılacak maç verisi
     * @return Maç verileri yüklenmiş AIPredictionDialogFragment
     */
    public static AIPredictionDialogFragment newInstance(MatchModel match) {
        AIPredictionDialogFragment fragment = new AIPredictionDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_HOME_TEAM, match.getHomeName());
        args.putString(ARG_AWAY_TEAM, match.getAwayName());
        args.putString(ARG_LEAGUE_NAME, match.getLeagueName());
        args.putInt(ARG_HOME_SCORE, match.getHomeScore());
        args.putInt(ARG_AWAY_SCORE, match.getAwayScore());
        args.putInt(ARG_STATUS, match.getStatus());
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ai_prediction, null);

        // View referanslarını al
        loadingContainer = view.findViewById(R.id.loadingContainer);
        resultContainer = view.findViewById(R.id.resultContainer);
        errorContainer = view.findViewById(R.id.errorContainer);
        tvPrediction = view.findViewById(R.id.tvPrediction);
        tvError = view.findViewById(R.id.tvError);

        // Kapat butonu
        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());

        // Dialog oluşturulduğunda hemen tahmin API isteğini başlat
        if (getArguments() != null) {
            fetchAIPrediction(
                    getArguments().getString(ARG_HOME_TEAM, ""),
                    getArguments().getString(ARG_AWAY_TEAM, ""),
                    getArguments().getString(ARG_LEAGUE_NAME, ""),
                    getArguments().getInt(ARG_HOME_SCORE, 0),
                    getArguments().getInt(ARG_AWAY_SCORE, 0),
                    getArguments().getInt(ARG_STATUS, 0)
            );
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }

    /**
     * Groq API'ye maç verilerini içeren prompt gönderir ve tahmin alır.
     *
     * Prompt yapısı:
     * - Sistem mesajı: AI'ya futbol analisti rolü verir
     * - Kullanıcı mesajı: Maç detayları (takımlar, lig, mevcut skor, durum)
     *
     * @param homeTeam   Ev sahibi takım adı
     * @param awayTeam   Deplasman takımı adı
     * @param leagueName Lig adı
     * @param homeScore  Ev sahibi skoru
     * @param awayScore  Deplasman skoru
     * @param status     Maç durumu (0=başlamadı, 1-5=canlı, -1=bitti)
     */
    private void fetchAIPrediction(String homeTeam, String awayTeam, String leagueName,
            int homeScore, int awayScore, int status) {
        showLoading();

        // Maç durumu metni
        String statusText = getStatusText(status);

        // Sistem mesajı: AI'ya futbol analisti kimliği ver
        String systemPrompt = getString(R.string.ai_system_prompt);


        // Kullanıcı mesajı: Maç detayları
        String userPrompt = getString(R.string.ai_user_prompt,
                leagueName, homeTeam, awayTeam, homeScore, awayScore, statusText);


        // Groq isteği oluştur
        List<GroqRequest.Message> messages = new ArrayList<>();
        messages.add(new GroqRequest.Message("system", systemPrompt));
        messages.add(new GroqRequest.Message("user", userPrompt));

        GroqRequest request = new GroqRequest(GROQ_MODEL, messages, MAX_TOKENS, 0.7);

        // Groq API anahtarı (local.properties'ten BuildConfig üzerinden)
        String authHeader = "Bearer " + BuildConfig.GROQ_API_KEY;

        // API isteğini gönder
        GroqRetrofitClient.getInstance().getGroqService()
                .getChatCompletion(authHeader, request)
                .enqueue(new Callback<GroqResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<GroqResponse> call,
                            @NonNull Response<GroqResponse> response) {
                        if (!isAdded()) return;

                        if (response.isSuccessful() && response.body() != null) {
                            String content = response.body().getContent();
                            if (content != null && !content.isEmpty()) {
                                showResult(content);
                            } else {
                                showError(getString(R.string.ai_error));
                            }
                        } else {
                            showError(getString(R.string.ai_error));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<GroqResponse> call,
                            @NonNull Throwable t) {
                        if (!isAdded()) return;
                        showError(getString(R.string.error_loading));
                    }
                });
    }

    /**
     * Maç durum kodunu kullanıcı dostu metne çevirir.
     *
     * @param status iSportsAPI durum kodu
     * @return Türkçe durum açıklaması
     */
    private String getStatusText(int status) {
        switch (status) {
            case 0: return getString(R.string.status_not_started);
            case 1: return getString(R.string.status_first_half);
            case 2: return getString(R.string.status_half_time);
            case 3: return getString(R.string.status_second_half);
            case 4: return getString(R.string.status_extra_time);
            case 5: return getString(R.string.status_penalties);
            case -1: return getString(R.string.status_finished_full);
            default: return getString(R.string.unknown);
        }
    }


    // ===== UI Durum Metodları =====

    /** Yükleniyor durumunu gösterir */
    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        resultContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
    }

    /**
     * AI tahmin sonucunu gösterir.
     *
     * @param prediction Groq LLM'den gelen tahmin metni
     */
    private void showResult(String prediction) {
        loadingContainer.setVisibility(View.GONE);
        resultContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        
        // Markdown formatında render et
        final Markwon markwon = Markwon.create(requireContext());
        markwon.setMarkdown(tvPrediction, prediction);
    }

    /**
     * Hata durumunu gösterir.
     *
     * @param message Kullanıcıya gösterilecek hata mesajı
     */
    private void showError(String message) {
        loadingContainer.setVisibility(View.GONE);
        resultContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        tvError.setText(message);
    }
}
