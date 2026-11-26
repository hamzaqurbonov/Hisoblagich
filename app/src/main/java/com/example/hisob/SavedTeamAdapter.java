package com.example.hisob;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SavedTeamAdapter extends RecyclerView.Adapter<SavedTeamAdapter.SavedTeamViewHolder> {
    private List<FirebaseManager.SavedTeamItem> teamList;

    public interface OnSavedTeamActionListener {
        void onSelect(String teamCode, String teamName);
        void onRemove(String teamCode);
    }

    private OnSavedTeamActionListener listener;

    public SavedTeamAdapter(List<FirebaseManager.SavedTeamItem> teamList, OnSavedTeamActionListener listener) {
        this.teamList = teamList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SavedTeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_team, parent, false);
        return new SavedTeamViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull SavedTeamViewHolder holder, int position) {
        FirebaseManager.SavedTeamItem item = teamList.get(position);
        holder.tvName.setText(item.name);
        holder.tvCode.setText("Код: " + item.code);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSelect(item.code, item.name);
        });

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) listener.onRemove(item.code);
        });
    }

    @Override
    public int getItemCount() {
        return teamList != null ? teamList.size() : 0;
    }

    public static class SavedTeamViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCode;
        ImageView btnRemove;

        public SavedTeamViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSavedTeamName);
            tvCode = itemView.findViewById(R.id.tvSavedTeamCode);
            btnRemove = itemView.findViewById(R.id.btnRemoveSavedTeam);
        }
    }
}
