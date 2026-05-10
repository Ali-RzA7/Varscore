package com.example.var.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.var.R;
import com.example.var.data.model.StatModel;

import java.util.ArrayList;
import java.util.List;

/**
 * MatchStatAdapter - Maç istatistiklerini (karşılaştırmalı barlar) listeleyen adapter.
 */
public class MatchStatAdapter extends RecyclerView.Adapter<MatchStatAdapter.StatViewHolder> {

    private List<StatModel> stats = new ArrayList<>();

    public void setStats(List<StatModel> stats) {
        this.stats = stats;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match_stat, parent, false);
        return new StatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatViewHolder holder, int position) {
        StatModel stat = stats.get(position);
        holder.bind(stat);
    }

    @Override
    public int getItemCount() {
        return stats.size();
    }

    static class StatViewHolder extends RecyclerView.ViewHolder {
        TextView tvHomeValue, tvAwayValue, tvStatName;
        View viewHomeBar, viewAwayBar;

        StatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHomeValue = itemView.findViewById(R.id.tvHomeValue);
            tvAwayValue = itemView.findViewById(R.id.tvAwayValue);
            tvStatName = itemView.findViewById(R.id.tvStatName);
            viewHomeBar = itemView.findViewById(R.id.viewHomeBar);
            viewAwayBar = itemView.findViewById(R.id.viewAwayBar);
        }

        void bind(StatModel stat) {
            int nameResId = stat.getTypeNameResId();
            if (nameResId != 0) {
                tvStatName.setText(itemView.getContext().getString(nameResId));
            } else {
                tvStatName.setText("-");
            }
            
            tvHomeValue.setText(stat.getHomeValue());
            tvAwayValue.setText(stat.getAwayValue());

            int homeWeight = stat.getHomePercentage();
            int awayWeight = 100 - homeWeight;

            // LayoutParams ağırlıklarını ayarla
            android.widget.LinearLayout.LayoutParams homeParams = (android.widget.LinearLayout.LayoutParams) viewHomeBar.getLayoutParams();
            homeParams.weight = homeWeight;
            viewHomeBar.setLayoutParams(homeParams);

            android.widget.LinearLayout.LayoutParams awayParams = (android.widget.LinearLayout.LayoutParams) viewAwayBar.getLayoutParams();
            awayParams.weight = awayWeight;
            viewAwayBar.setLayoutParams(awayParams);
        }
    }
}
