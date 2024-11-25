package com.example.hisob;

import static java.lang.String.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
    private Context context;
    private int currentSum, newSum;
    private List<ItemModel> itemList;

    // Ҳар бир фойдаланувчи учун қўшилган рақамлар рўйхати
    private Map<String, List<Integer>> userNumbersMap = new HashMap<>();

    public ItemAdapter(List<ItemModel> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;

        // Ҳар бир фойдаланувчи учун бошланғич рўйхат
        for (ItemModel item : itemList) {
            userNumbersMap.put(item.getName(), new ArrayList<>());
        }
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ItemModel item = itemList.get(position);

       holder.textView.setText(item.getName());

        holder.item_text_itog.setText(valueOf(item.getAmount()));
       Log.d("demo1", "adapter " + item.getAmount());


        SharedPreferences preferences = context.getSharedPreferences("UserSums", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        // Рақамни киритиш
        holder.btn_num.setOnClickListener(v -> {
            String numberStr = holder.etNumber.getText().toString().trim();

            if(numberStr.equals("-") || numberStr.equals("--") || numberStr.equals("/") || numberStr.equals("//") || numberStr.equals(".") || numberStr.equals("..")) {
                Log.d("demo1", "sss: ");
                Toast.makeText(v.getContext(), "Рақам киритинг", Toast.LENGTH_SHORT).show();
                return;
            }

            if(numberStr.equals("7.")) {
                holder.item_text_itog.setText(item.getName() + "жон, сиз учдингиз: ");
                holder.etNumber.setVisibility(View.GONE);
                holder.etNumber.setText("");
                return;
            }

            if (!numberStr.isEmpty() ) {
                int number = Integer.parseInt(numberStr);

                // Жорий йиғиндиси
                currentSum = preferences.getInt(item.getName(), 0);
                newSum = currentSum + number;

                // Янгиланган йиғиндисини сақлаш
                editor.putInt(item.getName(), newSum);
                editor.apply();

                List<Integer> numbers = userNumbersMap.get(item.getName());
                if (numbers == null) {
                    // Агар рўйхат мавжуд бўлмаса, янгисини яратиш
                    numbers = new ArrayList<>();
                    userNumbersMap.put(item.getName(), numbers);
                }
                numbers.add(number);

                // Қўшилган рақамларни кўрсатиш
                StringBuilder numberList = new StringBuilder();
                for (int num : numbers) {
                    numberList.append(num).append("+");
                }

                // Охирги "+" белгисини олиб ташлаш
                if (numberList.length() > 0) {
                    numberList.setLength(numberList.length() - 1);
                }




                // Хусусий ҳолатлар учун хабар кўрсатиш
                if (newSum >= 108) {
                    Log.d("demo1", "Сиз учдингиз: " + newSum);
                    holder.item_text_itog.setText(item.getName() + "жон, сиз учдингиз: " + numberList + "=" + newSum);
                    holder.etNumber.setVisibility(View.GONE);
                } else if (newSum == 107) {
                    editor.putInt(item.getName(), 0);
                    editor.apply();
                    holder.item_text_itog.setText(item.getName() + "жон, омадлисиз! Сизда 0");
                } else {
                    holder.item_text_itog.setText(numberList + "=" + newSum);
                }
//                itemList.add(new ItemModel(item.getName(), newSum));
                // Киритилган рақамни тозалаш
                holder.etNumber.setText("");
            }
        });

        // Фойдаланувчини ўчириш
        holder.textView.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Ўчириш")
                    .setMessage(item.getName() + " маълумотини ўчиришга ишончингиз комилми?")
                    .setPositiveButton("Ҳа", (dialog, which) -> {
                        itemList.remove(position);
                        userNumbersMap.remove(item.getName()); // Рақамлар рўйхатини ўчириш
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, itemList.size());
                    })
                    .setNegativeButton("Йўқ", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        TextView textView, item_text_itog, item_text_plus;
        Button btn_num;
        EditText etNumber;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            btn_num = itemView.findViewById(R.id.btn_num);
            textView = itemView.findViewById(R.id.item_text);
            etNumber = itemView.findViewById(R.id.etNumber);
            item_text_itog = itemView.findViewById(R.id.item_text_itog);
//            item_text_plus = itemView.findViewById(R.id.item_text_plus);
        }
    }
}
