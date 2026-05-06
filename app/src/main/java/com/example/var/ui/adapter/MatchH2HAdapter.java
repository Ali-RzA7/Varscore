package com.example.var.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.var.R;

import java.util.ArrayList;
import java.util.List;

/**
 * MatchH2HAdapter - Geçmiş rekabet (H2H) maçlarını listeleyen adapter.
 * Gelen CSV verilerini parçalayarak gösterir.
 */
public class MatchH2HAdapter extends RecyclerView.Adapter<MatchH2HAdapter.H2HViewHolder> {

    private List<String> h2hData = new ArrayList<>();

    public void setH2HData(List<String> data) {
        this.h2hData = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public H2HViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match_h2h, parent, false);
        return new H2HViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull H2HViewHolder holder, int position) {
        String rawData = h2hData.get(position);
        holder.bind(rawData);
    }

    @Override
    public int getItemCount() {
        return h2hData.size();
    }

    static class H2HViewHolder extends RecyclerView.ViewHolder {
        TextView tvLeagueName, tvDate, tvHomeTeam, tvScore, tvAwayTeam;

        H2HViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLeagueName = itemView.findViewById(R.id.tvLeagueName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvHomeTeam = itemView.findViewById(R.id.tvHomeTeam);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvAwayTeam = itemView.findViewById(R.id.tvAwayTeam);
        }

        void bind(String rawData) {
            if (rawData == null) return;
            
            // CSV Parçalama
            String[] parts = rawData.split(",");
            
            if (parts.length > 10) {
                // Python indexlerine göre:
                // 1: Lig, 2: Tarih, 4: Ev, 6: Dep, 8: Ev Skor, 9: Dep Skor
                tvLeagueName.setText(parts[1].trim());
                tvDate.setText(parts[2].trim());
                tvHomeTeam.setText(parts[4].trim());
                tvAwayTeam.setText(parts[6].trim());
                tvScore.setText(parts[8].trim() + " - " + parts[9].trim());
            } else {
                tvLeagueName.setText("-");
                tvDate.setText("-");
                tvHomeTeam.setText("Veri Hatası");
                tvAwayTeam.setText("-");
                tvScore.setText("-");
            }
        }
    }
}
