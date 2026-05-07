package com.example.var.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.var.R;
import com.example.var.data.model.StandingModel;

import java.util.ArrayList;
import java.util.List;

/**
 * StandingsTableAdapter - Lig puan tablosu için RecyclerView adaptörü.
 *
 * Her satır bir takımın puan tablosundaki verilerini gösterir:
 * Sıra | Takım Adı | Oynanan | G | B | M | Averaj | Puan
 *
 * Kullanım yeri: LeagueStandingsFragment
 *
 * Tıklama davranışı:
 * Bir takıma tıklandığında TeamMatchesFragment açılır.
 */
public class StandingsTableAdapter extends RecyclerView.Adapter<StandingsTableAdapter.StandingViewHolder> {

    /** Puan tablosu satırları */
    private List<StandingModel> standings = new ArrayList<>();

    /** Takım satırına tıklandığında çağrılan callback */
    private final OnTeamClickListener teamClickListener;

    /**
     * @param teamClickListener Takım satırı tıklanma callback'i
     */
    public StandingsTableAdapter(OnTeamClickListener teamClickListener) {
        this.teamClickListener = teamClickListener;
    }

    /**
     * Puan tablosu verilerini günceller.
     *
     * @param standings API'den gelen puan tablosu listesi
     */
    public void setStandings(List<StandingModel> standings) {
        this.standings = standings != null ? standings : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StandingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_standing, parent, false);
        return new StandingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StandingViewHolder holder, int position) {
        StandingModel standing = standings.get(position);
        holder.bind(standing, position);
    }

    @Override
    public int getItemCount() {
        return standings.size();
    }

    /**
     * StandingViewHolder - Her puan tablosu satırının görünüm bağlama sınıfı.
     */
    class StandingViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvRank;
        private final TextView tvTeamName;
        private final TextView tvPlayed;
        private final TextView tvWon;
        private final TextView tvDrawn;
        private final TextView tvLost;
        private final TextView tvGoalDiff;
        private final TextView tvPoints;

        StandingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvTeamName = itemView.findViewById(R.id.tvTeamName);
            tvPlayed = itemView.findViewById(R.id.tvPlayed);
            tvWon = itemView.findViewById(R.id.tvWon);
            tvDrawn = itemView.findViewById(R.id.tvDrawn);
            tvLost = itemView.findViewById(R.id.tvLost);
            tvGoalDiff = itemView.findViewById(R.id.tvGoalDiff);
            tvPoints = itemView.findViewById(R.id.tvPoints);
        }

        /**
         * Puan tablosu satır verilerini görünüme bağlar.
         *
         * @param standing Takımın puan tablosu verisi
         * @param position Listedeki pozisyon (alternatif arka plan rengi için)
         */
        void bind(StandingModel standing, int position) {
            // Sıra numarası
            tvRank.setText(String.valueOf(standing.getRank() > 0 ? standing.getRank() : position + 1));

            // Takım adı
            tvTeamName.setText(standing.getTeamName() != null ? standing.getTeamName() : "-");

            // Maç istatistikleri
            tvPlayed.setText(String.valueOf(standing.getPlayed()));
            tvWon.setText(String.valueOf(standing.getWon()));
            tvDrawn.setText(String.valueOf(standing.getDrawn()));
            tvLost.setText(String.valueOf(standing.getLost()));

            // Gol farkı (+ veya - ön eki ile)
            int diff = standing.getGoalDifference();
            tvGoalDiff.setText(diff > 0 ? "+" + diff : String.valueOf(diff));

            // Puan
            tvPoints.setText(String.valueOf(standing.getPoints()));

            // Alternatif satır arka planı (zebra efekti) - okunabilirlik için
            itemView.setAlpha(position % 2 == 0 ? 1.0f : 0.95f);

            // Takıma tıklanınca geçmiş maçları göster
            itemView.setOnClickListener(v -> {
                if (teamClickListener != null) {
                    teamClickListener.onTeamClick(standing);
                }
            });
        }
    }

    /**
     * OnTeamClickListener - Puan tablosundaki takıma tıklanınca çağrılan callback.
     * LeagueStandingsFragment'ta TeamMatchesFragment'a geçiş için kullanılır.
     */
    public interface OnTeamClickListener {
        /**
         * @param standing Tıklanan takımın puan tablosu verisi
         */
        void onTeamClick(StandingModel standing);
    }
}
