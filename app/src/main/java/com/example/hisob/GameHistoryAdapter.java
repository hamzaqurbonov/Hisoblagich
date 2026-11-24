package com.example.hisob;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class GameHistoryAdapter extends RecyclerView.Adapter<GameHistoryAdapter.HistoryViewHolder> {
    private List<GameHistoryModel> historyList;

    public GameHistoryAdapter(List<GameHistoryModel> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        GameHistoryModel item = historyList.get(position);
        holder.tvHistDate.setText(item.getDateStr());
        holder.tvHistWinner.setText("Ғолиб: " + item.getWinnerName());
        holder.tvHistDetails.setText(item.getDetails());
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvHistDate, tvHistWinner, tvHistDetails;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistDate = itemView.findViewById(R.id.tvHistDate);
            tvHistWinner = itemView.findViewById(R.id.tvHistWinner);
            tvHistDetails = itemView.findViewById(R.id.tvHistDetails);
        }
    }
}
