package com.example.hisob;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {
    DbSQL dbSQL;
    Spinner spinner, spinner_young;
    private EditText etNumber, edit_add_name;
    private Button btnAdd, btnClear, btnname, btn_num;
    private TextView tvResult;
    ImageView delete_all;

    // Фойдаланувчи исмлари учун массив
//    private String[] users = {"Акмал", "Азиз", "Бекзод", "Хамза", "Иномжон"};

    ArrayList<ItemModel> list = new ArrayList<>();
    private int currentUserIndex = 0; // Жорий фойдаланувчи индекси
    int currentSum, newSum;
    RecyclerView item_recycler;
    ItemAdapter adapter;

    String emploee;
//    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbSQL = new DbSQL(this);
        etNumber = findViewById(R.id.etNumber);
        item_recycler = findViewById(R.id.item_recycler);
        spinner_young = findViewById(R.id.spinner_young);
        btnClear = findViewById(R.id.btnClear);
        btnname = findViewById(R.id.btnname);

        delete_all = findViewById(R.id.delete_all);

        // SharedPreferences инициализацияси
        SharedPreferences preferences = getSharedPreferences("UserSums", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();


        list = dbSQL.readCourses();
        adapter = new ItemAdapter( MainActivity.this, list );
        item_recycler.setLayoutManager(new LinearLayoutManager(this));
        item_recycler.setAdapter(adapter);

        btnClear.setOnClickListener(v -> {
            Map<Integer, Integer> numberMap = adapter.collectAllNumbers();
            for (Map.Entry<Integer, Integer> entry : numberMap.entrySet()) {
                int id = entry.getKey();
                int amount = entry.getValue();

                // Киритилган қийматни `SQL`га жойлаш
                dbSQL.updateCourse(String.valueOf(id), amount);

            }

            // Барча `EditText` қийматини тозалаш
            adapter.clearAllInputs();

            // Маълумотларни янгилаш
            Refresh(dbSQL.readCourses());
        });

        btnname.setOnClickListener(v -> {
            dbSQL.addNewCourse(emploee,"0", "0");
            list = dbSQL.readCourses();
        Log.d("demo1", "activityllist " + preferences.getInt(emploee, 0));
            adapter = new  ItemAdapter( MainActivity.this, list );
            item_recycler.setLayoutManager(new LinearLayoutManager(this));
            item_recycler.setAdapter(adapter);
//            edit_add_name.setText("");
        });



        delete_all.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Ўчириш")
                    .setMessage("Барчасини ўчиришга ишончингиз комилми?")
                    .setPositiveButton("Ҳа", (dialog, which) -> {
                        dbSQL.deleteAllData();
                        Refresh(dbSQL.readCourses());
                    })
                    .setNegativeButton("Йўқ", null)
                    .show();
        });
        spinner_young();


    }

    private void spinner_young() {
        // spinner_young ga tegishli
        String[] spinner_young_list = {"Акмал", "Хамза", "Аббос", "Иномжон", "Бекзод", "Азиз", "Номаълум шахс"};

        CustomSpinnerAdapter adapter_young = new CustomSpinnerAdapter(this, Arrays.asList(spinner_young_list), R.layout.spinner_item_text);
        spinner_young.setAdapter(adapter_young);
//        Log.d("demo46", "activityllist " + activityllist);
        spinner_young.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                // Танланган элементни олиш
                String selectedOption = parentView.getItemAtPosition(position).toString();
                emploee = selectedOption;


                adapter = new  ItemAdapter( MainActivity.this, list );
                item_recycler.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                item_recycler.setAdapter(adapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Хеч нарса танланмаса (ўрнатилган ҳолат)
            }
        });
    }


    void Refresh(ArrayList<ItemModel> events) {
        list.clear();
        list.addAll(events);
        adapter.notifyDataSetChanged();
    }

}