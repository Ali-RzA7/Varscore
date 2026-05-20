package com.example.var.ui.fragment;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bumptech.glide.Glide;
import com.example.var.BuildConfig;
import com.example.var.R;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.MatchModel;
import com.example.var.data.model.TeamProfileModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentTeamMatchesBinding;
import com.example.var.util.FirebaseManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * TeamMatchesFragment - Profesyonel takım detay sayfası.
 * Collapsing header (logo, ad, stadyum, antrenör, form) + ViewPager2 (Maçlar / Kadro).
 *
 * Açılma yolları:
 * 1. Arama sonucunda takıma tıklandığında
 * 2. Puan tablosunda takıma tıklandığında
 * 3. Favori takımlar listesinden
 */
public class TeamMatchesFragment extends Fragment {

    private static final String ARG_TEAM_ID    = "team_id";
    private static final String ARG_TEAM_NAME  = "team_name";
    private static final String ARG_LEAGUE_ID  = "league_id";
    private static final String ARG_LOGO_MAP   = "logo_map";

    private FragmentTeamMatchesBinding binding;
    private MatchRepository repository;

    private String teamId;
    private String teamName;
    private String leagueId;
    private boolean isFavorite = false;

    // ===== Fabrika Metodları =====

    public static TeamMatchesFragment newInstance(String teamId, String teamName, String leagueId) {
        return newInstance(teamId, teamName, leagueId, null);
    }

    public static TeamMatchesFragment newInstance(String teamId, String teamName, String leagueId,
            HashMap<String, String> logoMap) {
        TeamMatchesFragment fragment = new TeamMatchesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEAM_ID, teamId);
        args.putString(ARG_TEAM_NAME, teamName);
        args.putString(ARG_LEAGUE_ID, leagueId != null ? leagueId : "");
        if (logoMap != null) args.putSerializable(ARG_LOGO_MAP, logoMap);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            teamId   = getArguments().getString(ARG_TEAM_ID, "");
            teamName = getArguments().getString(ARG_TEAM_NAME, "");
            leagueId = getArguments().getString(ARG_LEAGUE_ID, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentTeamMatchesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new MatchRepository(BuildConfig.API_KEY);

        setupToolbar();
        setupViewPager();
        checkFavoriteStatus();
        loadTeamProfile();
        loadRecentForm();
    }

    // ===== Kurulum =====

    /**
     * Toolbar'ı yapılandırır: başlık, geri butonu, favori butonu.
     */
    private void setupToolbar() {
        // Collapsed durumda toolbar başlığı
        binding.collapsingToolbar.setTitle(teamName);
        binding.toolbar.setTitle(teamName);
        // Expanded durumda built-in başlığı gizle (kendi tvTeamName kullanıyoruz)
        binding.collapsingToolbar.setExpandedTitleColor(Color.TRANSPARENT);

        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );
        binding.btnFavorite.setOnClickListener(v -> toggleFavorite());

