package com.example.namapopup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.FlashcardViewHolder> {

    private List<GlobalVariable.HougenInformation> Flashcards;

    public FlashcardAdapter(List<GlobalVariable.HougenInformation> Flashcards) {
        this.Flashcards = Flashcards;
    }

    @NonNull
    @Override
    public FlashcardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.flashcard_item, parent, false);
        return new FlashcardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlashcardViewHolder holder, int position) {
        GlobalVariable.HougenInformation flashcard = Flashcards.get(position);
        holder.titleHougen.setText(flashcard.hougen);
        holder.titleChihou.setText(flashcard.chihou);
        holder.titleDef.setText("意味: " + flashcard.def);
    }

    @Override
    public int getItemCount() {
        return Flashcards.size();
    }

    public static class FlashcardViewHolder extends RecyclerView.ViewHolder {
        TextView titleHougen;
        TextView titleChihou;
        TextView titleDef;

        public FlashcardViewHolder(@NonNull View itemView) {
            super(itemView);
            CardView flashcardItem = itemView.findViewById(R.id.flashcard_item);
            titleHougen = itemView.findViewById(R.id.tvHougen);
            titleChihou = itemView.findViewById(R.id.tvChihou);
            titleDef = itemView.findViewById(R.id.tvDef);

            // Set click listener on the CardView
            flashcardItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (titleChihou.getVisibility() == View.GONE) {
                        titleChihou.setVisibility(View.VISIBLE);
                        titleDef.setVisibility(View.VISIBLE);
                    } else {
                        titleChihou.setVisibility(View.GONE);
                        titleDef.setVisibility(View.GONE);
                    }
                }
            });
        }
    }
}

