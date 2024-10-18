package com.example.namapopup;

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

import java.util.List;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.FlashcardViewHolder> {

    private List<GlobalVariable.HougenInformation> flashcards;
    private Context context;
    public GlobalVariable.HougenInformation currentFlashcard;

    public FlashcardAdapter(List<GlobalVariable.HougenInformation> flashcards, Context context) {
        this.flashcards = flashcards;
        this.context = context;
    }

    @NonNull
    @Override
    public FlashcardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.flashcard_item, parent, false);
        return new FlashcardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlashcardViewHolder holder, int position) {
        currentFlashcard = flashcards.get(position);
        GlobalVariable.HougenInformation flashcard = flashcards.get(position); // [i]
        holder.titleHougen.setText(flashcard.hougen);
        holder.titleChihou.setText(flashcard.chihou);
        holder.titleDef.setText("意味: " + flashcard.def);
    }

    @Override
    public int getItemCount() {
        return flashcards.size();
    }

    public class FlashcardViewHolder extends RecyclerView.ViewHolder {
        TextView titleHougen;
        TextView titleChihou;
        TextView titleDef;

        public FlashcardViewHolder(@NonNull View itemView) {
            super(itemView);
            LinearLayout flashcardField = itemView.findViewById(R.id.flashcard_field);
            titleHougen = itemView.findViewById(R.id.tvHougen);
            titleChihou = itemView.findViewById(R.id.tvChihou);
            titleDef = itemView.findViewById(R.id.tvDef);
            currentFlashcard = null;

            // Set click listener on the CardView
            flashcardField.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("haha", "onClick: ");
                    if (titleChihou.getVisibility() == View.GONE) {
                        titleChihou.setVisibility(View.VISIBLE);
                        titleDef.setVisibility(View.VISIBLE);
                    } else {
                        titleChihou.setVisibility(View.GONE);
                        titleDef.setVisibility(View.GONE);
                    }
                }
            });

            // Set long click listener on the CardView
            flashcardField.setOnLongClickListener(new View.OnLongClickListener() {
                GlobalVariable.HougenInformation flashcard = currentFlashcard;
                @Override
                public boolean onLongClick(View view) {
                    Log.d("haha", "onLongClick: ");
                    launchShousaiActivity(currentFlashcard);
                    // Return true to indicate the event was handled
                    return true;
                }
            });
        }
    }

    private void launchShousaiActivity(GlobalVariable.HougenInformation hougenInformation) {
        Log.d("launchShousaiActivity", "lauched ShousaiActivity");
        Intent intent = new Intent(this.context, HougenInfoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("hougen", hougenInformation.hougen);
        intent.putExtra("chihou", hougenInformation.chihou);
        intent.putExtra("pref", hougenInformation.pref);
        intent.putExtra("area", hougenInformation.area);
        intent.putExtra("def", hougenInformation.def);
        intent.putExtra("example", hougenInformation.example);
        intent.putExtra("pos", hougenInformation.pos);
        context.startService(intent);
    }
}

