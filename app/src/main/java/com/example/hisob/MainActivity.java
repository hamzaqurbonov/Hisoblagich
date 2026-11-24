package com.example.hisob;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    DbSQL dbSQL;
    Spinner spinner_young;
    private Button btnClear, btnname;
    ImageView delete_all;
    TextView tvHeaderTitle;

    ArrayList<ItemModel> list = new ArrayList<>();
    RecyclerView item_recycler, rvGameHistory, rvStatistics;
    ItemAdapter adapter;
    GameHistoryAdapter historyAdapter;
    StatAdapter statAdapter;

    String emploee;

    public static boolean isPlayerOut(ItemModel player) {
        if (player == null) return false;
        int amt = 0;
        try {
            amt = Integer.parseInt(player.getAmount());
        } catch (Exception ignored) {}
        String plus = player.getAmountplus();
        boolean is77 = (plus != null && plus.contains("(7)"));
        return (amt >= 108 || amt <= -400 || is77);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbSQL = new DbSQL(this);

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        item_recycler = findViewById(R.id.item_recycler);
        rvGameHistory = findViewById(R.id.rvGameHistory);
        rvStatistics = findViewById(R.id.rvStatistics);

        spinner_young = findViewById(R.id.spinner_young);
        btnClear = findViewById(R.id.btnClear);
        btnname = findViewById(R.id.btnname);
        delete_all = findViewById(R.id.delete_all);

        // RecyclerView Layout Managers
        item_recycler.setLayoutManager(new LinearLayoutManager(this));
        rvGameHistory.setLayoutManager(new LinearLayoutManager(this));
        rvStatistics.setLayoutManager(new LinearLayoutManager(this));

        list = dbSQL.readCourses();
        adapter = new ItemAdapter(MainActivity.this, list);
        item_recycler.setAdapter(adapter);

        // Bottom Navigation Tabs setup
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        View layoutHisob = findViewById(R.id.layout_hisob);
        View layoutHistory = findViewById(R.id.layout_history);
        View layoutStat = findViewById(R.id.layout_stat);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_hisob) {
                layoutHisob.setVisibility(View.VISIBLE);
                layoutHistory.setVisibility(View.GONE);
                layoutStat.setVisibility(View.GONE);
                tvHeaderTitle.setText("Карта 108");
                return true;
            } else if (itemId == R.id.nav_history) {
                layoutHisob.setVisibility(View.GONE);
                layoutHistory.setVisibility(View.VISIBLE);
                layoutStat.setVisibility(View.GONE);
                tvHeaderTitle.setText("Ўйинлар Тарихи");

                ArrayList<GameHistoryModel> historyList = dbSQL.getGameHistory();
                historyAdapter = new GameHistoryAdapter(historyList);
                rvGameHistory.setAdapter(historyAdapter);
                return true;
            } else if (itemId == R.id.nav_stat) {
                layoutHisob.setVisibility(View.GONE);
                layoutHistory.setVisibility(View.GONE);
                layoutStat.setVisibility(View.VISIBLE);
                tvHeaderTitle.setText("Ўйинчилар Статистикаси");

                ArrayList<StatModel> statList = dbSQL.getPlayerStats();
                statAdapter = new StatAdapter(statList);
                rvStatistics.setAdapter(statAdapter);
                return true;
            }
            return false;
        });

        btnClear.setOnClickListener(v -> {
            // Track active players before score update
            ArrayList<ItemModel> beforeList = dbSQL.readCourses();

            Map<Integer, Integer> numberMap = adapter.collectAllNumbers();
            for (Map.Entry<Integer, Integer> entry : numberMap.entrySet()) {
                int id = entry.getKey();
                int amount = entry.getValue();

                if (amount != 0) {
                    dbSQL.updateCourse(String.valueOf(id), amount);
                }
            }

            adapter.clearAllInputs();
            ArrayList<ItemModel> updatedList = dbSQL.readCourses();

            // Track newly eliminated players in this turn
            List<Integer> newlyEliminatedIds = new ArrayList<>();

            for (ItemModel player : updatedList) {
                boolean isNowOut = isPlayerOut(player);
                if (isNowOut) {
                    // Check if player was active before
                    for (ItemModel prev : beforeList) {
                        if (prev.getId() == player.getId()) {
                            boolean wasActive = !isPlayerOut(prev);
                            if (wasActive) {
                                newlyEliminatedIds.add(player.getId());
                            }
                            break;
                        }
                    }
                }
            }

            if (!newlyEliminatedIds.isEmpty()) {
                dbSQL.recordTurnEliminations(newlyEliminatedIds);
            }

            Refresh(updatedList);
            checkWinner(updatedList);
        });

        btnname.setOnClickListener(v -> {
            if (emploee != null && !emploee.isEmpty()) {
                dbSQL.addNewCourse(emploee, "0", "0");
                Refresh(dbSQL.readCourses());
            }
        });

        delete_all.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Янги ўйин")
                    .setMessage("Ўйинни тугатиб, барчасини ўчиришга ишончингиз комилми?\n(Натижалар Статистика ва Тарихга сақланади)")
                    .setPositiveButton("Ҳа", (dialog, which) -> {
                        dbSQL.finishGameAndRecordStats(list);
                        dbSQL.deleteAllData();
                        Refresh(dbSQL.readCourses());
                    })
                    .setNegativeButton("Йўқ", null)
                    .show();
        });

        spinner_young();
    }

    private void checkWinner(ArrayList<ItemModel> currentPlayers) {
        if (currentPlayers == null || currentPlayers.size() <= 1) return;

        ItemModel lastActivePlayer = null;
        int activeCount = 0;
        int minScore = Integer.MAX_VALUE;
        ItemModel lowestScorePlayer = null;

        for (ItemModel player : currentPlayers) {
            int amt = 0;
            try {
                amt = Integer.parseInt(player.getAmount());
            } catch (Exception ignored) {}

            boolean isOut = isPlayerOut(player);
            if (!isOut) {
                activeCount++;
                lastActivePlayer = player;
            }
            if (amt < minScore) {
                minScore = amt;
                lowestScorePlayer = player;
            }
        }

        if (activeCount == 1 && lastActivePlayer != null) {
            String winnerName = lastActivePlayer.getName();
            new AlertDialog.Builder(this)
                    .setTitle("🏆 Ғолиб Аниқланди!")
                    .setMessage("Табриклаймиз! " + winnerName + " энг охирида 108 дан ошмай қолди ва ҒОЛИБ бўлди!\n\nЎйинни тугатиб, статистикага сақлаймизми?")
                    .setCancelable(false)
                    .setPositiveButton("Сақлаш ва Янги ўйин", (dialog, which) -> {
                        dbSQL.finishGameAndRecordStats(currentPlayers);
                        dbSQL.deleteAllData();
                        Refresh(dbSQL.readCourses());
                    })
                    .setNegativeButton("Давом этиш", null)
                    .show();
        } else if (activeCount == 0 && lowestScorePlayer != null) {
            String winnerName = lowestScorePlayer.getName();
            new AlertDialog.Builder(this)
                    .setTitle("🏆 Ўйин Тугади!")
                    .setMessage("Барча ўйинчилар 108 дан ошди. Энг кам балл билан " + winnerName + " ҒОЛИБ бўлди!\n\nЎйинни тугатиб, статистикага сақлаймизми?")
                    .setCancelable(false)
                    .setPositiveButton("Сақлаш ва Янги ўйин", (dialog, which) -> {
                        dbSQL.finishGameAndRecordStats(currentPlayers);
                        dbSQL.deleteAllData();
                        Refresh(dbSQL.readCourses());
                    })
                    .setNegativeButton("Давом этиш", null)
                    .show();
        }
    }

    private void spinner_young() {
        String[] spinner_young_list = {"Акмал", "Хамза", "Аббос", "Иномжон", "Азиз", "Номаълум шахс"};

        CustomSpinnerAdapter adapter_young = new CustomSpinnerAdapter(this, Arrays.asList(spinner_young_list), R.layout.spinner_item_text);
        spinner_young.setAdapter(adapter_young);
        spinner_young.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedOption = parentView.getItemAtPosition(position).toString();
                emploee = selectedOption;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });
    }

    void Refresh(ArrayList<ItemModel> events) {
        list.clear();
        list.addAll(events);
        adapter.notifyDataSetChanged();
    }
}
