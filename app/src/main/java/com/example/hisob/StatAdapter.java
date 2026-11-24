package com.example.hisob;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class StatAdapter extends RecyclerView.Adapter<StatAdapter.StatViewHolder> {
    private List<StatModel> statList;

    public StatAdapter(List<StatModel> statList) {
        this.statList = statList;
    }

    @NonNull
    @Override
    public StatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stat, parent, false);
        return new StatViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull StatViewHolder holder, int position) {
        StatModel stat = statList.get(position);

        int totalAllWins = 0;
        if (statList != null) {
            for (StatModel s : statList) {
                totalAllWins += s.getWins();
            }
        }

        int winRatePercent = 0;
        if (totalAllWins > 0) {
            winRatePercent = (int) Math.round(((double) stat.getWins() / totalAllWins) * 100);
        }

        holder.tvName.setText("Ўйинчи: " + stat.getName());
        holder.tvWinRate.setText(winRatePercent + "% Ғалаба");
        holder.tvGames.setText("Ўйинлар: " + stat.getGamesPlayed());
        holder.tvWins.setText("Ғалаба: " + stat.getWins());
        holder.tvRank.setText("Ўринлар: " + stat.getRankBreakdownStr());
        holder.tv107.setText("Омадли 107: " + stat.getTimes107());
        holder.tv77.setText("Бахтсиз 7 (-77): " + stat.getTimes77());
        holder.tvPoints.setText("Тўпланган баллар: " + stat.getTotalPoints());
    }

    @Override
    public int getItemCount() {
        return statList.size();
    }

    public static class StatViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvWinRate, tvGames, tvWins, tvRank, tv107, tv77, tvPoints;

        public StatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStatName);
            tvWinRate = itemView.findViewById(R.id.tvWinRate);
            tvGames = itemView.findViewById(R.id.tvStatGames);
            tvWins = itemView.findViewById(R.id.tvStatWins);
            tvRank = itemView.findViewById(R.id.tvStatRank);
            tv107 = itemView.findViewById(R.id.tvStat107);
            tv77 = itemView.findViewById(R.id.tvStat77);
            tvPoints = itemView.findViewById(R.id.tvStatPoints);
        }
    }
}
