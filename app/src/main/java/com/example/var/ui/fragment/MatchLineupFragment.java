package com.example.var.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.example.var.BuildConfig;
import com.example.var.data.model.AnalysisModel;
import com.example.var.data.model.AnalysisResponse;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.LineupModel;
import com.example.var.data.model.MatchModel;
import com.example.var.data.model.StandingLeagueResponse;
import com.example.var.data.model.StandingModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentMatchLineupBinding;
import com.example.var.ui.adapter.MatchH2HAdapter;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MatchLineupFragment — Kadro, form karşılaştırması ve son karşılaşmalar sekmesi.
 *
 * Üç bölümden oluşur (varsa gösterilir):
 *  1. Form Karşılaştırması — takımların birbirine karşı W/D/L istatistikleri
 *  2. Son Karşılaşmalar    — H2H geçmiş maçlar
 *  3. İlk 11              — kadro (açıklandığında)
 */
public class MatchLineupFragment extends Fragment {

    private static final String TAG = "MatchLineup";

    private FragmentMatchLineupBinding binding;
    private MatchRepository repository;
    private String matchId;
    private String homeName;
    private String awayName;
    private String leagueId;
    private String homeId;
    private String awayId;

    private MatchH2HAdapter h2hAdapter;

    public static MatchLineupFragment newInstance(String matchId) {
        MatchLineupFragment fragment = new MatchLineupFragment();
        Bundle args = new Bundle();
        args.putString("match_id", matchId);
        fragment.setArguments(args);
        return fragment;
    }

    /** Factory metodu — maç verisiyle oluşturur (takım adları için). */
    public static MatchLineupFragment newInstance(MatchModel match) {
        MatchLineupFragment fragment = new MatchLineupFragment();
        Bundle args = new Bundle();
        args.putString("match_id",   match.getMatchId());
        args.putString("home_name",  match.getHomeName());
        args.putString("away_name",  match.getAwayName());
        args.putString("league_id",  match.getLeagueId());
        args.putString("home_id",    match.getHomeId());
        args.putString("away_id",    match.getAwayId());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            matchId  = getArguments().getString("match_id");
            homeName = getArguments().getString("home_name", "Ev Sahibi");
            awayName = getArguments().getString("away_name", "Deplasman");
            leagueId = getArguments().getString("league_id");
            homeId   = getArguments().getString("home_id");
            awayId   = getArguments().getString("away_id");
        }
        repository = new MatchRepository(BuildConfig.API_KEY);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMatchLineupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvLineup.setLayoutManager(new LinearLayoutManager(getContext()));

        h2hAdapter = new MatchH2HAdapter();
        binding.rvH2hInLineup.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvH2hInLineup.setAdapter(h2hAdapter);

