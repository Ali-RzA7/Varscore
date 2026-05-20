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

import com.example.var.BuildConfig;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.LineupModel;
import com.example.var.data.model.MatchModel;
import com.example.var.data.model.PlayerModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentTeamSquadBinding;
import com.example.var.ui.adapter.PlayerAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeamSquadFragment extends Fragment {

    private static final String TAG = "TeamSquadFragment";
    private static final String ARG_TEAM_ID = "team_id";

    private FragmentTeamSquadBinding binding;
    private PlayerAdapter playerAdapter;
    private MatchRepository repository;
    private String teamId;

    public static TeamSquadFragment newInstance(String teamId) {
        TeamSquadFragment fragment = new TeamSquadFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEAM_ID, teamId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            teamId = getArguments().getString(ARG_TEAM_ID, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentTeamSquadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new MatchRepository(BuildConfig.API_KEY);

        playerAdapter = new PlayerAdapter();
        binding.rvPlayers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPlayers.setAdapter(playerAdapter);

        loadPlayers();
    }

    // ===== Adım 1: /player?teamId=X =====

    private void loadPlayers() {
        showLoading();
        if (teamId == null || teamId.isEmpty()) { showEmpty(); return; }

        Log.d(TAG, "Step1: getTeamPlayers teamId=" + teamId);
        repository.getTeamPlayers(teamId).enqueue(new Callback<ApiResponse<PlayerModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PlayerModel>> call,
                    @NonNull Response<ApiResponse<PlayerModel>> response) {
                if (!isAdded()) return;
                Log.d(TAG, "Step1 HTTP=" + response.code()
                        + " body=" + (response.body() != null ? "non-null" : "null"));
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Step1 apiCode=" + response.body().getCode()
                            + " msg=" + response.body().getMessage());
                    List<PlayerModel> players = response.body().getData();
                    Log.d(TAG, "Step1 players=" + (players != null ? players.size() : "null"));
                    if (players != null && !players.isEmpty()) {
                        playerAdapter.setPlayers(players);
                        showContent();
                        return;
                    }
                }
                Log.d(TAG, "Step1 no data, falling back to lineup");
                loadSquadFromRecentMatch();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PlayerModel>> call,
                    @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Step1 failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
                loadSquadFromRecentMatch();
            }
        });
    }

    // ===== Adım 2: Son maçı bul =====

    private void loadSquadFromRecentMatch() {
        Log.d(TAG, "Step2: getTeamMatches teamId=" + teamId);
        repository.getTeamMatches(teamId).enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Response<ApiResponse<MatchModel>> response) {
                if (!isAdded()) return;
                Log.d(TAG, "Step2 HTTP=" + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    List<MatchModel> matches = response.body().getData();
                    if (matches != null && !matches.isEmpty()) {
                        MatchModel recent = null;
                        for (MatchModel m : matches) {
                            if (m.isFinished()) {
                                if (recent == null || m.getMatchTime() > recent.getMatchTime()) {
                                    recent = m;
                                }
                            }
                        }
                        if (recent != null) {
                            Log.d(TAG, "Step2 found recent match=" + recent.getMatchId());
                            loadLineupForMatch(recent);
                            return;
                        }
                    }
                }
                Log.d(TAG, "Step2 no recent match found");
                showEmpty();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Step2 failed: " + t.getMessage());
                showEmpty();
            }
        });
    }

    // ===== Adım 3: Lineup'tan oyuncuları çek =====

    private void loadLineupForMatch(MatchModel match) {
        final boolean isHome = teamId.equals(match.getHomeId());
        Log.d(TAG, "Step3: getLineups matchId=" + match.getMatchId() + " isHome=" + isHome);
        repository.getLineups(match.getMatchId()).enqueue(new Callback<ApiResponse<LineupModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LineupModel>> call,
                    @NonNull Response<ApiResponse<LineupModel>> response) {
                if (!isAdded()) return;
                Log.d(TAG, "Step3 HTTP=" + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    List<LineupModel> lineups = response.body().getData();
                    if (lineups != null && !lineups.isEmpty()) {
                        LineupModel lineup = lineups.get(0);
                        List<LineupModel.PlayerModel> starters =
                                isHome ? lineup.getHomeLineup() : lineup.getAwayLineup();
                        List<LineupModel.PlayerModel> subs =
                                isHome ? lineup.getHomeBackup() : lineup.getAwayBackup();

                        List<PlayerModel> players = new ArrayList<>();
                        if (starters != null) {
                            for (LineupModel.PlayerModel p : starters) {
                                players.add(toPlayerModel(p));
                            }
                        }
                        if (subs != null) {
                            for (LineupModel.PlayerModel p : subs) {
                                players.add(toPlayerModel(p));
                            }
                        }
                        Log.d(TAG, "Step3 lineup players=" + players.size());
                        if (!players.isEmpty()) {
                            playerAdapter.setPlayers(players);
                            showContent();
                            return;
                        }
                    }
                }
                Log.d(TAG, "Step3 no lineup data");
                showEmpty();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LineupModel>> call,
                    @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Step3 failed: " + t.getMessage());
                showEmpty();
            }
        });
    }

    private PlayerModel toPlayerModel(LineupModel.PlayerModel p) {
        PlayerModel pm = new PlayerModel();
        pm.setPlayerId(p.getPlayerId());
        pm.setName(p.getPlayerName());
        pm.setNumber(p.getNumber());
        pm.setPosition(p.getPosition());
        return pm;
    }

    // ===== UI durumları =====

    private void showLoading() {
        binding.loadingContainer.setVisibility(View.VISIBLE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.rvPlayers.setVisibility(View.GONE);
    }

    private void showContent() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.rvPlayers.setVisibility(View.VISIBLE);
    }

    private void showEmpty() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.VISIBLE);
        binding.rvPlayers.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
