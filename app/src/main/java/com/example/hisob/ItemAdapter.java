package com.example.hisob;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
    private DbSQL dbSQL;
    private FirebaseManager fbManager;
    private List<ItemModel> itemList;
    private Context context;
    private String dealerName = "";

    public ItemAdapter(Context context, List<ItemModel> itemList) {
        this.context = context;
        this.itemList = itemList;
        this.dbSQL = new DbSQL(context);
        this.fbManager = FirebaseManager.getInstance(context);
    }

    public void setDealerName(String dealerName) {
        this.dealerName = dealerName != null ? dealerName : "";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        return new ItemViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ItemModel item = itemList.get(position);

        if (holder.currentWatcher != null) {
            holder.etNumber.removeTextChangedListener(holder.currentWatcher);
        }

        holder.etNumber.setVisibility(View.VISIBLE);

        if (item.getName().equals(dealerName)) {
            holder.tvName.setText("🎴 " + item.getName());
        } else {
            holder.tvName.setText(item.getName());
        }

        holder.etNumber.setText("");

        int amount = 0;
        try {
            amount = Integer.parseInt(item.getAmount());
        } catch (Exception ignored) {}

        String amountPlusStr = item.getAmountplus();
        if (amountPlusStr == null || amountPlusStr.isEmpty()) {
            amountPlusStr = "0";
        }
        holder.tvHistory.setText("Тарих: " + amountPlusStr);

        boolean is77Out = amountPlusStr.contains("(7)");
        String teamCode = fbManager.getCurrentTeamCode();

        // Styling based on score rules
        if (is77Out) {
            holder.etNumber.setVisibility(View.GONE);
            holder.tvBadge.setText("Кетди! (7)");
            holder.tvBadge.setBackgroundColor(Color.parseColor("#FFEBEE"));
            holder.tvBadge.setTextColor(Color.parseColor("#C62828"));
        } else if (amount == 107) {
            holder.tvBadge.setText("Омадли 107! (0 Ҳисоб)");
            holder.tvBadge.setBackgroundColor(Color.parseColor("#FFF8E1"));
            holder.tvBadge.setTextColor(Color.parseColor("#F57F17"));

            if (!teamCode.isEmpty()) {
                fbManager.resetPlayerScoreInFirebase(teamCode, item.getName());
            } else {
                dbSQL.updateNull(String.valueOf(item.getId()), "0", "0");
            }
        } else if (amount >= 108) {
            holder.etNumber.setVisibility(View.GONE);
            holder.tvBadge.setText("Кетди! (" + amount + ")");
            holder.tvBadge.setBackgroundColor(Color.parseColor("#FFEBEE"));
            holder.tvBadge.setTextColor(Color.parseColor("#C62828"));
        } else if (amount >= 100) {
            holder.tvBadge.setText(amount + " ҳисоб (Хавфли!)");
            holder.tvBadge.setBackgroundColor(Color.parseColor("#FFEBEE"));
            holder.tvBadge.setTextColor(Color.parseColor("#D32F2F"));
        } else if (amount <= 40) {
            holder.tvBadge.setText(amount + " ҳисоб");
            holder.tvBadge.setBackgroundColor(Color.parseColor("#E3F2FD"));
            holder.tvBadge.setTextColor(Color.parseColor("#1976D2"));
        } else {
            holder.tvBadge.setText(amount + " ҳисоб");
            holder.tvBadge.setBackgroundColor(Color.parseColor("#E8F5E9"));
            holder.tvBadge.setTextColor(Color.parseColor("#2E7D32"));
        }

        // Filter numbers (max 5 chars)
        holder.etNumber.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(5),
                (source, start, end, dest, dstart, dend) -> {
                    if (source.toString().matches("^-?\\d*$")) {
                        return null;
                    }
                    return "";
                }
        });

        // Input listener
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String numberStr = s.toString().trim();
                if (!numberStr.isEmpty() && isNumeric(numberStr)) {
                    int inputNumber = Integer.parseInt(numberStr);
                    item.setTemporaryAmount(inputNumber);

                    if (inputNumber == -77) {
                        holder.tvBadge.setText("Сизга ўйин тугади! (7)");
                        holder.tvBadge.setBackgroundColor(Color.parseColor("#FFEBEE"));
                        holder.tvBadge.setTextColor(Color.parseColor("#C62828"));
                    }
                } else {
                    item.setTemporaryAmount(0);
                }
            }
        };

        holder.currentWatcher = watcher;
        holder.etNumber.addTextChangedListener(watcher);

        // Delete single player
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Ўчириш")
                    .setMessage(item.getName() + " ни ўйиндан ўчиришга ишончингиз комилми?")
                    .setPositiveButton("Ҳа", (dialog, which) -> {
                        if (!teamCode.isEmpty()) {
                            fbManager.removePlayerFromGame(teamCode, item.getName());
                        } else {
                            dbSQL.deleteSelect(String.valueOf(item.getId()));
                            Refresh(dbSQL.readCourses());
                        }
                    })
                    .setNegativeButton("Йўқ", null)
                    .show();
        });
    }

    public void clearAllInputs() {
        for (ItemModel item : itemList) {
            item.setTemporaryAmount(0);
        }
        notifyDataSetChanged();
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void Refresh(ArrayList<ItemModel> events) {
        itemList.clear();
        itemList.addAll(events);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public Map<Integer, Integer> collectAllNumbers() {
        Map<Integer, Integer> numberMap = new HashMap<>();
        for (ItemModel item : itemList) {
            numberMap.put(item.getId(), item.getTemporaryAmount());
        }
        return numberMap;
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBadge, tvHistory;
        EditText etNumber;
        ImageView btnDelete;
        TextWatcher currentWatcher;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.item_text);
            tvBadge = itemView.findViewById(R.id.item_text_itog);
            tvHistory = itemView.findViewById(R.id.item_text_history);
            etNumber = itemView.findViewById(R.id.etNumber);
            btnDelete = itemView.findViewById(R.id.btn_delete_item);
        }
    }
}
