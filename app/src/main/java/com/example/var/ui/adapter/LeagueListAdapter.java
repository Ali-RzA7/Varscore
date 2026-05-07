package com.example.var.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.var.R;
import com.example.var.data.model.LeagueModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * LeagueListAdapter - Puan Durumu ekranındaki lig listesi için RecyclerView adaptörü.
 *
 * Her satırda şunlar gösterilir:
 * - Lig renk çubuğu (iSportsAPI'den gelen leagueColor)
 * - Lig tipi ikonu: ⚽ (Lig) veya 🏆 (Kupa)
 * - Lig tam adı ve kısa adı
 * - Favori lig butonu (yıldız)
 * - Puan tablosuna gitme butonu (ok)
 *
 * Callback'ler:
 * - OnLeagueClickListener: Puan tablosunu açmak için lig satırına tıklanınca
 * - OnFavoriteClickListener: Favoriye ekle/çıkar butonuna tıklanınca
 */
public class LeagueListAdapter extends RecyclerView.Adapter<LeagueListAdapter.LeagueViewHolder> {

    /** Tüm lig listesi */
    private List<LeagueModel> leagues = new ArrayList<>();

    /** Arama sonucu filtrelenmiş lig listesi */
    private List<LeagueModel> filteredLeagues = new ArrayList<>();

    /** Kullanıcının favori lig ID'leri (yıldız durumunu belirlemek için) */
    private List<String> favoriteLeagueIds = new ArrayList<>();

    /** Lig satırına tıklanınca puan tablosunu açan callback */
    private final OnLeagueClickListener leagueClickListener;

    /** Favori butonuna tıklanınca ekleme/çıkarma yapan callback */
    private final OnFavoriteClickListener favoriteClickListener;

    /**
     * LeagueListAdapter yapıcı metodu.
     *
     * @param leagueClickListener   Lig satırı tıklanma callback'i
     * @param favoriteClickListener Favori butonu tıklanma callback'i
     */
    public LeagueListAdapter(OnLeagueClickListener leagueClickListener,
                             OnFavoriteClickListener favoriteClickListener) {
        this.leagueClickListener = leagueClickListener;
        this.favoriteClickListener = favoriteClickListener;
    }

    /**
     * Lig listesini günceller ve RecyclerView'ı yeniler.
     * Hem tam listeyi hem de filtrelenmiş listeyi günceller.
     *
     * @param leagues API'den gelen lig listesi
     */
    public void setLeagues(List<LeagueModel> leagues) {
        this.leagues = leagues != null ? leagues : new ArrayList<>();
        this.filteredLeagues = new ArrayList<>(this.leagues);
        notifyDataSetChanged();
    }

    /**
     * Favori lig ID listesini günceller.
     * Yıldız ikonlarının dolu/boş gösterimini etkiler.
     *
     * @param favoriteIds Kullanıcının favori lig ID'leri
     */
    public void setFavoriteLeagueIds(List<String> favoriteIds) {
        this.favoriteLeagueIds = favoriteIds != null ? favoriteIds : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Arama metnine göre ligleri filtreler.
     * Büyük/küçük harf duyarsız filtreleme yapar.
     *
     * @param query Arama metni (boş ise tüm ligler gösterilir)
     */
    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            filteredLeagues = new ArrayList<>(leagues);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            filteredLeagues = new ArrayList<>();
            for (LeagueModel league : leagues) {
                if (league.getName() != null && league.getName().toLowerCase().contains(lowerQuery)) {
                    filteredLeagues.add(league);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LeagueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_league, parent, false);
        return new LeagueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeagueViewHolder holder, int position) {
        LeagueModel league = filteredLeagues.get(position);
        holder.bind(league, favoriteLeagueIds.contains(league.getLeagueId()));
    }

    @Override
    public int getItemCount() {
        return filteredLeagues.size();
    }

    /**
     * LeagueViewHolder - Her lig satırının görünüm bağlama sınıfı.
     */
    class LeagueViewHolder extends RecyclerView.ViewHolder {

        private final View viewLeagueColor;
        private final TextView tvLeagueIcon;
        private final TextView tvLeagueName;
        private final TextView tvLeagueShortName;
        private final MaterialButton btnFavoriteLeague;
        private final MaterialButton btnOpenLeague;

        LeagueViewHolder(@NonNull View itemView) {
            super(itemView);
            viewLeagueColor = itemView.findViewById(R.id.viewLeagueColor);
            tvLeagueIcon = itemView.findViewById(R.id.tvLeagueIcon);
            tvLeagueName = itemView.findViewById(R.id.tvLeagueName);
            tvLeagueShortName = itemView.findViewById(R.id.tvLeagueShortName);
            btnFavoriteLeague = itemView.findViewById(R.id.btnFavoriteLeague);
            btnOpenLeague = itemView.findViewById(R.id.btnOpenLeague);
        }

        /**
         * Lig verilerini ve favori durumunu görünüme bağlar.
         *
         * @param league     Gösterilecek lig modeli
         * @param isFavorite Lig favori listesinde mi?
         */
        void bind(LeagueModel league, boolean isFavorite) {
            // Lig adı ve kısa adı
            tvLeagueName.setText(league.getName());
            tvLeagueShortName.setText(league.getShortName() != null ? league.getShortName() : "");

            // Lig tipi ikonu: 1=Lig(⚽), 2=Kupa(🏆)
            tvLeagueIcon.setText(league.getType() == 2 ? "🏆" : "⚽");

            // Lig renk çubuğu (API'den gelen renk kodu)
            try {
                if (league.getLeagueColor() != null && !league.getLeagueColor().isEmpty()) {
                    viewLeagueColor.setBackgroundColor(Color.parseColor(league.getLeagueColor()));
                }
            } catch (IllegalArgumentException e) {
                // Geçersiz renk kodu - varsayılan rengi kullan
                viewLeagueColor.setBackgroundResource(R.color.primary);
            }

            // Favori durumuna göre yıldız ikonunu güncelle
            btnFavoriteLeague.setIconResource(
                    isFavorite ? R.drawable.ic_star_filled : R.drawable.ic_star
            );

            // Favori buton tıklaması
            btnFavoriteLeague.setOnClickListener(v -> {
                if (favoriteClickListener != null) {
                    favoriteClickListener.onFavoriteClick(league, isFavorite);
                }
            });

            // Lig satırına veya ok butonuna tıklanınca puan tablosunu aç
            itemView.setOnClickListener(v -> {
                if (leagueClickListener != null) {
                    leagueClickListener.onLeagueClick(league);
                }
            });
            btnOpenLeague.setOnClickListener(v -> {
                if (leagueClickListener != null) {
                    leagueClickListener.onLeagueClick(league);
                }
            });
        }
    }

    /**
     * OnLeagueClickListener - Lig satırına tıklanınca çağrılan callback arayüzü.
     * StandingsFragment'ta LeagueStandingsFragment'a geçiş için kullanılır.
     */
    public interface OnLeagueClickListener {
        /**
         * @param league Tıklanan lig modeli
         */
        void onLeagueClick(LeagueModel league);
    }

    /**
     * OnFavoriteClickListener - Favori butona tıklanınca çağrılan callback arayüzü.
     * StandingsFragment ve LeagueStandingsFragment'ta Firebase işlemi başlatır.
     */
    public interface OnFavoriteClickListener {
        /**
         * @param league       Favoriye eklenecek/çıkarılacak lig
         * @param currentState Mevcut favori durumu (true = zaten favoride)
         */
        void onFavoriteClick(LeagueModel league, boolean currentState);
    }
}
