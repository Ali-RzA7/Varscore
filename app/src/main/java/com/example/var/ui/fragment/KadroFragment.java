package com.example.var.ui.fragment;

import android.os.Bundle;
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
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentKadroBinding;
import com.example.var.ui.adapter.LineupPlayerAdapter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KadroFragment extends Fragment {

    private FragmentKadroBinding binding;
    private MatchRepository repository;
    private String matchId;
    private String homeName;
    private String awayName;

    private LineupPlayerAdapter startingAdapter;
    private LineupPlayerAdapter backupAdapter;

    public static KadroFragment newInstance(String matchId) {
        KadroFragment f = new KadroFragment();
        Bundle args = new Bundle();
        args.putString("match_id", matchId);
        f.setArguments(args);
        return f;
    }

    public static KadroFragment newInstance(MatchModel match) {
        KadroFragment f = new KadroFragment();
        Bundle args = new Bundle();
        args.putString("match_id", match.getMatchId());
        args.putString("home_name", match.getHomeName());
        args.putString("away_name", match.getAwayName());
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
        }
        repository = new MatchRepository(BuildConfig.API_KEY);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentKadroBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        startingAdapter = new LineupPlayerAdapter();
        backupAdapter   = new LineupPlayerAdapter();

        binding.rvStarting.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvStarting.setAdapter(startingAdapter);

        binding.rvBackup.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvBackup.setAdapter(backupAdapter);

        loadLineup();
    }

    private void loadLineup() {
        if (matchId == null) {
            showEmpty();
            return;
        }

        binding.pbKadro.setVisibility(View.VISIBLE);

        repository.getLineups(matchId).enqueue(new Callback<ApiResponse<LineupModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LineupModel>> call,
                    @NonNull Response<ApiResponse<LineupModel>> response) {
                if (!isAdded() || binding == null) return;
                binding.pbKadro.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null
                        || response.body().getData() == null
                        || response.body().getData().isEmpty()) {
                    showEmpty();
                    return;
                }

                LineupModel lineup = response.body().getData().get(0);
                bindLineup(lineup);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LineupModel>> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                binding.pbKadro.setVisibility(View.GONE);
                showEmpty();
            }
        });
    }

    private void bindLineup(LineupModel lineup) {
        // Takım adları başlık kartı
        binding.tvHomeTeamName.setText(homeName);
        binding.tvAwayTeamName.setText(awayName);
        binding.cardFormation.setVisibility(View.VISIBLE);

        // İlk 11
        boolean hasStarting = (lineup.getHomeLineup() != null && !lineup.getHomeLineup().isEmpty())
                || (lineup.getAwayLineup() != null && !lineup.getAwayLineup().isEmpty());

        if (hasStarting) {
            startingAdapter.setPlayers(lineup.getHomeLineup(), lineup.getAwayLineup());
            binding.cardStarting.setVisibility(View.VISIBLE);
        }

        // Yedekler
        boolean hasBackup = (lineup.getHomeBackup() != null && !lineup.getHomeBackup().isEmpty())
                || (lineup.getAwayBackup() != null && !lineup.getAwayBackup().isEmpty());

        if (hasBackup) {
            backupAdapter.setPlayers(lineup.getHomeBackup(), lineup.getAwayBackup());
            binding.cardBackup.setVisibility(View.VISIBLE);
        }

        if (!hasStarting && !hasBackup) {
            showEmpty();
        }
    }

    private void showEmpty() {
        binding.tvEmptyKadro.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