        // Hero alanındaki takım adını hemen yaz (profil yüklenene kadar)
        binding.tvTeamName.setText(teamName);
    }

    /**
     * ViewPager2 ve TabLayout'u ayarlar.
     */
    private void setupViewPager() {
        TeamDetailPagerAdapter pagerAdapter = new TeamDetailPagerAdapter(
                getChildFragmentManager(), getLifecycle(), teamId, leagueId);
        binding.viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    if (position == 0) tab.setText("MAÇLAR");
                    else tab.setText("KADRO");
                }).attach();
    }

    // ===== Veri Yükleme =====

    /**
     * Takım profil bilgilerini API'den çeker ve hero alanını doldurur.
     */
    private void loadTeamProfile() {
        if (teamId == null || teamId.isEmpty()) return;

        repository.getTeamProfile(teamId).enqueue(new Callback<ApiResponse<TeamProfileModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<TeamProfileModel>> call,
                    @NonNull Response<ApiResponse<TeamProfileModel>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<TeamProfileModel> data = response.body().getData();
                    if (data != null && !data.isEmpty()) {
                        bindTeamProfile(data.get(0));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<TeamProfileModel>> call,
                    @NonNull Throwable t) {
                // Profil yüklenemedi, hero'daki takım adı zaten yazılı
            }
        });
    }

    /**
     * Profil verisini hero alanına bağlar.
     */
    private void bindTeamProfile(TeamProfileModel profile) {
        // Logo
        if (profile.getLogo() != null && !profile.getLogo().isEmpty()) {
            Glide.with(this)
                    .load(profile.getLogo())
                    .placeholder(R.drawable.ic_person)
                    .into(binding.ivTeamLogo);
        }

        // Takım adı (profile'dan geleni kullan, boşsa constructor'dan gelen teamName kalır)
        if (profile.getName() != null && !profile.getName().isEmpty()) {
            binding.tvTeamName.setText(profile.getName());
            binding.collapsingToolbar.setTitle(profile.getName());
            binding.toolbar.setTitle(profile.getName());
        }

        // Stadyum
        if (profile.getVenue() != null && !profile.getVenue().isEmpty()) {
            binding.tvTeamVenue.setText("🏟 " + profile.getVenue());
            binding.tvTeamVenue.setVisibility(View.VISIBLE);
        }

        // Antrenör
        if (profile.getCoach() != null && !profile.getCoach().isEmpty()) {
            binding.tvTeamCoach.setText("👤 " + profile.getCoach());
            binding.tvTeamCoach.setVisibility(View.VISIBLE);
        }

        // Kuruluş yılı
        if (profile.getFoundingDate() != null && !profile.getFoundingDate().isEmpty()) {
            binding.tvTeamFounded.setText("Est. " + profile.getFoundingDate());
            binding.tvTeamFounded.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Son 5 maçın form göstergesini yükler.
     */
    private void loadRecentForm() {
        if (leagueId == null || leagueId.isEmpty()) return;

        repository.getLeagueMatches(leagueId).enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Response<ApiResponse<MatchModel>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<MatchModel> all = response.body().getData();
                    if (all != null) {
                        buildFormIndicator(all);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Throwable t) {
                // Form yüklenemedi, gösterge boş kalır
            }
        });
    }

    /**
     * Takımın son 5 bitmiş maçından form noktaları oluşturur.
     */
    private void buildFormIndicator(List<MatchModel> all) {
        // Takımın bitmiş maçlarını filtrele
        List<MatchModel> finished = new ArrayList<>();
        for (MatchModel m : all) {
            if ((teamId.equals(m.getHomeId()) || teamId.equals(m.getAwayId()))
                    && m.isFinished()) {
                finished.add(m);
            }
        }
        if (finished.isEmpty()) return;

        // Tarihe göre azalan sırala, son 5 al
        Collections.sort(finished, new Comparator<MatchModel>() {
            @Override
            public int compare(MatchModel a, MatchModel b) {
                return Long.compare(b.getMatchTime(), a.getMatchTime());
            }
        });
        int count = Math.min(5, finished.size());
        List<MatchModel> last5 = finished.subList(0, count);

        // Gösterim için eski→yeni sıraya çevir
        Collections.reverse(last5);

        // LinearLayout'a form daireleri ekle
        binding.llForm.removeAllViews();
        int dp22 = (int) (22 * getResources().getDisplayMetrics().density);
        int dp4  = (int) (4  * getResources().getDisplayMetrics().density);

        for (MatchModel m : last5) {
            boolean isHome = teamId.equals(m.getHomeId());
            int teamScore = isHome ? m.getHomeScore() : m.getAwayScore();
            int oppScore  = isHome ? m.getAwayScore() : m.getHomeScore();

            String label;
            int bgColor;
            if (teamScore > oppScore) {
                label = "G"; bgColor = 0xFF388E3C;  // Win - yeşil
            } else if (teamScore == oppScore) {
                label = "B"; bgColor = 0xFFF57F17;  // Draw - sarı
            } else {
                label = "M"; bgColor = 0xFFC62828;  // Loss - kırmızı
            }

            TextView dot = new TextView(requireContext());
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(bgColor);
            dot.setBackground(shape);
            dot.setText(label);
            dot.setTextColor(Color.WHITE);
            dot.setTextSize(9f);
            dot.setGravity(android.view.Gravity.CENTER);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp22, dp22);
            params.setMargins(0, 0, dp4, 0);
            dot.setLayoutParams(params);

            binding.llForm.addView(dot);
        }
    }

    // ===== Favori İşlemleri =====

    private void checkFavoriteStatus() {
        if (!FirebaseManager.isLoggedIn()) return;
        FirebaseManager.isTeamOrLeagueFavorite(teamId, leagueId, isFav -> {
            if (!isAdded()) return;
            isFavorite = isFav;
            updateFavoriteIcon();
        });
    }

    private void updateFavoriteIcon() {
        binding.btnFavorite.setIconResource(
                isFavorite ? R.drawable.ic_star_filled : R.drawable.ic_star
        );
    }

    private void toggleFavorite() {
        if (!FirebaseManager.isLoggedIn()) {
            Snackbar.make(binding.getRoot(),
                    getString(R.string.login_required),
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        if (isFavorite) {
            FirebaseManager.removeFavoriteTeam(teamId,
                    unused -> {
                        if (!isAdded()) return;
                        isFavorite = false;
                        updateFavoriteIcon();
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.removed_from_favorites),
                                Snackbar.LENGTH_SHORT).show();
                    },
                    e -> showSnackbar(getString(R.string.error_loading))
            );
        } else {
            FirebaseManager.addFavoriteTeam(teamId, teamName, leagueId,
                    unused -> {
                        if (!isAdded()) return;
                        isFavorite = true;
                        updateFavoriteIcon();
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.added_to_favorites),
                                Snackbar.LENGTH_SHORT).show();
                    },
                    e -> showSnackbar(getString(R.string.error_loading))
            );
        }
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ===== İç Sınıf: Pager Adapter =====

    /**
     * ViewPager2 için fragment adapter.
     * Tab 0: Maçlar (TeamMatchesTabFragment)
     * Tab 1: Kadro  (TeamSquadFragment)
     */
    private static class TeamDetailPagerAdapter extends FragmentStateAdapter {

        private final String teamId;
        private final String leagueId;

        TeamDetailPagerAdapter(@NonNull FragmentManager fm,
                @NonNull androidx.lifecycle.Lifecycle lifecycle,
                String teamId, String leagueId) {
            super(fm, lifecycle);
            this.teamId   = teamId;
            this.leagueId = leagueId;
        }

        @Override
        public int getItemCount() { return 2; }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return TeamMatchesTabFragment.newInstance(teamId, leagueId);
            } else {
                return TeamSquadFragment.newInstance(teamId);
            }
        }
    }
}
