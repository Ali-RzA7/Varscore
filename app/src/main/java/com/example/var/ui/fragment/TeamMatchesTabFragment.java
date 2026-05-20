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
import com.example.var.R;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.MatchModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentTeamMatchesTabBinding;
import com.example.var.ui.adapter.MatchAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * TeamMatchesTabFragment - Takım maçlarını gösteren tab fragment'ı.
 * TeamMatchesFragment içinde ViewPager2'nin birinci tab'ında kullanılır.
 */
public class TeamMatchesTabFragment extends Fragment
        implements MatchAdapter.OnMatchClickListener {

    private static final String ARG_TEAM_ID = "team_id";
    private static final String ARG_LEAGUE_ID = "league_id";

    private FragmentTeamMatchesTabBinding binding;
    private MatchAdapter matchAdapter;
    private MatchRepository repository;
    private String teamId;
    private String leagueId;

    /**
     * Fragment oluşturma fabrika metodu.
     *
     * @param teamId   Maçları gösterilecek takımın ID'si
     * @param leagueId Lig ID'si (boş olabilir)
     * @return Argümanları yüklenmiş TeamMatchesTabFragment örneği
     */
    public static TeamMatchesTabFragment newInstance(String teamId, String leagueId) {
        TeamMatchesTabFragment fragment = new TeamMatchesTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEAM_ID, teamId);
        args.putString(ARG_LEAGUE_ID, leagueId != null ? leagueId : "");
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            teamId = getArguments().getString(ARG_TEAM_ID, "");
            leagueId = getArguments().getString(ARG_LEAGUE_ID, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentTeamMatchesTabBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new MatchRepository(BuildConfig.API_KEY);

        matchAdapter = new MatchAdapter(requireContext(), this);
        if (teamId != null && !teamId.isEmpty()) {
            matchAdapter.setCurrentTeamId(teamId);
        }
        binding.rvMatches.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMatches.setAdapter(matchAdapter);

        loadMatches();
    }

    /**
     * Takım maçlarını yükler.
     * leagueId varsa lig maçlarını çekip teamId ile filtreler;
     * yoksa doğrudan teamId ile maçları çeker.
     */
    private void loadMatches() {
        showLoading();

        if (leagueId != null && !leagueId.isEmpty()) {
            loadLeagueMatchesFiltered();
        } else if (teamId != null && !teamId.isEmpty()) {
            loadTeamMatchesDirect();
        } else {
            showEmpty();
        }
    }

    private void loadLeagueMatchesFiltered() {
        repository.getLeagueMatches(leagueId).enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Response<ApiResponse<MatchModel>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<MatchModel> all = response.body().getData();
                    List<MatchModel> teamMatches = new ArrayList<>();
                    if (all != null) {
                        for (MatchModel m : all) {
                            if (teamId.equals(m.getHomeId()) || teamId.equals(m.getAwayId())) {
                                teamMatches.add(m);
                            }
                        }
                    }
                    if (!teamMatches.isEmpty()) {
                        matchAdapter.setMatches(teamMatches);
                        showContent();
                    } else {
                        showEmpty();
                    }
                } else {
                    showEmpty();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Throwable t) {
                if (!isAdded()) return;
                showEmpty();
            }
        });
    }

    private void loadTeamMatchesDirect() {
        repository.getTeamMatches(teamId).enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Response<ApiResponse<MatchModel>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<MatchModel> matches = response.body().getData();
                    if (matches != null && !matches.isEmpty()) {
                        matchAdapter.setMatches(matches);
                        showContent();
                    } else {
                        showEmpty();
                    }
                } else {
                    showEmpty();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Throwable t) {
                if (!isAdded()) return;
                showEmpty();
            }
        });
    }

    @Override
    public void onMatchClick(MatchModel match) {
        MatchDetailFragment detailFragment = MatchDetailFragment.newInstance(match);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                     R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    private void showLoading() {
        binding.loadingContainer.setVisibility(View.VISIBLE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.rvMatches.setVisibility(View.GONE);
    }

    private void showContent() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.rvMatches.setVisibility(View.VISIBLE);
    }

    private void showEmpty() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.VISIBLE);
        binding.rvMatches.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
