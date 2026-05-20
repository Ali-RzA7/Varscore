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
import com.example.var.data.model.PlayerModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentTeamSquadBinding;
import com.example.var.ui.adapter.PlayerAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * TeamSquadFragment - Takım kadrosunu gösteren tab fragment'ı.
 * TeamMatchesFragment içinde ViewPager2'nin ikinci tab'ında kullanılır.
 */
public class TeamSquadFragment extends Fragment {

    private static final String ARG_TEAM_ID = "team_id";

    private FragmentTeamSquadBinding binding;
    private PlayerAdapter playerAdapter;
    private MatchRepository repository;
    private String teamId;

    /**
     * Fragment oluşturma fabrika metodu.
     *
     * @param teamId Kadrosu gösterilecek takımın ID'si
     * @return Argümanları yüklenmiş TeamSquadFragment örneği
     */
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

    /**
     * Takım oyuncularını API'den yükler.
     */
    private void loadPlayers() {
        showLoading();

        if (teamId == null || teamId.isEmpty()) {
            showEmpty();
            return;
        }

        repository.getTeamPlayers(teamId).enqueue(new Callback<ApiResponse<PlayerModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PlayerModel>> call,
                    @NonNull Response<ApiResponse<PlayerModel>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<PlayerModel> players = response.body().getData();
                    if (players != null && !players.isEmpty()) {
                        playerAdapter.setPlayers(players);
                        showContent();
                    } else {
                        showEmpty();
                    }
                } else {
                    showEmpty();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PlayerModel>> call,
                    @NonNull Throwable t) {
                if (!isAdded()) return;
                showEmpty();
            }
        });
    }

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
