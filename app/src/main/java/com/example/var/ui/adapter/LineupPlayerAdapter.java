package com.example.var.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.var.R;
import com.example.var.data.model.LineupModel.PlayerModel;

import java.util.ArrayList;
import java.util.List;

public class LineupPlayerAdapter extends RecyclerView.Adapter<LineupPlayerAdapter.PlayerViewHolder> {

    private List<PlayerModel> homePlayers = new ArrayList<>();
    private List<PlayerModel> awayPlayers = new ArrayList<>();

    public void setPlayers(List<PlayerModel> home, List<PlayerModel> away) {
        this.homePlayers = home != null ? home : new ArrayList<>();
        this.awayPlayers = away != null ? away : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lineup_player, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        PlayerModel home = position < homePlayers.size() ? homePlayers.get(position) : null;
        PlayerModel away = position < awayPlayers.size() ? awayPlayers.get(position) : null;

        if (home != null) {
            holder.tvHomeNumber.setText(String.valueOf(home.getNumber()));
            holder.tvHomeName.setText(home.getPlayerName() != null ? home.getPlayerName() : "-");
        } else {
            holder.tvHomeNumber.setText("");
            holder.tvHomeName.setText("");
        }

        if (away != null) {
            holder.tvAwayNumber.setText(String.valueOf(away.getNumber()));
            holder.tvAwayName.setText(away.getPlayerName() != null ? away.getPlayerName() : "-");
        } else {
            holder.tvAwayNumber.setText("");
            holder.tvAwayName.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return Math.max(homePlayers.size(), awayPlayers.size());
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        TextView tvHomeNumber, tvHomeName, tvAwayNumber, tvAwayName;

        PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHomeNumber = itemView.findViewById(R.id.tvHomeNumber);
            tvHomeName   = itemView.findViewById(R.id.tvHomeName);
            tvAwayNumber = itemView.findViewById(R.id.tvAwayNumber);
            tvAwayName   = itemView.findViewById(R.id.tvAwayName);
        }
    }
}
