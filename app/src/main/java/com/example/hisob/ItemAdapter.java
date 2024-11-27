package com.example.hisob;

import android.annotation.SuppressLint;
import android.content.Context;

import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Бунинг ёрдамида ҳар бир `etNumber` қийматини йиғамиз
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
    DbSQL dbSQL;
    private List<ItemModel> itemList;
    private Context context;

    // Интерфейс орқали маълумотни топшириш
    public interface OnSubmitClickListener {
        void onSubmitClicked(Map<Integer, Integer> numberMap);
    }

    private OnSubmitClickListener listener;

    public void setOnSubmitClickListener(OnSubmitClickListener listener) {
        this.listener = listener;
    }

    public ItemAdapter(Context context, List<ItemModel> itemList) {
        this.context = context;
        this.itemList = itemList;
        dbSQL = new DbSQL(context);
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        return new ItemViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ItemModel item = itemList.get(position);
        String inputText = holder.etNumber.getText().toString().trim();

        holder.etNumber.setVisibility(View.VISIBLE);
        holder.item_text_itog.setTextColor(Color.BLACK);
        holder.textView.setText(item.getName());
        holder.item_text_itog.setText(String.valueOf(item.getAmountplus()));
        holder.etNumber.setText("");

        int amount = Integer.parseInt(item.getAmount());

        if (amount <= 0) {
            holder.item_text_itog.setTextColor(Color.BLUE);
        } else if ((amount == 100) || (amount == 101) || (amount == 102) || (amount == 103) || (amount == 104) || (amount == 105 || (amount == 106))) {
            holder.item_text_itog.setTextColor(Color.RED);
        } else if (amount == 107) {
            holder.item_text_itog.setTextColor(Color.BLUE);
            dbSQL.updateNull(String.valueOf(item.getId()), "0", "0");
            holder.item_text_itog.setText(item.getName() + "жон сизда омадли 107, сизда хозир 0");
        } else if (amount >= 108) {
            holder.etNumber.setVisibility(View.GONE);
            holder.item_text_itog.setTextColor(Color.RED);
            holder.item_text_itog.setText(item.getName() + "жон сиз учдингиз. Ҳисобингиз " + item.getAmount());
        } else {
            holder.item_text_itog.setTextColor(Color.BLACK);
        }

        // Ҳар бир EditText учун киритилган қийматни оламиз
        holder.etNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String numberStr = s.toString().trim();
                if (!numberStr.isEmpty() && isNumeric(numberStr)) {
                    item.setTemporaryAmount(Integer.parseInt(numberStr));
                } else {
                    item.setTemporaryAmount(0); // Нотўғри ёки бўш қиймат учун 0 сақланади
                }
                if (!numberStr.isEmpty() && isNumeric(numberStr)) {
                    int inputNumber = Integer.parseInt(numberStr);
                    if (inputNumber == -77) {
                        holder.etNumber.setVisibility(View.GONE);
                        holder.item_text_itog.setTextColor(Color.RED);
                        holder.item_text_itog.setText(item.getName() + "жон сиз учдингиз қўлингизда бахтсиз 7");
                    }
                }

            }

        });

        if (!inputText.isEmpty()) {
            try {
                if (inputText.equals("-77")) {
                    holder.etNumber.setVisibility(View.GONE);
                    holder.item_text_itog.setTextColor(Color.RED);
                    holder.item_text_itog.setText(item.getName() + "жон сиз учдингиз қўлизда бахтсиз 7");
                } else {
                }
            } catch (NumberFormatException e) {
            }
        } else {
            holder.item_text_itog.setTextColor(Color.BLACK);
        }

        // Ўчириш функцияси қолади
        holder.textView.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Ўчириш")
                    .setMessage(item.getName() + " маълумотини ўчиришга ишончингиз комилми?")
                    .setPositiveButton("Ҳа", (dialog, which) -> {
                        dbSQL.deleteSelect(String.valueOf(item.getId()));
                        Refresh(dbSQL.readCourses());
                    })
                    .setNegativeButton("Йўқ", null)
                    .show();
        });

        holder.etNumber.setFilters(new InputFilter[]{
                (source, start, end, dest, dstart, dend) -> {
                    // Фақат манфий ёки рақамлар қабул қилинади
                    if (source.toString().matches("^-?\\d*$")) {
                        return null; // Қабул қилинади
                    }
                    return ""; // Қабул қилинмайди
                }
        });


    }

    // Ёрдамчи метод: Қиймат тўғри ёзилганини текшириш
    private boolean isValidNumber(String str) {
        if (str.equals("-")) {
            return false; // Фақат "минус" киритилган бўлса, нотўғри деб қаралади
        }

        try {
            Integer.parseInt(str); // Мусбат ёки манфий сонни текшириш
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Барча EditText'ларни тозалаш
    public void clearAllInputs() {
        for (ItemModel item : itemList) {
            item.setTemporaryAmount(0);

        }
        notifyDataSetChanged();
    }

    // Ёрдамчи метод: Сон ёки йўқлигини текшириш
    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    void Refresh(ArrayList<ItemModel> events) {
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
        TextView textView, item_text_itog;
        EditText etNumber;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.item_text);
            item_text_itog = itemView.findViewById(R.id.item_text_itog);
            etNumber = itemView.findViewById(R.id.etNumber);
        }
    }
}

