package com.example.var.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bumptech.glide.Glide;
import com.example.var.BuildConfig;
import com.example.var.R;
import com.example.var.databinding.FragmentMatchDetailBinding;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.MatchModel;
import com.example.var.data.model.StandingLeagueResponse;
import com.example.var.data.repository.MatchRepository;
import com.example.var.ui.dialog.AIPredictionDialogFragment;
import com.example.var.util.DateUtils;
import com.example.var.util.FirebaseManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MatchDetailFragment - Maç detay ekranı.
 *
 * Üstte skor ve takımlar (CollapsingToolbar + AppBar), altta
 * ViewPager2 ile 5 sekme: Özet, Kadro, İstatistik, H2H, Oranlar.
 *
 * Giriş yapmış kullanıcılar için sağ altta AI tahmin FAB'ı görünür;
 * tıklandığında AIPredictionDialogFragment açılır ve Groq API'ye istek atılır.
 */
public class MatchDetailFragment extends Fragment {

    /** View binding referansı — onDestroyView'da null'lanır */
    private FragmentMatchDetailBinding binding;

    /** Maç verisi (arguments'tan deserialize edilir) */
    private MatchModel match;

    /** API çağrıları için repository */
    private MatchRepository repository;

    /**
     * Fragment'ı MatchModel ile oluşturmak için factory metodu.
     * MatchModel Serializable olmalıdır.
     */
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
            match = getArguments().getSerializable("match_data", MatchModel.class);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMatchDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new MatchRepository(BuildConfig.API_KEY);

        if (match != null) {
            bindMatchData();
            fetchDetailedInfo();
            loadTeamLogos();
        }

        setupToolbar();
        setupViewPager();
        setupAiFab();
    }

    /**
     * Ev sahibi ve deplasman takımlarının logolarını standing/league API'sinden
     * çeker.
     */
    private void loadTeamLogos() {
        if (match.getLeagueId() == null)
            return;

        repository.getLeagueTable(match.getLeagueId()).enqueue(new Callback<StandingLeagueResponse>() {
            @Override
            public void onResponse(@NonNull Call<StandingLeagueResponse> call,
                    @NonNull Response<StandingLeagueResponse> response) {
                if (!isAdded() || binding == null)
                    return;
                if (!response.isSuccessful() || response.body() == null
                        || !response.body().isSuccess()
                        || response.body().getData() == null)
                    return;

                List<StandingLeagueResponse.TeamInfo> teamInfos = response.body().getData().getTeamInfos();
                if (teamInfos == null)
                    return;

                for (StandingLeagueResponse.TeamInfo ti : teamInfos) {
                    if (ti.getTeamId() == null || ti.getLogo() == null || ti.getLogo().isEmpty())
                        continue;
                    if (ti.getTeamId().equals(match.getHomeId())) {
                        Glide.with(MatchDetailFragment.this)
                                .load(ti.getLogo())
                                .placeholder(R.drawable.ic_launcher_background)
                                .into(binding.ivHomeLogo);
                    }
                    if (ti.getTeamId().equals(match.getAwayId())) {
                        Glide.with(MatchDetailFragment.this)
                                .load(ti.getLogo())
                                .placeholder(R.drawable.ic_launcher_background)
                                .into(binding.ivAwayLogo);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<StandingLeagueResponse> call, @NonNull Throwable t) {
                // Sessiz hata — logo olmadan devam et
            }
        });
    }

    /**
     * API'den maç detay bilgisini çeker ve ekranı günceller.
     * Başarısız olursa sessizce devam eder (ilk veri zaten gösterilmekte).
     */
    private void fetchDetailedInfo() {
        if (match.getMatchId() == null)
            return;

        repository.getMatchDetail(match.getMatchId()).enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Response<ApiResponse<MatchModel>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null
                        && !response.body().getData().isEmpty()) {
                    match = response.body().getData().get(0);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            bindMatchData();
                            setupAiFab();
                        });
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MatchModel>> call, @NonNull Throwable t) {
                // Sessiz hata — ilk veriler ekranda kalmaya devam eder
            }
        });
    }

    /**
     * Maç verilerini üst header alanına bağlar:
     * takım adları, skor, durum ve maç saati.
     */
    private void bindMatchData() {
        binding.tvHomeName.setText(match.getHomeName());
        binding.tvAwayName.setText(match.getAwayName());

        if (match.isNotStarted()) {
            binding.tvScore.setText("v");
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(match.getMatchTime() * 1000L);
            binding.tvMatchTime.setText(DateUtils.formatMatchTime(match.getMatchTime()));
            binding.tvStatus.setText(getString(R.string.status_not_started));
        } else {
            binding.tvScore.setText(match.getHomeScore() + " - " + match.getAwayScore());
            binding.tvMatchTime.setText(match.isLive() ? match.getInjuryTime() + "'" : "");
            binding.tvStatus.setText(match.isLive()
                    ? getString(R.string.status_live)
                    : getString(R.string.status_finished));
        }
    }

    /** Toolbar geri tuşunu FragmentManager back stack'iyle bağlar. */
    private void setupToolbar() {
        binding.toolbar
                .setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    /**
     * ViewPager2'yi sekme adapter'ıyla ve TabLayout mediator'ıyla yapılandırır.
     * Sekme metinleri strings.xml'den çekilmelidir (şimdilik sabit metinler).
     */
    private void setupViewPager() {
        MatchDetailPagerAdapter adapter = new MatchDetailPagerAdapter(this, match);
        binding.viewPager.setAdapter(adapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.match_details);
                    break;
                case 1:
                    tab.setText(R.string.statistics);
                    break;
                case 2:
                    tab.setText(R.string.comparison);
                    break;
                case 3:
                    tab.setText(R.string.lineup);
                    break;

            }
        }).attach();
    }

    /**
     * AI tahmin FAB'ını yapılandırır.
     *
     * - Giriş yapmış kullanıcı → FAB görünür, tıklanınca AIPredictionDialogFragment
     * açılır
     * - Giriş yapılmamış → FAB gizli; tıklanırsa Snackbar gösterir
     *
     * Not: FAB yalnızca giriş yapılmış kullanıcılara sunulur; giriş yapılmamışsa
     * tıklama olayı da bağlanmaz (FAB zaten GONE).
     */
    private void setupAiFab() {
        if (match != null && match.isFinished()) {
            binding.fabAiPrediction.setVisibility(View.GONE);
            return;
        }
        if (FirebaseManager.isLoggedIn()) {
            binding.fabAiPrediction.setVisibility(View.VISIBLE);
            binding.fabAiPrediction.setOnClickListener(v -> openAiPrediction());
        } else {
            binding.fabAiPrediction.setVisibility(View.GONE);
        }
    }

    /**
     * AIPredictionDialogFragment'ı açar.
     * Mevcut maç verisi dialog'a iletilir ve Groq API analizi başlatılır.
     */
    private void openAiPrediction() {
        if (match == null)
            return;
        AIPredictionDialogFragment dialog = AIPredictionDialogFragment.newInstance(match);
        dialog.show(getChildFragmentManager(), "ai_prediction");
    }

    // =========================================================
    // ViewPager2 Adapter (iç sınıf)
    // =========================================================

    /**
     * Maç detayı için 5 sekmeyi yöneten ViewPager2 adapter'ı.
     * Her pozisyon ilgili Fragment'ı oluşturur.
     */
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
                case 0:
                    return MatchSummaryFragment.newInstance(match);
                case 1:
                    return MatchStatsFragment.newInstance(match);
                case 2:
                    return MatchH2HFragment.newInstance(match);
                default:
                    return KadroFragment.newInstance(match);
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
