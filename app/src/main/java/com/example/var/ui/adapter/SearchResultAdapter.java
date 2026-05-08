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
 * SearchResultAdapter - Arama sonuçlarını listeleyen adapter.
 * Takım ve lig arama sonuçlarını gösterir.
 *
 * Her sonuç bir ikon, ad ve tür bilgisi içerir.
 * Sonuca tıklandığında OnSearchResultClickListener callback'i tetiklenir.
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder> {

    /** Arama sonuçları listesi */
    private final List<SearchResultItem> results;

    /** Sonuç tıklama callback'i */
    private final OnSearchResultClickListener listener;

    /**
     * Arama sonucuna tıklandığında tetiklenen callback arayüzü.
     */
    public interface OnSearchResultClickListener {
        /**
         * Bir arama sonucuna tıklandığında çağrılır.
         * @param item Tıklanan arama sonucu
         */
        void onSearchResultClick(SearchResultItem item);
    }

    /**
     * Constructor.
     * @param listener Sonuç tıklama callback'i
     */
    public SearchResultAdapter(OnSearchResultClickListener listener) {
        this.results = new ArrayList<>();
        this.listener = listener;
    }

    /**
     * Arama sonuçlarını günceller.
     * @param newResults Yeni sonuç listesi
     */
    public void setResults(List<SearchResultItem> newResults) {
        results.clear();
        if (newResults != null) {
            results.addAll(newResults);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        holder.bind(results.get(position));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    /**
     * SearchResultViewHolder - Arama sonucu görünüm tutucu sınıfı.
     */
    class SearchResultViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvSearchIcon;
        private final TextView tvSearchResultName;
        private final TextView tvSearchResultType;

        SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSearchIcon = itemView.findViewById(R.id.tvSearchIcon);
            tvSearchResultName = itemView.findViewById(R.id.tvSearchResultName);
            tvSearchResultType = itemView.findViewById(R.id.tvSearchResultType);
        }

        /**
         * Arama sonucu verisini görünüme bağlar.
         * @param item Arama sonucu modeli
         */
        void bind(SearchResultItem item) {
            tvSearchIcon.setText(item.icon);
            tvSearchResultName.setText(item.name);
            tvSearchResultType.setText(item.type);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSearchResultClick(item);
                }
            });
        }
    }

    /**
     * SearchResultItem - Arama sonucu veri modeli.
     * Adapter'da gösterilecek bilgileri tutar.
     */
    public static class SearchResultItem {
        /** İkon (emoji: ⚽ takım, 🏆 lig) */
        public final String icon;
        /** Sonuç adı */
        public final String name;
        /** Sonuç türü (Takım veya Lig) */
        public final String type;
        /** Benzersiz kimlik (matchId, leagueId veya teamId) */
        public final String id;
        /** Sonuç kategorisi (team veya league) */
        public final String category;
        /** Takımlar için lig ID'si */
        public final String leagueId;

        public SearchResultItem(String icon, String name, String type, String id, String category) {
            this(icon, name, type, id, category, null);
        }

        public SearchResultItem(String icon, String name, String type, String id, String category, String leagueId) {
            this.icon = icon;
            this.name = name;
            this.type = type;
            this.id = id;
            this.category = category;
            this.leagueId = leagueId;
        }
    }
}
