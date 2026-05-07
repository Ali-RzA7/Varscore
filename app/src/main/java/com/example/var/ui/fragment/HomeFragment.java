package com.example.var.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.var.databinding.FragmentHomeBinding;
import com.example.var.ui.adapter.DatePickerAdapter;
import com.example.var.ui.adapter.MatchAdapter;
import com.example.var.ui.dialog.SearchDialogFragment;
import com.example.var.ui.dialog.SettingsDialogFragment;
import com.example.var.ui.fragment.MatchDetailFragment;
import com.example.var.util.DateUtils;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HomeFragment - Ana sayfa fragment'ı.
 * Uygulamanın ana ekranı olarak tarih seçici, carousel toggle,
 * maç listesi ve arama/ayarlar butonlarını yönetir.
 *
 * Yapı:
 * 1. Toolbar: Uygulama adı + Arama + Ayarlar butonları
 * 2. Carousel Toggle: Canlı Maçlar / Tüm Maçlar geçiş butonları
 * 3. Date Picker: Yatay kaydırılabilir tarih seçici
 * 4. Match List: Lig bazında gruplu maç listesi
 */
public class HomeFragment extends Fragment implements
        DatePickerAdapter.OnDateSelectedListener,
        MatchAdapter.OnMatchClickListener {

    /** ViewBinding referansı */
    private FragmentHomeBinding binding;

    /** Tarih seçici adapter */
    private DatePickerAdapter datePickerAdapter;

    /** Maç listesi adapter */
    private MatchAdapter matchAdapter;

    /** Veri erişim katmanı */
    private MatchRepository repository;

    /** Seçili tarih */
    private Calendar selectedDate;

    /** Canlı maç modu aktif mi? */
    private boolean isLiveMode = false;

    /** Canlı skor polling handler'ı */
    private Handler pollingHandler;

    /** Polling çalışıyor mu? */
    private boolean isPolling = false;

    /** Polling aralığı: 15 saniye */
    private static final long POLLING_INTERVAL = 15000;

    private static final String API_KEY = BuildConfig.API_KEY;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new MatchRepository(API_KEY);
        pollingHandler = new Handler(Looper.getMainLooper());
        selectedDate = Calendar.getInstance();

        // UI bileşenlerini kur
        setupToolbar();
        setupCarouselToggle();
        setupDatePicker();
        setupMatchList();
        setupSwipeRefresh();

        // İlk veri yüklemesi (bugünün maçları)
        loadMatchesForDate(selectedDate);
    }

    // ================================================================
    // Toolbar Kurulumu
    // ================================================================

    /**
     * Toolbar butonlarını yapılandırır.
     * Arama ve ayarlar butonlarına tıklama olayları atanır.
     */
    private void setupToolbar() {
        // Arama butonu - SearchDialogFragment açar
        binding.btnSearch.setOnClickListener(v -> {
            SearchDialogFragment searchDialog = new SearchDialogFragment();
            searchDialog.show(getParentFragmentManager(), "search_dialog");
        });

        // Ayarlar butonu - SettingsDialogFragment açar
        binding.btnSettings.setOnClickListener(v -> {
            SettingsDialogFragment settingsDialog = new SettingsDialogFragment();
            settingsDialog.show(getParentFragmentManager(), "settings_dialog");
        });
    }

    // ================================================================
    // Carousel Toggle Kurulumu
    // ================================================================

    /**
     * Canlı Maçlar / Tüm Maçlar geçiş butonlarını yapılandırır.
     * Aktif mod butonun arka plan rengini değiştirir.
     */
    private void setupCarouselToggle() {
        // Canlı Maçlar butonuna tıklama
        binding.btnLiveMatches.setOnClickListener(v -> {
            if (!isLiveMode) {
                isLiveMode = true;
                updateCarouselUI();
                // Date picker'ı gizle (canlı modda tarih seçilmez)
                binding.datePickerContainer.setVisibility(View.GONE);
                loadLiveMatches();
                startPolling();
            }
        });

        // Tüm Maçlar butonuna tıklama
        binding.btnAllMatches.setOnClickListener(v -> {
            if (isLiveMode) {
                isLiveMode = false;
                updateCarouselUI();
                // Date picker'ı göster
                binding.datePickerContainer.setVisibility(View.VISIBLE);
                stopPolling();
                loadMatchesForDate(selectedDate);
            }
        });
    }

    /**
     * Carousel butonlarının görsel durumunu günceller.
     * Aktif buton altın sarısı, pasif buton şeffaf olur.
     */
    private void updateCarouselUI() {
        if (isLiveMode) {
            // Canlı mod aktif
            binding.btnLiveMatches.setBackgroundTintList(
                    requireContext().getColorStateList(R.color.secondary));
            binding.btnLiveMatches.setTextColor(requireContext().getColor(R.color.on_secondary));
            binding.btnAllMatches.setBackgroundTintList(
                    requireContext().getColorStateList(android.R.color.transparent));
            binding.btnAllMatches.setTextColor(requireContext().getColor(R.color.white));
        } else {
            // Tüm maçlar modu aktif
            binding.btnAllMatches.setBackgroundTintList(
                    requireContext().getColorStateList(R.color.secondary));
            binding.btnAllMatches.setTextColor(requireContext().getColor(R.color.on_secondary));
            binding.btnLiveMatches.setBackgroundTintList(
                    requireContext().getColorStateList(android.R.color.transparent));
            binding.btnLiveMatches.setTextColor(requireContext().getColor(R.color.white));
        }
    }

    // ================================================================
    // Date Picker Kurulumu
    // ================================================================

    /**
     * Yatay tarih seçici RecyclerView'ı yapılandırır.
     * LinearLayoutManager ile yatay scroll sağlanır.
     * Bugünün tarihi ortada görünecek şekilde kaydırılır.
     */
    private void setupDatePicker() {
        datePickerAdapter = new DatePickerAdapter(requireContext(), this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.rvDatePicker.setLayoutManager(layoutManager);
        binding.rvDatePicker.setAdapter(datePickerAdapter);

        // Bugünün tarihini ortaya kaydır
        binding.rvDatePicker.post(() -> {
            int todayPos = datePickerAdapter.getTodayPosition();
            layoutManager.scrollToPositionWithOffset(todayPos,
                    binding.rvDatePicker.getWidth() / 2 - 80);
        });

        // Takvim Picker Butonu
        binding.btnOpenCalendar.setOnClickListener(v -> openCalendarPicker());
    }

    /**
     * Custom Calendar Picker diyaloğunu açar.
     */
    private void openCalendarPicker() {
        com.example.var.ui.dialog.CalendarDialogFragment calendarDialog =
                new com.example.var.ui.dialog.CalendarDialogFragment(selectedDate, this::onDateSelectedFromCalendar);
        calendarDialog.show(getParentFragmentManager(), "calendar_picker");
    }

    /**
     * Takvim diyaloğundan tarih seçildiğinde çağrılır.
     */
    private void onDateSelectedFromCalendar(Calendar calendar) {
        // State güncelle
        selectedDate = calendar;

        // Yatay slider'ı güncelle ve oraya kaydır
        int position = datePickerAdapter.setSelectedDate(calendar);
        if (binding.rvDatePicker.getLayoutManager() instanceof LinearLayoutManager) {
            ((LinearLayoutManager) binding.rvDatePicker.getLayoutManager())
                    .scrollToPositionWithOffset(position, binding.rvDatePicker.getWidth() / 2 - 80);
        }

        // Maçları yükle
        loadMatchesForDate(calendar);
    }

    // ================================================================
    // Maç Listesi Kurulumu
    // ================================================================

    /**
     * Maç listesi RecyclerView'ı yapılandırır.
     */
    private void setupMatchList() {
        matchAdapter = new MatchAdapter(requireContext(), this);
        binding.rvMatches.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        binding.rvMatches.setAdapter(matchAdapter);
    }

    /**
     * SwipeRefreshLayout pull-to-refresh yapılandırması.
     */
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
                R.color.primary, R.color.secondary);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isLiveMode) {
                loadLiveMatches();
            } else {
                loadMatchesForDate(selectedDate);
            }
        });
    }

    // ================================================================
    // Veri Yükleme
    // ================================================================

    /**
     * Seçilen tarihteki maçları API'den çeker.
     * Yükleme sırasında loading göstergesi, hata durumunda error göstergesi
     * gösterilir.
     *
     * @param calendar Maçları çekilecek tarih
     */
    private void loadMatchesForDate(Calendar calendar) {
        showLoading();

        String dateStr = DateUtils.formatForApi(calendar);
        repository.getMatchesByDate(dateStr).enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Response<ApiResponse<MatchModel>> response) {
                if (!isAdded())
                    return;
                binding.swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<MatchModel> matches = response.body().getData();
                    if (matches != null && !matches.isEmpty()) {
                        showContent();
                        matchAdapter.setMatches(matches);
                    } else {
                        showEmpty(getString(R.string.no_matches));
                    }
                } else {
                    showError(getString(R.string.error_loading));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Throwable t) {
                if (!isAdded())
                    return;
                binding.swipeRefreshLayout.setRefreshing(false);
                showError(getString(R.string.error_loading));
            }
        });
    }

    /**
     * Canlı maçları API'den çeker.
     */
    private void loadLiveMatches() {
        showLoading();

        repository.getLiveScores().enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Response<ApiResponse<MatchModel>> response) {
                if (!isAdded())
                    return;
                binding.swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<MatchModel> allMatches = response.body().getData();
                    // Sadece canlı maçları filtrele (status 1-5)
                    List<MatchModel> liveMatches = new ArrayList<>();
                    if (allMatches != null) {
                        for (MatchModel m : allMatches) {
                            if (m.isLive())
                                liveMatches.add(m);
                        }
                    }
                    if (!liveMatches.isEmpty()) {
                        showContent();
                        matchAdapter.setMatches(liveMatches);
                    } else {
                        showEmpty(getString(R.string.no_live_matches));
                    }
                } else {
                    showError(getString(R.string.error_loading));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Throwable t) {
                if (!isAdded())
                    return;
                binding.swipeRefreshLayout.setRefreshing(false);
                showError(getString(R.string.error_loading));
            }
        });
    }

    // ================================================================
    // Canlı Skor Polling (Her 15 saniyede skor değişikliklerini sorgula)
    // ================================================================

    /** Polling Runnable - Tekrarlayan skor güncelleme görevi */
    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPolling && isLiveMode) {
                loadLiveMatches();
                pollingHandler.postDelayed(this, POLLING_INTERVAL);
            }
        }
    };

    /** Polling'i başlatır */
    private void startPolling() {
        isPolling = true;
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL);
    }

    /** Polling'i durdurur */
    private void stopPolling() {
        isPolling = false;
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    // ================================================================
    // UI Durum Yönetimi
    // ================================================================

    /** Loading göstergesini gösterir */
    private void showLoading() {
        binding.loadingContainer.setVisibility(View.VISIBLE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setVisibility(View.GONE);
    }

    /** İçerik görünümünü gösterir */
    private void showContent() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setVisibility(View.VISIBLE);
    }

    /** Boş durum mesajını gösterir */
    private void showEmpty(String message) {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.VISIBLE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setVisibility(View.GONE);
        binding.tvEmptyMessage.setText(message);
    }

    /** Hata mesajını gösterir ve retry butonu yapılandırır */
    private void showError(String message) {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.VISIBLE);
        binding.swipeRefreshLayout.setVisibility(View.GONE);
        binding.tvErrorMessage.setText(message);

        binding.btnRetry.setOnClickListener(v -> {
            if (isLiveMode)
                loadLiveMatches();
            else
                loadMatchesForDate(selectedDate);
        });
    }

    // ================================================================
    // Callback Implementations
    // ================================================================

    /** DatePickerAdapter callback: Tarih seçildiğinde maçları yükle */
    @Override
    public void onDateSelected(Calendar calendar) {
        selectedDate = calendar;
        loadMatchesForDate(calendar);
    }

    /** MatchAdapter callback: Maça tıklandığında detay bilgi göster */
    @Override
    public void onMatchClick(MatchModel match) {
        MatchDetailFragment detailFragment = MatchDetailFragment.newInstance(match);
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                )
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    // ================================================================
    // Lifecycle
    // ================================================================

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isLiveMode)
            startPolling();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
        binding = null;
    }
}
