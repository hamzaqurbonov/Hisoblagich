package com.example.hisob;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerSelectAdapter extends RecyclerView.Adapter<PlayerSelectAdapter.PlayerSelectViewHolder> {
    private Context context;
    private List<String> playerList;
    private Set<String> selectedPlayers = new HashSet<>();
    private boolean isTrashMode = false;

    public interface OnPlayerActionListener {
        void onDelete(String playerName);
        void onRestore(String playerName);
    }

    private OnPlayerActionListener listener;

    public PlayerSelectAdapter(Context context, List<String> playerList, boolean isTrashMode, OnPlayerActionListener listener) {
        this.context = context;
        this.playerList = playerList;
        this.isTrashMode = isTrashMode;
        this.listener = listener;
        if (!isTrashMode && playerList != null) {
            this.selectedPlayers.addAll(playerList);
        }
    }

    public void updateList(List<String> newList, boolean isTrashMode) {
        this.playerList = newList;
        this.isTrashMode = isTrashMode;
        if (!isTrashMode && newList != null) {
            this.selectedPlayers.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public Set<String> getSelectedPlayers() {
        return selectedPlayers;
    }

    @NonNull
    @Override
    public PlayerSelectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player_select, parent, false);
        return new PlayerSelectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerSelectViewHolder holder, int position) {
        String name = playerList.get(position);
        holder.tvName.setText(name);

        RelativeLayout.LayoutParams nameParams = (RelativeLayout.LayoutParams) holder.tvName.getLayoutParams();

        if (isTrashMode) {
            holder.cbSelect.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnRestore.setVisibility(View.VISIBLE);

            nameParams.removeRule(RelativeLayout.END_OF);
            nameParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
        } else {
            holder.cbSelect.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnRestore.setVisibility(View.GONE);

            nameParams.removeRule(RelativeLayout.ALIGN_PARENT_START);
            nameParams.addRule(RelativeLayout.END_OF, R.id.cbSelectPlayer);

            holder.cbSelect.setOnCheckedChangeListener(null);
            holder.cbSelect.setChecked(selectedPlayers.contains(name));

            holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedPlayers.add(name);
                } else {
                    selectedPlayers.remove(name);
                }
            });
        }
        holder.tvName.setLayoutParams(nameParams);

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("🗑️ Корзинкага олиш")
                    .setMessage(name + " ни корзинкага олишга ишончингиз комилми?\n(Эслатма: Статистикаси ўчиб кетмайди)")
                    .setPositiveButton("Ҳа", (dialog, which) -> {
                        selectedPlayers.remove(name);
                        if (listener != null) listener.onDelete(name);
                    })
                    .setNegativeButton("Йўқ", null)
                    .show();
        });

        holder.btnRestore.setOnClickListener(v -> {
            if (listener != null) listener.onRestore(name);
        });
    }

    @Override
    public int getItemCount() {
        return playerList != null ? playerList.size() : 0;
    }

    public static class PlayerSelectViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvName;
        ImageView btnDelete, btnRestore;

        public PlayerSelectViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cbSelectPlayer);
            tvName = itemView.findViewById(R.id.tvPlayerName);
            btnDelete = itemView.findViewById(R.id.btnDeletePlayer);
            btnRestore = itemView.findViewById(R.id.btnRestorePlayer);
        }
    }
}
