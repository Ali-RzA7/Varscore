package com.example.var.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.var.databinding.FragmentMatchDetailBinding;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.MatchModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.ui.fragment.MatchStatsFragment;
import com.example.var.ui.fragment.MatchH2HFragment;
import com.example.var.util.DateUtils;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MatchDetailFragment - Maç detay ekranı.
 * Üstte skor ve takımlar, altta sekmeler bulunur.
 */
public class MatchDetailFragment extends Fragment {

    private FragmentMatchDetailBinding binding;
    private MatchModel match;

    public static MatchDetailFragment newInstance(MatchModel match) {
        MatchDetailFragment fragment = new MatchDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("match_data", match);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            match = (MatchModel) getArguments().getSerializable("match_data");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMatchDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private MatchRepository repository;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        repository = new MatchRepository("BuildConfig.API_KEY");

        if (match != null) {
            bindMatchData();
            fetchDetailedInfo();
        }
        
        setupToolbar();
        setupViewPager();
    }

    private void fetchDetailedInfo() {
        if (match.getMatchId() == null) return;
        
        repository.getMatchDetail(match.getMatchId()).enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<MatchModel>> call, Response<ApiResponse<MatchModel>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null && !response.body().getData().isEmpty()) {
                    match = response.body().getData().get(0);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> bindMatchData());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MatchModel>> call, Throwable t) {
            }
        });
    }

    private void bindMatchData() {
        binding.tvHomeName.setText(match.getHomeName());
        binding.tvAwayName.setText(match.getAwayName());
        
        if (match.isNotStarted()) {
            binding.tvScore.setText("v");
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(match.getMatchTime() * 1000);
            binding.tvMatchTime.setText(DateUtils.formatMatchTime(match.getMatchTime()));
            binding.tvStatus.setText("Yakında");
        } else {
            binding.tvScore.setText(match.getHomeScore() + " - " + match.getAwayScore());
            binding.tvMatchTime.setText(match.isLive() ? match.getInjuryTime() + "'" : "");
            binding.tvStatus.setText(match.isLive() ? "CANLI" : "MS");
        }

        // Logo yükleme işlemi (Glide vb. ile yapılmalı)
        // binding.ivHomeLogo.load(match.getHomeId());
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void setupViewPager() {
        MatchDetailPagerAdapter adapter = new MatchDetailPagerAdapter(this, match);
        binding.viewPager.setAdapter(adapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Detay"); break;
                case 1: tab.setText("Kadro"); break;
                case 2: tab.setText("İstatistik"); break;
                case 3: tab.setText("Eşleşmeler"); break;
                case 4: tab.setText("Oranlar"); break;
            }
        }).attach();
    }

    private static class MatchDetailPagerAdapter extends FragmentStateAdapter {
        private final MatchModel match;

        public MatchDetailPagerAdapter(@NonNull Fragment fragment, MatchModel match) {
            super(fragment);
            this.match = match;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return MatchSummaryFragment.newInstance(match.getMatchId());
                case 1: return MatchLineupFragment.newInstance(match.getMatchId());
                case 2: return MatchStatsFragment.newInstance(match);
                case 3: return MatchH2HFragment.newInstance(match.getMatchId());
                default: return new Fragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
