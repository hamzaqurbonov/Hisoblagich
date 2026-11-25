package com.example.hisob;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private DbSQL dbSQL;
    private FirebaseManager fbManager;
    private ValueEventListener firebaseGameListener;

    private Spinner spinner_young;
    private Button btnClear, btnname;
    private ImageView delete_all, btnSwitchTeam;
    private TextView tvHeaderTitle, tvTeamInfo;

    private ArrayList<ItemModel> list = new ArrayList<>();
    private RecyclerView item_recycler, rvGameHistory, rvStatistics;
    private ItemAdapter adapter;
    private GameHistoryAdapter historyAdapter;
    private StatAdapter statAdapter;

    private String emploee;

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
        fbManager = FirebaseManager.getInstance(this);

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvTeamInfo = findViewById(R.id.tvTeamInfo);
        btnSwitchTeam = findViewById(R.id.btnSwitchTeam);

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

        adapter = new ItemAdapter(MainActivity.this, list);
        item_recycler.setAdapter(adapter);

        setupBottomNavigation();

        btnSwitchTeam.setOnClickListener(v -> showTeamManagerDialog());

        btnClear.setOnClickListener(v -> processRoundCalculation());

        btnname.setOnClickListener(v -> {
            if (emploee != null && !emploee.isEmpty()) {
                String teamCode = fbManager.getCurrentTeamCode();
                if (!teamCode.isEmpty()) {
                    fbManager.addPlayerToGame(teamCode, emploee);
                } else {
                    dbSQL.addNewCourse(emploee, "0", "0");
                    Refresh(dbSQL.readCourses());
                }
            }
        });

        delete_all.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Янги ўйин")
                    .setMessage("Ўйинни тугатиб, барчасини ўчиришга ишончингиз комилми?\n(Натижалар Статистика ва Тарихга сақланади)")
                    .setPositiveButton("Ҳа", (dialog, which) -> {
                        String teamCode = fbManager.getCurrentTeamCode();
                        if (!teamCode.isEmpty()) {
                            fbManager.clearActiveGameInFirebase(teamCode);
                        } else {
                            dbSQL.finishGameAndRecordStats(list);
                            dbSQL.deleteAllData();
                            Refresh(dbSQL.readCourses());
                        }
                    })
                    .setNegativeButton("Йўқ", null)
                    .show();
        });

        spinner_young();
        loadActiveTeamState();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        View layoutHisob = findViewById(R.id.layout_hisob);
        View layoutHistory = findViewById(R.id.layout_history);
        View layoutStat = findViewById(R.id.layout_stat);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            String teamCode = fbManager.getCurrentTeamCode();

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

                if (!teamCode.isEmpty()) {
                    fbManager.listenToTeamHistory(teamCode, historyList -> {
                        historyAdapter = new GameHistoryAdapter(historyList);
                        rvGameHistory.setAdapter(historyAdapter);
                    });
                } else {
                    ArrayList<GameHistoryModel> historyList = dbSQL.getGameHistory();
                    historyAdapter = new GameHistoryAdapter(historyList);
                    rvGameHistory.setAdapter(historyAdapter);
                }
                return true;
            } else if (itemId == R.id.nav_stat) {
                layoutHisob.setVisibility(View.GONE);
                layoutHistory.setVisibility(View.GONE);
                layoutStat.setVisibility(View.VISIBLE);
                tvHeaderTitle.setText("Ўйинчилар Статистикаси");

                if (!teamCode.isEmpty()) {
                    fbManager.listenToTeamStats(teamCode, statList -> {
                        statAdapter = new StatAdapter(statList);
                        rvStatistics.setAdapter(statAdapter);
                    });
                } else {
                    ArrayList<StatModel> statList = dbSQL.getPlayerStats();
                    statAdapter = new StatAdapter(statList);
                    rvStatistics.setAdapter(statAdapter);
                }
                return true;
            }
            return false;
        });
    }

    private void loadActiveTeamState() {
        String teamCode = fbManager.getCurrentTeamCode();
        String teamName = fbManager.getCurrentTeamName();

        if (!teamCode.isEmpty()) {
            tvTeamInfo.setText("👥 " + teamName + " (Код: " + teamCode + ")");

            if (firebaseGameListener != null) {
                fbManager.removeGameListener(teamCode, firebaseGameListener);
            }

            firebaseGameListener = fbManager.listenToActiveGame(teamCode, activePlayers -> {
                Refresh((ArrayList<ItemModel>) activePlayers);
                checkWinner((ArrayList<ItemModel>) activePlayers);
            });
        } else {
            tvTeamInfo.setText("👥 Оффлайн Режим (Маҳаллий ўйин)");
            Refresh(dbSQL.readCourses());
        }
    }

    private void processRoundCalculation() {
        String teamCode = fbManager.getCurrentTeamCode();
        Map<Integer, Integer> numberMap = adapter.collectAllNumbers();

        if (!teamCode.isEmpty()) {
            for (ItemModel item : list) {
                int inputNum = item.getTemporaryAmount();
                if (inputNum != 0) {
                    fbManager.updateCourseInFirebase(teamCode, item.getName(), inputNum);
                }
            }
            adapter.clearAllInputs();
        } else {
            ArrayList<ItemModel> beforeList = dbSQL.readCourses();

            for (Map.Entry<Integer, Integer> entry : numberMap.entrySet()) {
                int id = entry.getKey();
                int amount = entry.getValue();

                if (amount != 0) {
                    dbSQL.updateCourse(String.valueOf(id), amount);
                }
            }

            adapter.clearAllInputs();
            ArrayList<ItemModel> updatedList = dbSQL.readCourses();

            List<Integer> newlyEliminatedIds = new ArrayList<>();
            for (ItemModel player : updatedList) {
                if (isPlayerOut(player)) {
                    for (ItemModel prev : beforeList) {
                        if (prev.getId() == player.getId() && !isPlayerOut(prev)) {
                            newlyEliminatedIds.add(player.getId());
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
        }
    }

    private void showTeamManagerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_team_manager, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        EditText etUserName = dialogView.findViewById(R.id.etUserName);
        EditText etCreateTeamName = dialogView.findViewById(R.id.etCreateTeamName);
        Button btnCreateTeamSubmit = dialogView.findViewById(R.id.btnCreateTeamSubmit);

        EditText etJoinTeamCode = dialogView.findViewById(R.id.etJoinTeamCode);
        Button btnJoinTeamSubmit = dialogView.findViewById(R.id.btnJoinTeamSubmit);
        Button btnPlayOffline = dialogView.findViewById(R.id.btnPlayOffline);

        etUserName.setText(fbManager.getUserName());

        btnCreateTeamSubmit.setOnClickListener(v -> {
            String userName = etUserName.getText().toString().trim();
            String teamName = etCreateTeamName.getText().toString().trim();

            if (userName.isEmpty() || teamName.isEmpty()) {
                Toast.makeText(this, "Исм ва Жамоа номини киритинг!", Toast.LENGTH_SHORT).show();
                return;
            }

            fbManager.createTeam(teamName, userName, new FirebaseManager.OnTeamCreatedListener() {
                @Override
                public void onSuccess(String teamCode, String name) {
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, "Жамоа яратилди! Код: " + teamCode, Toast.LENGTH_LONG).show();
                    loadActiveTeamState();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(MainActivity.this, "Хатолик: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnJoinTeamSubmit.setOnClickListener(v -> {
            String userName = etUserName.getText().toString().trim();
            String code = etJoinTeamCode.getText().toString().trim();

            if (userName.isEmpty() || code.length() != 6) {
                Toast.makeText(this, "Исм ва 6 хонали кодни тўғри киритинг!", Toast.LENGTH_SHORT).show();
                return;
            }

            fbManager.joinTeam(code, userName, new FirebaseManager.OnTeamJoinedListener() {
                @Override
                public void onSuccess(String teamCode, String name) {
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, name + " жамоасига қўшилдингиз!", Toast.LENGTH_SHORT).show();
                    loadActiveTeamState();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnPlayOffline.setOnClickListener(v -> {
            fbManager.saveCurrentTeam("", "");
            dialog.dismiss();
            Toast.makeText(this, "Оффлайн режимга ўтилди", Toast.LENGTH_SHORT).show();
            loadActiveTeamState();
        });

        dialog.show();
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
                        String teamCode = fbManager.getCurrentTeamCode();
                        if (!teamCode.isEmpty()) {
                            fbManager.clearActiveGameInFirebase(teamCode);
                        } else {
                            dbSQL.finishGameAndRecordStats(currentPlayers);
                            dbSQL.deleteAllData();
                            Refresh(dbSQL.readCourses());
                        }
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
                        String teamCode = fbManager.getCurrentTeamCode();
                        if (!teamCode.isEmpty()) {
                            fbManager.clearActiveGameInFirebase(teamCode);
                        } else {
                            dbSQL.finishGameAndRecordStats(currentPlayers);
                            dbSQL.deleteAllData();
                            Refresh(dbSQL.readCourses());
                        }
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
