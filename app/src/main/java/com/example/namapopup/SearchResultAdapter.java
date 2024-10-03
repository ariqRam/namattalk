package com.example.namapopup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private List<HashMap<String, String>> results;

    public SearchResultAdapter() {
        this.results = new ArrayList<>();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final TextView meaningView;
        private final TextView regionView;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.result_text);
            meaningView = view.findViewById(R.id.meaning_text);
            regionView = view.findViewById(R.id.region_text);
        }

        public TextView getTextView() {
            return textView;
        }

        public TextView getMeaningView() {
            return meaningView;
        }

        public TextView getRegionView() {
            return regionView;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_result_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.getTextView().setText(results.get(position).get("text") + ": ");
        holder.getMeaningView().setText(results.get(position).get("meaning"));
        holder.getRegionView().setText(results.get(position).get("region"));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public void updateResults(List<HashMap<String,String>> newResults) {
        this.results = newResults;
        notifyDataSetChanged();
    }
}