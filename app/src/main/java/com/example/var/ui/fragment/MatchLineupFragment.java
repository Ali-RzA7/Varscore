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
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentMatchLineupBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MatchLineupFragment - Maç kadroları sekmesi.
 */
public class MatchLineupFragment extends Fragment {

    private FragmentMatchLineupBinding binding;
    private MatchRepository repository;
    private String matchId;

    public static MatchLineupFragment newInstance(String matchId) {
        MatchLineupFragment fragment = new MatchLineupFragment();
        Bundle args = new Bundle();
        args.putString("match_id", matchId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            matchId = getArguments().getString("match_id");
        }
        repository = new MatchRepository(BuildConfig.API_KEY);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMatchLineupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.rvLineup.setLayoutManager(new LinearLayoutManager(getContext()));
        loadLineup();
    }

    private void loadLineup() {
        if (matchId == null) return;

        repository.getLineup(matchId).enqueue(new Callback<ApiResponse<LineupModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<LineupModel>> call, Response<ApiResponse<LineupModel>> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    LineupModel lineup = response.body().getData() != null && !response.body().getData().isEmpty()
                            ? response.body().getData().get(0) : null;
                    if (lineup != null) {
                        bindLineup(lineup);
                        binding.tvEmptyLineup.setVisibility(View.GONE);
                        binding.cardLineup.setVisibility(View.VISIBLE);
                        binding.llFormation.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvEmptyLineup.setVisibility(View.VISIBLE);
                        binding.cardLineup.setVisibility(View.GONE);
                        binding.llFormation.setVisibility(View.GONE);
                    }
                } else {
                    binding.tvEmptyLineup.setVisibility(View.VISIBLE);
                    binding.cardLineup.setVisibility(View.GONE);
                    binding.llFormation.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LineupModel>> call, Throwable t) {
                if (!isAdded() || binding == null) return;
                binding.tvEmptyLineup.setVisibility(View.VISIBLE);
                binding.cardLineup.setVisibility(View.GONE);
                binding.llFormation.setVisibility(View.GONE);
            }
        });
    }

    private void bindLineup(LineupModel lineup) {
        binding.tvHomeFormation.setText(lineup.getHomeFormation());
        binding.tvAwayFormation.setText(lineup.getAwayFormation());
        // Adapter logic here
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
