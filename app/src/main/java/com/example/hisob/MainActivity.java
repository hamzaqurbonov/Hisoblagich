package com.example.hisob;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private DbSQL dbSQL;
    private FirebaseManager fbManager;
    private ValueEventListener firebaseGameListener;

    private Button btnClear, btnname;
    private ImageView delete_all, btnSwitchTeam;
    private TextView tvHeaderTitle, tvTeamInfo;

    private ArrayList<ItemModel> list = new ArrayList<>();
    private List<String> currentTeamPlayers = new ArrayList<>();
    private List<String> currentTrashPlayers = new ArrayList<>();
    private RecyclerView item_recycler, rvGameHistory, rvStatistics;
    private ItemAdapter adapter;
    private GameHistoryAdapter historyAdapter;
    private StatAdapter statAdapter;

    private PlayerSelectAdapter activeDialogSelectAdapter;
    private boolean activeDialogIsTrashMode = false;
    private String currentDealerName = "";

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

        tvTeamInfo.setOnClickListener(v -> shareOrCopyTeamCode());

        btnClear.setOnClickListener(v -> processRoundCalculation());

        btnname.setOnClickListener(v -> showAddCustomPlayerDialog());

        delete_all.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Янги ўйин")
                    .setMessage("Ўйинни тугатиб, барчасини ўчиришга ишончингиз комилми?\n(Натижалар Статистика ва Тарихга сақланади)")
                    .setPositiveButton("Ҳа", (dialog, which) -> {
                        currentDealerName = "";
                        String teamCode = fbManager.getCurrentTeamCode();
                        if (!teamCode.isEmpty()) {
                            fbManager.finishGameAndRecordStatsInFirebase(teamCode, list);
                        } else {
                            dbSQL.finishGameAndRecordStats(list);
                            dbSQL.deleteAllData();
                            Refresh(dbSQL.readCourses());
                        }
                    })
                    .setNegativeButton("Йўқ", null)
                    .show();
        });

        loadActiveTeamState();
    }

    private void shareOrCopyTeamCode() {
        String teamCode = fbManager.getCurrentTeamCode();
        String teamName = fbManager.getCurrentTeamName();

        if (teamCode.isEmpty()) {
            Toast.makeText(this, "Жамоани танлаш учун босинг", Toast.LENGTH_SHORT).show();
            showTeamManagerDialog();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("TeamCode", teamCode);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }

        Toast.makeText(this, "Жамоа коди нусхаланди: " + teamCode, Toast.LENGTH_SHORT).show();

        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "👥 \"Карта 108\" иловаси учун \"" + teamName + "\" жамоамиз коди: " + teamCode + "\n\nЖамоага қўшилиш учун иловага ушбу кодни киритинг!");
            startActivity(Intent.createChooser(shareIntent, "Жамоа кодини юбориш"));
        } catch (Exception ignored) {}
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        View layoutHisob = findViewById(R.id.layout_hisob);
        View layoutHistory = findViewById(R.id.layout_history);
        View layoutStat = findViewById(R.id.layout_stat);

        Button btnStatActive = findViewById(R.id.btnStatActive);
        Button btnStatTrash = findViewById(R.id.btnStatTrash);

        View.OnClickListener updateStatsListener = v -> {
            boolean trash = (v != null && v.getId() == R.id.btnStatTrash);
            btnStatActive.setTextColor(Color.parseColor(trash ? "#757575" : "#1976D2"));
            btnStatTrash.setTextColor(Color.parseColor(trash ? "#1976D2" : "#757575"));

            String teamCode = fbManager.getCurrentTeamCode();
            if (!teamCode.isEmpty()) {
                fbManager.listenToTeamStatsCategorized(teamCode, (activeStats, trashStats) -> {
                    statAdapter = new StatAdapter(trash ? trashStats : activeStats);
                    rvStatistics.setAdapter(statAdapter);
                });
            } else {
                ArrayList<StatModel> allStats = dbSQL.getPlayerStats();
                List<StatModel> filtered = new ArrayList<>();
                for (StatModel s : allStats) {
                    boolean isArchived = currentTrashPlayers.contains(s.getName());
                    if (trash == isArchived) {
                        filtered.add(s);
                    }
                }
                filtered.sort((s1, s2) -> Integer.compare(s2.getWins(), s1.getWins()));
                statAdapter = new StatAdapter(filtered);
                rvStatistics.setAdapter(statAdapter);
            }
        };

        btnStatActive.setOnClickListener(updateStatsListener);
        btnStatTrash.setOnClickListener(updateStatsListener);

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

                updateStatsListener.onClick(btnStatActive);
                return true;
            }
            return false;
        });
    }

    private void loadActiveTeamState() {
        String teamCode = fbManager.getCurrentTeamCode();
        String teamName = fbManager.getCurrentTeamName();

        if (!teamCode.isEmpty()) {
            tvTeamInfo.setText("👥 " + teamName + " (Код: " + teamCode + " 🔗)");

            if (firebaseGameListener != null) {
                fbManager.removeGameListener(teamCode, firebaseGameListener);
            }

            firebaseGameListener = fbManager.listenToActiveGame(teamCode, activePlayers -> {
                Refresh((ArrayList<ItemModel>) activePlayers);
                checkWinner((ArrayList<ItemModel>) activePlayers);
            });

            fbManager.listenToTeamPlayerList(teamCode, (activeList, trashList) -> {
                currentTeamPlayers.clear();
                currentTrashPlayers.clear();

                if (activeList != null) currentTeamPlayers.addAll(activeList);
                if (trashList != null) currentTrashPlayers.addAll(trashList);

                if (activeDialogSelectAdapter != null) {
                    activeDialogSelectAdapter.updateList(activeDialogIsTrashMode ? currentTrashPlayers : currentTeamPlayers, activeDialogIsTrashMode);
                }
            });
        } else {
            tvTeamInfo.setText("👥 Оффлайн Режим (Маҳаллий ўйин)");
            Refresh(dbSQL.readCourses());
            currentTeamPlayers.clear();
        }
    }

    private void showAddCustomPlayerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_player_selection, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();

        EditText etNewPlayerName = dialogView.findViewById(R.id.etNewPlayerName);
        Button btnAddPlayerToList = dialogView.findViewById(R.id.btnAddPlayerToList);

        Button btnViewActive = dialogView.findViewById(R.id.btnViewActive);
        Button btnViewTrash = dialogView.findViewById(R.id.btnViewTrash);

        RecyclerView rvSelectPlayers = dialogView.findViewById(R.id.rvSelectPlayers);
        Button btnConfirmSelection = dialogView.findViewById(R.id.btnConfirmSelection);

        rvSelectPlayers.setLayoutManager(new LinearLayoutManager(this));

        String teamCode = fbManager.getCurrentTeamCode();
        activeDialogIsTrashMode = false;

        PlayerSelectAdapter selectAdapter = new PlayerSelectAdapter(this, currentTeamPlayers, false, new PlayerSelectAdapter.OnPlayerActionListener() {
            @Override
            public void onDelete(String playerName) {
                if (!teamCode.isEmpty()) {
                    fbManager.deletePlayerFromTeamList(teamCode, playerName);
                }
                currentTeamPlayers.remove(playerName);
                if (!currentTrashPlayers.contains(playerName)) {
                    currentTrashPlayers.add(playerName);
                }
                if (activeDialogSelectAdapter != null) {
                    activeDialogSelectAdapter.updateList(activeDialogIsTrashMode ? currentTrashPlayers : currentTeamPlayers, activeDialogIsTrashMode);
                }
            }

            @Override
            public void onRestore(String playerName) {
                if (!teamCode.isEmpty()) {
                    fbManager.restorePlayerInTeamList(teamCode, playerName);
                }
                currentTrashPlayers.remove(playerName);
                if (!currentTeamPlayers.contains(playerName)) {
                    currentTeamPlayers.add(playerName);
                }
                if (activeDialogSelectAdapter != null) {
                    activeDialogSelectAdapter.updateList(activeDialogIsTrashMode ? currentTrashPlayers : currentTeamPlayers, activeDialogIsTrashMode);
                }
            }
        });

        activeDialogSelectAdapter = selectAdapter;
        rvSelectPlayers.setAdapter(selectAdapter);

        dialog.setOnDismissListener(d -> activeDialogSelectAdapter = null);

        btnViewActive.setOnClickListener(v -> {
            activeDialogIsTrashMode = false;
            btnViewActive.setTextColor(Color.parseColor("#1976D2"));
            btnViewTrash.setTextColor(Color.parseColor("#757575"));
            selectAdapter.updateList(currentTeamPlayers, false);
        });

        btnViewTrash.setOnClickListener(v -> {
            activeDialogIsTrashMode = true;
            btnViewActive.setTextColor(Color.parseColor("#757575"));
            btnViewTrash.setTextColor(Color.parseColor("#1976D2"));
            selectAdapter.updateList(currentTrashPlayers, true);
        });

        btnAddPlayerToList.setOnClickListener(v -> {
            String newName = etNewPlayerName.getText().toString().trim();
            if (!newName.isEmpty()) {
                if (currentTeamPlayers.contains(newName)) {
                    Toast.makeText(this, newName + " рўйхатда аллақачон мавжуд!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!teamCode.isEmpty()) {
                    fbManager.addPlayerToTeamList(teamCode, newName);
                } else {
                    currentTeamPlayers.add(newName);
                    selectAdapter.updateList(currentTeamPlayers, false);
                }
                etNewPlayerName.setText("");
                Toast.makeText(this, newName + " рўйхатга қўшилди", Toast.LENGTH_SHORT).show();
            }
        });

        btnConfirmSelection.setOnClickListener(v -> {
            Set<String> selectedNames = selectAdapter.getSelectedPlayers();
            if (selectedNames != null && !selectedNames.isEmpty()) {
                for (String name : selectedNames) {
                    if (!teamCode.isEmpty()) {
                        fbManager.addPlayerToGame(teamCode, name);
                    } else {
                        dbSQL.addNewCourse(name, "0", "0");
                    }
                }
                if (teamCode.isEmpty()) {
                    Refresh(dbSQL.readCourses());
                }
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void processRoundCalculation() {
        String teamCode = fbManager.getCurrentTeamCode();
        Map<Integer, Integer> numberMap = adapter.collectAllNumbers();

        boolean anyInputEntered = false;
        for (int val : numberMap.values()) {
            if (val != 0) {
                anyInputEntered = true;
                break;
            }
        }

        if (!anyInputEntered) {
            Toast.makeText(this, "Ҳеч қандай балл киритилмади!", Toast.LENGTH_SHORT).show();
            return;
        }

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

        View layoutSavedTeamsContainer = dialogView.findViewById(R.id.layoutSavedTeamsContainer);
        RecyclerView rvSavedTeams = dialogView.findViewById(R.id.rvSavedTeams);

        etUserName.setText(fbManager.getUserName());

        List<FirebaseManager.SavedTeamItem> savedTeams = fbManager.getSavedTeamsList();
        if (savedTeams != null && !savedTeams.isEmpty()) {
            layoutSavedTeamsContainer.setVisibility(View.VISIBLE);
            rvSavedTeams.setLayoutManager(new LinearLayoutManager(this));

            final SavedTeamAdapter[] savedAdapterHolder = new SavedTeamAdapter[1];
            savedAdapterHolder[0] = new SavedTeamAdapter(savedTeams, new SavedTeamAdapter.OnSavedTeamActionListener() {
                @Override
                public void onSelect(String teamCode, String teamName) {
                    fbManager.saveCurrentTeam(teamCode, teamName);
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, teamName + " жамоасига уландингиз!", Toast.LENGTH_SHORT).show();
                    loadActiveTeamState();
                }

                @Override
                public void onRemove(String teamCode) {
                    fbManager.removeTeamFromSavedList(teamCode);
                    List<FirebaseManager.SavedTeamItem> updatedSaved = fbManager.getSavedTeamsList();
                    if (updatedSaved.isEmpty()) {
                        layoutSavedTeamsContainer.setVisibility(View.GONE);
                    } else {
                        savedAdapterHolder[0] = new SavedTeamAdapter(updatedSaved, this);
                        rvSavedTeams.setAdapter(savedAdapterHolder[0]);
                    }
                }
            });
            rvSavedTeams.setAdapter(savedAdapterHolder[0]);
        } else {
            layoutSavedTeamsContainer.setVisibility(View.GONE);
        }

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
                        currentDealerName = "";
                        String teamCode = fbManager.getCurrentTeamCode();
                        if (!teamCode.isEmpty()) {
                            fbManager.finishGameAndRecordStatsInFirebase(teamCode, currentPlayers);
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
                        currentDealerName = "";
                        String teamCode = fbManager.getCurrentTeamCode();
                        if (!teamCode.isEmpty()) {
                            fbManager.finishGameAndRecordStatsInFirebase(teamCode, currentPlayers);
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

    private void updateDealerIfNecessary(List<ItemModel> activePlayers) {
        if (activePlayers == null || activePlayers.isEmpty()) {
            currentDealerName = "";
            return;
        }

        boolean allZeros = true;
        for (ItemModel item : activePlayers) {
            int amt = 0;
            try { amt = Integer.parseInt(item.getAmount()); } catch (Exception ignored) {}
            if (amt != 0) {
                allZeros = false;
                break;
            }
        }

        if (allZeros) {
            boolean dealerExists = false;
            for (ItemModel item : activePlayers) {
                if (item.getName().equals(currentDealerName)) {
                    dealerExists = true;
                    break;
                }
            }
            if (!dealerExists || currentDealerName.isEmpty()) {
                int randomIndex = new Random().nextInt(activePlayers.size());
                currentDealerName = activePlayers.get(randomIndex).getName();
            }
        }
    }

    void Refresh(ArrayList<ItemModel> events) {
        list.clear();
        if (events != null && !events.isEmpty()) {
            list.addAll(events);
            updateDealerIfNecessary(list);

            list.sort((item1, item2) -> {
                boolean out1 = isPlayerOut(item1);
                boolean out2 = isPlayerOut(item2);

                if (out1 != out2) {
                    return out1 ? 1 : -1;
                }

                int amount1 = 0, amount2 = 0;
                try { amount1 = Integer.parseInt(item1.getAmount()); } catch (Exception ignored) {}
                try { amount2 = Integer.parseInt(item2.getAmount()); } catch (Exception ignored) {}

                if (amount1 != amount2) {
                    return Integer.compare(amount2, amount1);
                }
                if (item1.getName().equals(currentDealerName)) return -1;
                if (item2.getName().equals(currentDealerName)) return 1;
                return 0;
            });

            boolean anyNonZeroActive = false;
            for (ItemModel item : list) {
                if (!isPlayerOut(item)) {
                    int amt = 0;
                    try { amt = Integer.parseInt(item.getAmount()); } catch (Exception ignored) {}
                    if (amt != 0) {
                        anyNonZeroActive = true;
                        break;
                    }
                }
            }

            if (anyNonZeroActive) {
                currentDealerName = list.get(0).getName();
            }
        }
        adapter.setDealerName(currentDealerName);
        adapter.notifyDataSetChanged();
    }
}