        loadAnalysis();
        loadRecentForm();
        loadLineup();
    }

    // ── Analysis (Form + H2H) ──────────────────────────────────────────

    private void loadAnalysis() {
        if (matchId == null) return;

        repository.getAnalysis(matchId).enqueue(new Callback<AnalysisResponse>() {
            @Override
            public void onResponse(@NonNull Call<AnalysisResponse> call,
                    @NonNull Response<AnalysisResponse> response) {
                if (!isAdded() || binding == null) return;
                if (!response.isSuccessful() || response.body() == null) return;

                JsonElement dataEl = response.body().getData();
                if (dataEl == null) return;

                try {
                    JsonElement actual = null;
                    if (dataEl.isJsonArray() && dataEl.getAsJsonArray().size() > 0) {
                        actual = dataEl.getAsJsonArray().get(0);
                    } else if (dataEl.isJsonObject()) {
                        actual = dataEl;
                    }
                    if (actual == null || !actual.isJsonObject()) return;

                    // ── Form Karşılaştırması ──
                    bindFormData(actual);

                    // ── H2H Maçlar ──
                    bindH2HData(actual);

                } catch (Exception e) {
                    Log.e(TAG, "Analysis parse error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<AnalysisResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Analysis failure: " + t.getMessage());
            }
        });
    }

    private void bindFormData(JsonElement actual) {
        JsonElement homeVsEl = actual.getAsJsonObject().get("homeDataVs");
        JsonElement awayVsEl = actual.getAsJsonObject().get("awayDataVs");

        if (homeVsEl == null || awayVsEl == null) return;
        if (!homeVsEl.isJsonObject() || !awayVsEl.isJsonObject()) return;

        int hWin   = jsonInt(homeVsEl, "win");
        int hDraw  = jsonInt(homeVsEl, "draw");
        int hLose  = jsonInt(homeVsEl, "lose");
        int hGoals = jsonInt(homeVsEl, "scored");
        int hConc  = jsonInt(homeVsEl, "conceded");

        int aWin   = jsonInt(awayVsEl, "win");
        int aDraw  = jsonInt(awayVsEl, "draw");
        int aLose  = jsonInt(awayVsEl, "lose");
        int aGoals = jsonInt(awayVsEl, "scored");
        int aConc  = jsonInt(awayVsEl, "conceded");

        binding.tvHomeFormName.setText(homeName);
        binding.tvAwayFormName.setText(awayName);

        binding.tvHomeWin.setText(String.valueOf(hWin));
        binding.tvHomeDraw.setText(String.valueOf(hDraw));
        binding.tvHomeLose.setText(String.valueOf(hLose));
        binding.tvHomeGoals.setText(hGoals + "/" + hConc);

        binding.tvAwayWin.setText(String.valueOf(aWin));
        binding.tvAwayDraw.setText(String.valueOf(aDraw));
        binding.tvAwayLose.setText(String.valueOf(aLose));
        binding.tvAwayGoals.setText(aGoals + "/" + aConc);

        binding.cardForm.setVisibility(View.VISIBLE);
    }

    private void bindH2HData(JsonElement actual) {
        JsonElement h2hEl = actual.getAsJsonObject().get("headToHead");
        if (h2hEl == null || !h2hEl.isJsonArray()) return;

        List<String> h2hList = new ArrayList<>();
        for (JsonElement item : h2hEl.getAsJsonArray()) {
            if (item.isJsonPrimitive()) h2hList.add(item.getAsString());
        }

        if (!h2hList.isEmpty()) {
            h2hAdapter.setH2HData(h2hList);
            binding.cardH2H.setVisibility(View.VISIBLE);
        }
    }

    // ── Son Form (Standings API'sinden) ───────────────────────────────

    private void loadRecentForm() {
        if (leagueId == null) return;

        repository.getLeagueTable(leagueId).enqueue(new Callback<StandingLeagueResponse>() {
            @Override
            public void onResponse(@NonNull Call<StandingLeagueResponse> call,
                    @NonNull Response<StandingLeagueResponse> response) {
                if (!isAdded() || binding == null) return;
                if (!response.isSuccessful() || response.body() == null
                        || !response.body().isSuccess()
                        || response.body().getData() == null) return;

                java.util.List<StandingModel> standings =
                        response.body().getData().getTotalStandings();
                if (standings == null) return;

                StandingModel homeSt = null, awaySt = null;
                for (StandingModel s : standings) {
                    if (homeId != null && homeId.equals(s.getTeamId())) homeSt = s;
                    if (awayId != null && awayId.equals(s.getTeamId())) awaySt = s;
                }

                boolean found = homeSt != null || awaySt != null;
                if (!found) return;

                binding.tvHomeRecentName.setText(homeName);
                binding.tvAwayRecentName.setText(awayName);

                if (homeSt != null) {
                    binding.tvHomeRecentForm.setText(buildFormSpan(homeSt.getRecentForm()));
                }
                if (awaySt != null) {
                    binding.tvAwayRecentForm.setText(buildFormSpan(awaySt.getRecentForm()));
                }
                binding.cardRecentForm.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(@NonNull Call<StandingLeagueResponse> call,
                    @NonNull Throwable t) { /* sessiz */ }
        });
    }

    /** 0=G(yeşil), 1=B(gri), 2=M(kırmızı), 3=boş(-) olarak renkli span döner. */
    private SpannableStringBuilder buildFormSpan(int[] form) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int i = 0; i < form.length; i++) {
            if (i > 0) sb.append("  ");
            String letter;
            int color;
            switch (form[i]) {
                case 0: letter = "G"; color = 0xFF4CAF50; break;  // yeşil
                case 1: letter = "B"; color = 0xFF9E9E9E; break;  // gri
                case 2: letter = "M"; color = 0xFFF44336; break;  // kırmızı
                default: letter = "·"; color = 0xFF9E9E9E; break;
            }
            int start = sb.length();
            sb.append(letter);
            sb.setSpan(new ForegroundColorSpan(color), start, sb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    // ── Kadro ─────────────────────────────────────────────────────────

    private void loadLineup() {
        if (matchId == null) return;

        repository.getLineup(matchId).enqueue(new Callback<ApiResponse<LineupModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LineupModel>> call,
                    @NonNull Response<ApiResponse<LineupModel>> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    LineupModel lineup = response.body().getData() != null
                            && !response.body().getData().isEmpty()
                            ? response.body().getData().get(0) : null;
                    if (lineup != null) {
                        bindLineup(lineup);
                        binding.tvEmptyLineup.setVisibility(View.GONE);
                        binding.cardLineup.setVisibility(View.VISIBLE);
                        binding.llFormation.setVisibility(View.VISIBLE);
                        return;
                    }
                }
                // Kadro yoksa boş mesaj yalnızca form/h2h da yoksa göster
                if (binding.cardForm.getVisibility() != View.VISIBLE
                        && binding.cardH2H.getVisibility() != View.VISIBLE) {
                    binding.tvEmptyLineup.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LineupModel>> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                if (binding.cardForm.getVisibility() != View.VISIBLE
                        && binding.cardH2H.getVisibility() != View.VISIBLE) {
                    binding.tvEmptyLineup.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void bindLineup(LineupModel lineup) {
        binding.tvHomeFormation.setText(lineup.getHomeFormation());
        binding.tvAwayFormation.setText(lineup.getAwayFormation());
    }

    // ── Yardımcı ──────────────────────────────────────────────────────

    private int jsonInt(JsonElement parent, String key) {
        try {
            JsonElement el = parent.getAsJsonObject().get(key);
            return (el != null && el.isJsonPrimitive()) ? el.getAsInt() : 0;
        } catch (Exception e) { return 0; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
