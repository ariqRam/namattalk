package com.example.namapopup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.FlashcardViewHolder> {

    private List<GlobalVariable.Flashcard> Flashcards;

    public FlashcardAdapter(List<GlobalVariable.Flashcard> Flashcards) {
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
        GlobalVariable.Flashcard Flashcard = Flashcards.get(position);
        holder.titleTextView.setText(Flashcard.getTitle());
    }

    @Override
    public int getItemCount() {
        return Flashcards.size();
    }

    public static class FlashcardViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;

        public FlashcardViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tvTitle);
        }
    }
}

