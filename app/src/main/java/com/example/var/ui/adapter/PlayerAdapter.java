package com.example.var.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.var.R;
import com.example.var.data.model.PlayerModel;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PlayerAdapter - Oyuncuları pozisyon başlıklarıyla gruplanmış listeleyen adapter.
 * İki view type kullanır:
 *   - TYPE_HEADER (0): Pozisyon başlığı (Kaleciler, Defanslar, vb.)
 *   - TYPE_PLAYER (1): Oyuncu satırı
 */
public class PlayerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PLAYER = 1;

    /** Karma liste: String (başlık) veya PlayerModel (oyuncu) */
    private final List<Object> items = new ArrayList<>();

    /**
     * Oyuncu listesini pozisyona göre gruplandırarak listeye ekler.
     * Sıralama: GK > DF > MF > FW, aynı pozisyonda forma numarasına göre.
     */
    public void setPlayers(List<PlayerModel> players) {
        items.clear();
        if (players == null || players.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // Pozisyon sıralama değerleri
        Map<String, Integer> posOrder = new LinkedHashMap<>();
        posOrder.put("GK", 0);
        posOrder.put("DF", 1);
        posOrder.put("MF", 2);
        posOrder.put("FW", 3);

        // Oyuncuları normalize pozisyona göre gruplandır
        Map<String, List<PlayerModel>> groups = new LinkedHashMap<>();
        for (String pos : posOrder.keySet()) {
            groups.put(pos, new ArrayList<>());
        }
        List<PlayerModel> others = new ArrayList<>();

        for (PlayerModel player : players) {
            String pos = player.getPosition();
            if (pos != null && pos.equalsIgnoreCase("Coach")) continue;
            String norm = normalizePosition(pos);
            if (groups.containsKey(norm)) {
                groups.get(norm).add(player);
            } else {
                others.add(player);
            }
        }

        // Her grubu forma numarasına göre sırala ve listeye ekle
        for (Map.Entry<String, List<PlayerModel>> entry : groups.entrySet()) {
            List<PlayerModel> group = entry.getValue();
            if (group.isEmpty()) continue;
            Collections.sort(group, new Comparator<PlayerModel>() {
                @Override
                public int compare(PlayerModel a, PlayerModel b) {
                    return Integer.compare(a.getNumber(), b.getNumber());
                }
            });
            items.add(getPositionLabel(entry.getKey()));
            items.addAll(group);
        }
        if (!others.isEmpty()) {
            Collections.sort(others, new Comparator<PlayerModel>() {
                @Override
                public int compare(PlayerModel a, PlayerModel b) {
                    return Integer.compare(a.getNumber(), b.getNumber());
                }
            });
            items.add("Diğer");
            items.addAll(others);
        }

        notifyDataSetChanged();
    }

    /**
     * Pozisyon string'ini normalize eder: GK, DF, MF, FW veya ham değer.
     */
    private String normalizePosition(String pos) {
        if (pos == null || pos.isEmpty()) return "OTHER";
        String p = pos.toUpperCase().trim();
        if (p.equals("GK") || p.contains("GOAL")) return "GK";
        if (p.equals("DF") || p.contains("DEFEND") || p.equals("CB") ||
                p.equals("LB") || p.equals("RB") || p.equals("WB") ||
                p.equals("LWB") || p.equals("RWB")) return "DF";
        if (p.equals("MF") || p.equals("MID") || p.equals("CM") ||
                p.equals("CDM") || p.equals("CAM") || p.equals("DM") ||
                p.equals("AM") || p.contains("MIDFIELD")) return "MF";
        if (p.equals("FW") || p.contains("FORWARD") || p.equals("ST") ||
                p.equals("CF") || p.equals("LW") || p.equals("RW") ||
                p.equals("SS")) return "FW";
        return "OTHER";
    }

    /**
     * Pozisyon kodunu Türkçe etikete çevirir.
     */
    private String getPositionLabel(String pos) {
        switch (pos) {
            case "GK": return "Kaleciler";
            case "DF": return "Defanslar";
            case "MF": return "Orta Sahalar";
            case "FW": return "Forvetler";
            default:   return "Diğer";
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_PLAYER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_player_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_player, parent, false);
            return new PlayerViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else if (holder instanceof PlayerViewHolder) {
            ((PlayerViewHolder) holder).bind((PlayerModel) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ===== ViewHolder: Başlık =====

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvHeader;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvPositionHeader);
        }

        void bind(String label) {
            tvHeader.setText(label);
        }
    }

    // ===== ViewHolder: Oyuncu =====

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNumber;
        private final ShapeableImageView ivPlayerPhoto;
        private final TextView tvPlayerName;
        private final TextView tvPlayerPosition;
        private final TextView tvPlayerDetails;

        PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvNumber);
            ivPlayerPhoto = itemView.findViewById(R.id.ivPlayerPhoto);
            tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
            tvPlayerPosition = itemView.findViewById(R.id.tvPlayerPosition);
            tvPlayerDetails = itemView.findViewById(R.id.tvPlayerDetails);
        }

        void bind(PlayerModel player) {
            // Forma numarası
            if (player.getNumber() > 0) {
                tvNumber.setText("#" + player.getNumber());
            } else {
                tvNumber.setText("-");
            }

            // Fotoğraf
            if (player.getPhoto() != null && !player.getPhoto().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(player.getPhoto())
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivPlayerPhoto);
            } else {
                ivPlayerPhoto.setImageResource(R.drawable.ic_person);
            }

            // İsim
            tvPlayerName.setText(player.getName() != null ? player.getName() : "");

            // Pozisyon
            String pos = player.getPosition();
            tvPlayerPosition.setText(pos != null ? pos : "");

            // Detay: ülke · yaş · boy
            StringBuilder details = new StringBuilder();
            if (player.getCountry() != null && !player.getCountry().isEmpty()) {
                details.append(player.getCountry());
            }
            int age = player.getAge();
            if (age > 0) {
                if (details.length() > 0) details.append(" · ");
                details.append(age).append(" yıl");
            }
            String h = player.getHeight();
            if (h != null && !h.isEmpty() && !h.equals("0")) {
                if (details.length() > 0) details.append(" · ");
                details.append(h).append(" cm");
            }
            tvPlayerDetails.setText(details.toString());
        }
    }
}
