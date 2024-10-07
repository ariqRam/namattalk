package com.example.namapopup;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private List<GlobalVariable.HougenInformation> results;

    public SearchResultAdapter() {
        this.results = new ArrayList<>();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final TextView meaningView;
        private final TextView regionView;
        private final LinearLayout searchItemLayout;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.result_text);
            meaningView = view.findViewById(R.id.meaning_text);
            regionView = view.findViewById(R.id.region_text);
            searchItemLayout = view.findViewById(R.id.search_result_item);
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

        public LinearLayout getSearchItemLayout() { return searchItemLayout; }
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
        GlobalVariable.HougenInformation hougenInformation = results.get(position);
        holder.getTextView().setText(hougenInformation.hougen + ": ");
        holder.getMeaningView().setText(hougenInformation.def);
        holder.getRegionView().setText(hougenInformation.chihou);
        View.OnClickListener resultItemOnClickHandler = v -> {
            launchDictinfoActivity(v.getContext(), hougenInformation);
        };
        holder.getSearchItemLayout().setOnClickListener(resultItemOnClickHandler);
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public void updateResults(List<GlobalVariable.HougenInformation> newResults) {
        this.results = newResults;
        notifyDataSetChanged();
    }

    private void launchDictinfoActivity(Context context, GlobalVariable.HougenInformation hougenInformation) {
        Log.d("launchDictinfoActivity", "launched dictionary info activity");
        Intent intent = new Intent(context, DictionaryInfoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("hougen", hougenInformation.hougen);
        intent.putExtra("chihou", hougenInformation.chihou);
        intent.putExtra("pref", hougenInformation.pref);
        intent.putExtra("area", hougenInformation.area);
        intent.putExtra("def", hougenInformation.def);
        intent.putExtra("pos", hougenInformation.pos);
        intent.putExtra("example", hougenInformation.example);
        startActivity(context, intent, null);
    }

}