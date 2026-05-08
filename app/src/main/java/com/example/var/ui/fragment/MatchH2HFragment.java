package com.example.var.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.var.BuildConfig;
import com.example.var.data.model.AnalysisResponse;
import com.example.var.data.model.MatchModel;
import com.example.var.data.model.StandingLeagueResponse;
import com.example.var.data.model.StandingModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentMatchH2hBinding;
import com.example.var.ui.adapter.MatchH2HAdapter;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchH2HFragment extends Fragment {

    private FragmentMatchH2hBinding binding;
    private MatchRepository repository;

    private String matchId;
    private String homeName;
    private String awayName;
    private String leagueId;
    private String homeId;
    private String awayId;

    private MatchH2HAdapter adapter;

    public static MatchH2HFragment newInstance(String matchId) {
        MatchH2HFragment f = new MatchH2HFragment();
        Bundle args = new Bundle();
        args.putString("match_id", matchId);
        f.setArguments(args);
        return f;
    }

    public static MatchH2HFragment newInstance(MatchModel match) {
        MatchH2HFragment f = new MatchH2HFragment();
        Bundle args = new Bundle();
        args.putString("match_id",  match.getMatchId());
        args.putString("home_name", match.getHomeName());
        args.putString("away_name", match.getAwayName());
        args.putString("league_id", match.getLeagueId());
        args.putString("home_id",   match.getHomeId());
        args.putString("away_id",   match.getAwayId());
        f.setArguments(args);
        return f;
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
        binding = FragmentMatchH2hBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new MatchH2HAdapter();
        binding.rvH2h.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvH2h.setAdapter(adapter);

        loadRecentForm();
        loadH2HData();
    }

    // ── Son Form ─────────────────────────────────────────────────────────

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

                List<StandingModel> standings = response.body().getData().getTotalStandings();
                if (standings == null) return;

                StandingModel homeSt = null, awaySt = null;
                for (StandingModel s : standings) {
                    if (homeId != null && homeId.equals(s.getTeamId())) homeSt = s;
                    if (awayId != null && awayId.equals(s.getTeamId())) awaySt = s;
                }
                if (homeSt == null && awaySt == null) return;

                binding.tvHomeFormName.setText(homeName);
                binding.tvAwayFormName.setText(awayName);
                if (homeSt != null)
                    binding.tvHomeRecentForm.setText(buildFormSpan(homeSt.getRecentForm()));
                if (awaySt != null)
                    binding.tvAwayRecentForm.setText(buildFormSpan(awaySt.getRecentForm()));

                binding.cardRecentForm.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(@NonNull Call<StandingLeagueResponse> call, @NonNull Throwable t) { }
        });
    }

    private SpannableStringBuilder buildFormSpan(int[] form) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int i = 0; i < form.length; i++) {
            if (i > 0) sb.append("  ");
            String letter;
            int color;
            switch (form[i]) {
                case 0: letter = "G"; color = 0xFF4CAF50; break;
                case 1: letter = "B"; color = 0xFF9E9E9E; break;
                case 2: letter = "M"; color = 0xFFF44336; break;
                default: letter = "·"; color = 0xFF9E9E9E; break;
            }
            int start = sb.length();
            sb.append(letter);
            sb.setSpan(new ForegroundColorSpan(color), start, sb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    // ── H2H Maçlar ───────────────────────────────────────────────────────

    private void loadH2HData() {
        if (matchId == null) return;

        repository.getAnalysis(matchId).enqueue(new Callback<AnalysisResponse>() {
            @Override
            public void onResponse(@NonNull Call<AnalysisResponse> call,
                    @NonNull Response<AnalysisResponse> response) {
                if (!isAdded() || binding == null) return;
                if (!response.isSuccessful() || response.body() == null) {
                    showEmptyH2H();
                    return;
                }

                JsonElement dataEl = response.body().getData();
                if (dataEl == null) { showEmptyH2H(); return; }

                try {
                    JsonElement actual = null;
                    if (dataEl.isJsonArray() && dataEl.getAsJsonArray().size() > 0) {
                        actual = dataEl.getAsJsonArray().get(0);
                    } else if (dataEl.isJsonObject()) {
                        actual = dataEl;
                    }
                    if (actual == null || !actual.isJsonObject()) { showEmptyH2H(); return; }

                    JsonElement h2hEl = actual.getAsJsonObject().get("headToHead");
                    if (h2hEl == null || !h2hEl.isJsonArray()) { showEmptyH2H(); return; }

                    List<String> h2hList = new ArrayList<>();
                    for (JsonElement item : h2hEl.getAsJsonArray()) {
                        if (item.isJsonPrimitive()) h2hList.add(item.getAsString());
                    }

                    if (!h2hList.isEmpty()) {
                        adapter.setH2HData(h2hList);
                        binding.tvEmptyH2h.setVisibility(View.GONE);
                        binding.rvH2h.setVisibility(View.VISIBLE);
                    } else {
                        showEmptyH2H();
                    }
                } catch (Exception e) {
                    showEmptyH2H();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AnalysisResponse> call, @NonNull Throwable t) {
                if (isAdded() && binding != null) showEmptyH2H();
            }
        });
    }

    private void showEmptyH2H() {
        binding.tvEmptyH2h.setVisibility(View.VISIBLE);
        binding.rvH2h.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
