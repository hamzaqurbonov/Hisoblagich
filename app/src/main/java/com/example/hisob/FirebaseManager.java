package com.example.hisob;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class FirebaseManager {
    private static FirebaseManager instance;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private Context context;

    private static final String PREF_NAME = "HisobFirebasePrefs";
    private static final String KEY_TEAM_CODE = "current_team_code";
    private static final String KEY_TEAM_NAME = "current_team_name";
    private static final String KEY_USER_NAME = "user_display_name";
    private static final String DB_URL = "https://hisoblagich-108-app-default-rtdb.firebaseio.com";

    public static class SavedTeamItem {
        public String code;
        public String name;

        public SavedTeamItem(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    public interface OnTeamCreatedListener {
        void onSuccess(String teamCode, String teamName);
        void onError(String message);
    }

    public interface OnTeamJoinedListener {
        void onSuccess(String teamCode, String teamName);
        void onError(String message);
    }

    public interface OnGameDataListener {
        void onDataChanged(List<ItemModel> activePlayers);
    }

    public interface OnStatsDataListener {
        void onStatsLoaded(List<StatModel> statList);
    }

    public interface OnCategorizedStatsDataListener {
        void onStatsLoaded(List<StatModel> activeStats, List<StatModel> trashStats);
    }

    public interface OnHistoryDataListener {
        void onHistoryLoaded(List<GameHistoryModel> historyList);
    }

    public interface OnPlayerListListener {
        void onPlayerListLoaded(List<String> activeList, List<String> trashList);
    }

    private FirebaseManager(Context context) {
        this.context = context.getApplicationContext();
        try {
            FirebaseDatabase.getInstance(DB_URL).setPersistenceEnabled(true);
        } catch (Exception ignored) {}
        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();
        mAuth = FirebaseAuth.getInstance();
        ensureAuth();
    }

    public static synchronized FirebaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseManager(context);
        }
        return instance;
    }

    private void ensureAuth() {
        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously();
        }
    }

    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : "anonymous";
    }

    // SharedPreferences Helpers
    public void saveCurrentTeam(String teamCode, String teamName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TEAM_CODE, teamCode).putString(KEY_TEAM_NAME, teamName).apply();
    }

    public String getCurrentTeamCode() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TEAM_CODE, "");
    }

    public String getCurrentTeamName() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TEAM_NAME, "");
    }

    public void saveUserName(String userName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USER_NAME, userName).apply();
    }

    public String getUserName() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "Иномжон");
    }

    // --- Saved Teams Manager ---

    public void addTeamToSavedList(String teamCode, String teamName) {
        if (teamCode == null || teamCode.isEmpty()) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String currentSaved = prefs.getString("saved_teams_json", "");

        Map<String, String> map = parseTeamsString(currentSaved);
        map.put(teamCode, teamName != null ? teamName : "Жамоа " + teamCode);

        prefs.edit().putString("saved_teams_json", serializeTeamsMap(map)).apply();
    }

    public List<SavedTeamItem> getSavedTeamsList() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String currentSaved = prefs.getString("saved_teams_json", "");
        Map<String, String> map = parseTeamsString(currentSaved);

        List<SavedTeamItem> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            list.add(new SavedTeamItem(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    public void removeTeamFromSavedList(String teamCode) {
        if (teamCode == null || teamCode.isEmpty()) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String currentSaved = prefs.getString("saved_teams_json", "");
        Map<String, String> map = parseTeamsString(currentSaved);
        map.remove(teamCode);
        prefs.edit().putString("saved_teams_json", serializeTeamsMap(map)).apply();
    }

    private static Map<String, String> parseTeamsString(String str) {
        Map<String, String> map = new HashMap<>();
        if (str == null || str.trim().isEmpty()) return map;
        String[] pairs = str.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split("::");
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }

    private static String serializeTeamsMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append("::").append(entry.getValue());
        }
        return sb.toString();
    }

    // --- Team Operations ---

    public void createTeam(String teamName, String creatorName, OnTeamCreatedListener listener) {
        ensureAuth();
        String teamCode = String.format(Locale.US, "%06d", new Random().nextInt(1000000));

        DatabaseReference teamRef = mDatabase.child("teams").child(teamCode);
        Map<String, Object> teamData = new HashMap<>();
        teamData.put("name", teamName);
        teamData.put("code", teamCode);
        teamData.put("createdBy", getCurrentUserId());
        teamData.put("createdAt", System.currentTimeMillis());

        teamRef.setValue(teamData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                teamRef.child("members").child(creatorName).setValue(true);
                addPlayerToTeamList(teamCode, creatorName);
                saveCurrentTeam(teamCode, teamName);
                addTeamToSavedList(teamCode, teamName);
                saveUserName(creatorName);
                listener.onSuccess(teamCode, teamName);
            } else {
                listener.onError(task.getException() != null ? task.getException().getMessage() : "Хатолик юз берди");
            }
        });
    }

    public void joinTeam(String teamCode, String playerName, OnTeamJoinedListener listener) {
        ensureAuth();
        DatabaseReference teamRef = mDatabase.child("teams").child(teamCode);

        teamRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String teamName = snapshot.child("name").getValue(String.class);
                    if (teamName == null) teamName = "Жамоа " + teamCode;

                    teamRef.child("members").child(playerName).setValue(true);
                    addPlayerToTeamList(teamCode, playerName);
                    saveCurrentTeam(teamCode, teamName);
                    addTeamToSavedList(teamCode, teamName);
                    saveUserName(playerName);
                    listener.onSuccess(teamCode, teamName);
                } else {
                    listener.onError("Жамоа коди топилмади: " + teamCode);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    // --- Custom Team Player List Management ---

    public void listenToTeamPlayerList(String teamCode, OnPlayerListListener listener) {
        if (teamCode == null || teamCode.isEmpty()) return;

        mDatabase.child("teams").child(teamCode).child("playerList").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> activeList = new ArrayList<>();
                List<String> trashList = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.getKey();
                    Object statusObj = child.getValue();
                    String status = statusObj != null ? statusObj.toString() : "active";

                    if (name != null) {
                        if ("archived".equals(status)) {
                            trashList.add(name);
                        } else {
                            activeList.add(name);
                        }
                    }
                }
                listener.onPlayerListLoaded(activeList, trashList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void addPlayerToTeamList(String teamCode, String playerName) {
        if (teamCode == null || teamCode.isEmpty() || playerName == null || playerName.trim().isEmpty()) return;
        mDatabase.child("teams").child(teamCode).child("playerList").child(playerName.trim()).setValue("active");
    }

    public void deletePlayerFromTeamList(String teamCode, String playerName) {
        if (teamCode == null || teamCode.isEmpty() || playerName == null) return;
        // Soft delete (moves to Trash Bin, preserves stats and history!)
        mDatabase.child("teams").child(teamCode).child("playerList").child(playerName).setValue("archived");
    }

    public void restorePlayerInTeamList(String teamCode, String playerName) {
        if (teamCode == null || teamCode.isEmpty() || playerName == null) return;
        mDatabase.child("teams").child(teamCode).child("playerList").child(playerName).setValue("active");
    }

    public void renamePlayerInTeam(String teamCode, String oldName, String newName) {
        if (teamCode == null || teamCode.isEmpty() || oldName == null || newName == null) return;
        String cleanNew = newName.trim();
        if (cleanNew.isEmpty() || cleanNew.equals(oldName)) return;

        mDatabase.child("teams").child(teamCode).child("playerList").child(oldName).removeValue();
        mDatabase.child("teams").child(teamCode).child("playerList").child(cleanNew).setValue("active");

        DatabaseReference oldStatRef = mDatabase.child("teams").child(teamCode).child("stats").child(oldName);
        DatabaseReference newStatRef = mDatabase.child("teams").child(teamCode).child("stats").child(cleanNew);

        oldStatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    newStatRef.setValue(snapshot.getValue()).addOnCompleteListener(task -> oldStatRef.removeValue());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- Realtime Game State ---

    public ValueEventListener listenToActiveGame(String teamCode, OnGameDataListener listener) {
        if (teamCode == null || teamCode.isEmpty()) return null;

        DatabaseReference gameRef = mDatabase.child("teams").child(teamCode).child("activeGame").child("players");

        ValueEventListener valListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ItemModel> list = new ArrayList<>();
                int idCounter = 1;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("name").getValue(String.class);
                    String amount = child.child("amount").getValue(String.class);
                    String amountplus = child.child("amountplus").getValue(String.class);
                    Long orderVal = child.child("order").getValue(Long.class);
                    int order = orderVal != null ? orderVal.intValue() : idCounter;

                    if (name != null) {
                        if (amount == null) amount = "0";
                        if (amountplus == null) amountplus = "0";
                        list.add(new ItemModel(idCounter++, name, amount, amountplus, order));
                    }
                }
                list.sort(Comparator.comparingInt(ItemModel::getOrder));
                listener.onDataChanged(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        gameRef.addValueEventListener(valListener);
        return valListener;
    }

    public void removeGameListener(String teamCode, ValueEventListener listener) {
        if (teamCode != null && !teamCode.isEmpty() && listener != null) {
            mDatabase.child("teams").child(teamCode).child("activeGame").child("players").removeEventListener(listener);
        }
    }

    public void addPlayerToGameWithOrder(String teamCode, String playerName, int order) {
        if (teamCode == null || teamCode.isEmpty()) return;
        DatabaseReference pRef = mDatabase.child("teams").child(teamCode).child("activeGame").child("players").child(playerName);

        Map<String, Object> data = new HashMap<>();
        data.put("name", playerName);
        data.put("amount", "0");
        data.put("amountplus", "0");
        data.put("elim_order", 0);
        data.put("order", order);
        pRef.setValue(data);

        addPlayerToTeamList(teamCode, playerName);
    }

    public void addPlayerToGame(String teamCode, String playerName) {
        addPlayerToGameWithOrder(teamCode, playerName, 999);
    }

    public void removePlayerFromGame(String teamCode, String playerName) {
        if (teamCode == null || teamCode.isEmpty()) return;
        mDatabase.child("teams").child(teamCode).child("activeGame").child("players").child(playerName).removeValue();
    }

    public void resetPlayerScoreInFirebase(String teamCode, String playerName) {
        if (teamCode == null || teamCode.isEmpty()) return;
        DatabaseReference pRef = mDatabase.child("teams").child(teamCode).child("activeGame").child("players").child(playerName);
        Map<String, Object> updates = new HashMap<>();
        updates.put("amount", "0");
        updates.put("amountplus", "0");
        pRef.updateChildren(updates);
    }

    public void recordFirebasePlayerElimination(String teamCode, String playerName) {
        if (teamCode == null || teamCode.isEmpty()) return;
        DatabaseReference playersRef = mDatabase.child("teams").child(teamCode).child("activeGame").child("players");

        playersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int maxOrder = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Long orderVal = child.child("elim_order").getValue(Long.class);
                    if (orderVal != null && orderVal.intValue() > maxOrder) {
                        maxOrder = orderVal.intValue();
                    }
                }
                DataSnapshot pChild = snapshot.child(playerName);
                Long currentOrder = pChild.child("elim_order").getValue(Long.class);
                if (currentOrder == null || currentOrder.intValue() == 0) {
                    playersRef.child(playerName).child("elim_order").setValue(maxOrder + 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void updateCourseInFirebase(String teamCode, String playerName, int addedAmount) {
        if (teamCode == null || teamCode.isEmpty()) return;
        DatabaseReference pRef = mDatabase.child("teams").child(teamCode).child("activeGame").child("players").child(playerName);

        pRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String amountStr = snapshot.child("amount").getValue(String.class);
                    String amountPlusStr = snapshot.child("amountplus").getValue(String.class);

                    int currentAmount = 0;
                    try { currentAmount = Integer.parseInt(amountStr); } catch (Exception ignored) {}

                    if (addedAmount == -77) {
                        if (amountPlusStr == null) amountPlusStr = "";
                        if (!amountPlusStr.contains("(7)")) {
                            String newPlus = amountPlusStr.isEmpty() ? "(7)" : amountPlusStr + " (7)";
                            pRef.child("amountplus").setValue(newPlus);
                            recordFirebasePlayerElimination(teamCode, playerName);
                            incrementTeamStat(teamCode, playerName, "times_77");
                        }
                        return;
                    }

                    int newAmount = currentAmount + addedAmount;
                    String newAmountPlus;

                    if (amountPlusStr == null || amountPlusStr.isEmpty()) {
                        newAmountPlus = String.valueOf(addedAmount);
                    } else {
                        int equalIndex = amountPlusStr.lastIndexOf("=");
                        if (equalIndex != -1) {
                            newAmountPlus = amountPlusStr.substring(0, equalIndex).trim() + "+" + addedAmount;
                        } else {
                            newAmountPlus = amountPlusStr + "+" + addedAmount;
                        }
                    }
                    newAmountPlus += "=" + newAmount;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("amount", String.valueOf(newAmount));
                    updates.put("amountplus", newAmountPlus);
                    pRef.updateChildren(updates);

                    if (newAmount >= 108 && currentAmount < 108) {
                        recordFirebasePlayerElimination(teamCode, playerName);
                    }

                    if (newAmount == 107) {
                        incrementTeamStat(teamCode, playerName, "times_107");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void clearActiveGameInFirebase(String teamCode) {
        if (teamCode == null || teamCode.isEmpty()) return;
        mDatabase.child("teams").child(teamCode).child("activeGame").removeValue();
    }

    public void finishGameAndRecordStatsInFirebase(String teamCode, List<ItemModel> currentPlayers) {
        if (teamCode == null || teamCode.isEmpty()) return;

        DatabaseReference gameRef = mDatabase.child("teams").child(teamCode).child("activeGame").child("players");
        gameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<DbSQL.PlayerGameStatus> statuses = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String name = child.child("name").getValue(String.class);
                        String amtStr = child.child("amount").getValue(String.class);
                        String amtPlusStr = child.child("amountplus").getValue(String.class);
                        Long orderVal = child.child("elim_order").getValue(Long.class);

                        int amt = 0;
                        try { amt = Integer.parseInt(amtStr); } catch (Exception ignored) {}
                        if (amtPlusStr == null) amtPlusStr = "";
                        int order = orderVal != null ? orderVal.intValue() : 0;

                        if (name != null) {
                            statuses.add(new DbSQL.PlayerGameStatus(name, amt, amtPlusStr, order));
                        }
                    }
                }

                if (statuses.isEmpty() && currentPlayers != null) {
                    for (ItemModel item : currentPlayers) {
                        int amt = 0;
                        try { amt = Integer.parseInt(item.getAmount()); } catch (Exception ignored) {}
                        String plus = item.getAmountplus() != null ? item.getAmountplus() : "";
                        statuses.add(new DbSQL.PlayerGameStatus(item.getName(), amt, plus, 0));
                    }
                }

                if (statuses.isEmpty()) return;

                List<DbSQL.PlayerGameStatus> activePlayers = new ArrayList<>();
                List<DbSQL.PlayerGameStatus> eliminatedPlayers = new ArrayList<>();

                for (DbSQL.PlayerGameStatus p : statuses) {
                    boolean is77 = p.amountPlus.contains("(7)");
                    boolean isOut = (p.amount >= 108 || is77 || p.elimOrder > 0);
                    if (!isOut) {
                        activePlayers.add(p);
                    } else {
                        eliminatedPlayers.add(p);
                    }
                }

                // Active players: lowest score first (winner, 1st place)
                activePlayers.sort(Comparator.comparingInt(p -> p.amount));

                // Eliminated players: player eliminated LAST gets BETTER rank (e.g. 2nd place)
                eliminatedPlayers.sort((p1, p2) -> {
                    int order1 = p1.elimOrder;
                    int order2 = p2.elimOrder;

                    if (order1 != order2) {
                        if (order1 == 0) order1 = 999; // order 0 means eliminated in final turn -> stayed longest!
                        if (order2 == 0) order2 = 999;
                        if (order1 != order2) {
                            return Integer.compare(order2, order1); // Higher elimOrder = eliminated later = better rank
                        }
                    }
                    return Integer.compare(p1.amount, p2.amount); // Lower score (closest to 107) = stayed longer
                });

                int currentRank = 1;
                String winnerName = "Аниқланмади";

                for (DbSQL.PlayerGameStatus p : activePlayers) {
                    p.computedRank = currentRank++;
                    if (p.computedRank == 1) {
                        winnerName = p.name;
                    }
                }

                for (DbSQL.PlayerGameStatus p : eliminatedPlayers) {
                    p.computedRank = currentRank++;
                    if (winnerName.equals("Аниқланмади") && p.computedRank == 1) {
                        winnerName = p.name;
                    }
                }

                // Sort statuses by computedRank ascending (1-ўрин first at top)
                statuses.sort(Comparator.comparingInt(p -> p.computedRank));

                // History details
                String dateStr = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());
                StringBuilder detailsBuilder = new StringBuilder();

                for (DbSQL.PlayerGameStatus p : statuses) {
                    boolean is77 = p.amountPlus.contains("(7)");
                    boolean isOut = (p.amount >= 108 || is77 || p.elimOrder > 0);

                    detailsBuilder.append(p.name).append(": ").append(p.amount);
                    if (is77) {
                        detailsBuilder.append(" (7)");
                    }
                    detailsBuilder.append(" (").append(p.computedRank).append("-ўрин)");
                    if (isOut && !is77) detailsBuilder.append("");
                    detailsBuilder.append("\n");
                }

                // Save to Firebase History
                DatabaseReference histRef = mDatabase.child("teams").child(teamCode).child("history").push();
                Map<String, Object> histData = new HashMap<>();
                histData.put("dateStr", dateStr);
                histData.put("winnerName", winnerName);
                histData.put("details", detailsBuilder.toString().trim());
                histRef.setValue(histData);

                // Update Firebase Stats for each player
                DatabaseReference statsRef = mDatabase.child("teams").child(teamCode).child("stats");

                for (DbSQL.PlayerGameStatus p : statuses) {
                    boolean isWin = (p.computedRank == 1);
                    DatabaseReference pStatRef = statsRef.child(p.name);

                    pStatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            int wins = getIntVal(snap.child("wins"));
                            int losses = getIntVal(snap.child("losses"));
                            int games = getIntVal(snap.child("games_played"));
                            int points = getIntVal(snap.child("total_points"));
                            int rankSum = getIntVal(snap.child("total_rank"));
                            String oldRankCounts = snap.child("rank_counts").getValue(String.class);

                            Map<Integer, Integer> rankMap = DbSQL.parseRankMap(oldRankCounts);
                            rankMap.put(p.computedRank, rankMap.getOrDefault(p.computedRank, 0) + 1);
                            String newRankCounts = DbSQL.serializeRankMap(rankMap);

                            Map<String, Object> statUpdates = new HashMap<>();
                            statUpdates.put("wins", isWin ? wins + 1 : wins);
                            statUpdates.put("losses", !isWin ? losses + 1 : losses);
                            statUpdates.put("games_played", games + 1);
                            statUpdates.put("total_points", points + p.amount);
                            statUpdates.put("total_rank", rankSum + p.computedRank);
                            statUpdates.put("rank_counts", newRankCounts);

                            pStatRef.updateChildren(statUpdates);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }

                // Reset active game scores to 0 for new match, and set 1st place winner to order = 1 (position 0 / Card Dealer 🎴)
                DatabaseReference activePlayersRef = mDatabase.child("teams").child(teamCode).child("activeGame").child("players");
                activePlayersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot activeSnap) {
                        int defaultOrder = 2;
                        for (DataSnapshot pChild : activeSnap.getChildren()) {
                            String pName = pChild.getKey();
                            if (pName == null) continue;

                            int assignedOrder = defaultOrder++;
                            for (DbSQL.PlayerGameStatus pStatus : statuses) {
                                if (pStatus.name.equals(pName) && pStatus.computedRank == 1) {
                                    assignedOrder = 1; // 1st place winner gets position 0 / Card Dealer 🎴!
                                    break;
                                }
                            }

                            Map<String, Object> resetData = new HashMap<>();
                            resetData.put("amount", "0");
                            resetData.put("amountplus", "0");
                            resetData.put("elim_order", 0);
                            resetData.put("order", assignedOrder);

                            activePlayersRef.child(pName).updateChildren(resetData);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void incrementTeamStat(String teamCode, String playerName, String statField) {
        if (teamCode == null || teamCode.isEmpty()) return;
        DatabaseReference statRef = mDatabase.child("teams").child(teamCode).child("stats").child(playerName).child(statField);
        statRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long current = 0;
                if (snapshot.exists() && snapshot.getValue(Long.class) != null) {
                    current = snapshot.getValue(Long.class);
                }
                statRef.setValue(current + 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- Team Isolated Stats and History ---

    public void listenToTeamStatsCategorized(String teamCode, OnCategorizedStatsDataListener listener) {
        if (teamCode == null || teamCode.isEmpty()) return;

        mDatabase.child("teams").child(teamCode).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot teamSnapshot) {
                DataSnapshot statsSnap = teamSnapshot.child("stats");
                DataSnapshot playerListSnap = teamSnapshot.child("playerList");

                Map<String, String> playerStatusMap = new HashMap<>();
                for (DataSnapshot pChild : playerListSnap.getChildren()) {
                    if (pChild.getKey() != null) {
                        Object val = pChild.getValue();
                        playerStatusMap.put(pChild.getKey(), val != null ? val.toString() : "active");
                    }
                }

                List<StatModel> activeStats = new ArrayList<>();
                List<StatModel> trashStats = new ArrayList<>();

                for (DataSnapshot child : statsSnap.getChildren()) {
                    String name = child.getKey();
                    if (name == null) continue;

                    int wins = getIntVal(child.child("wins"));
                    int losses = getIntVal(child.child("losses"));
                    int games = getIntVal(child.child("games_played"));
                    int times107 = getIntVal(child.child("times_107"));
                    int times77 = getIntVal(child.child("times_77"));
                    int totalPoints = getIntVal(child.child("total_points"));
                    int rankSum = getIntVal(child.child("total_rank"));
                    String rankCountsStr = child.child("rank_counts").getValue(String.class);

                    Map<Integer, Integer> rankMap = DbSQL.parseRankMap(rankCountsStr);
                    StatModel statModel = new StatModel(name, wins, losses, games, times107, times77, totalPoints, rankSum, rankMap);

                    String status = playerStatusMap.getOrDefault(name, "active");
                    if ("archived".equals(status)) {
                        trashStats.add(statModel);
                    } else {
                        activeStats.add(statModel);
                    }
                }

                // Sort stats by Wins descending (highest wins first at top)
                activeStats.sort((s1, s2) -> Integer.compare(s2.getWins(), s1.getWins()));
                trashStats.sort((s1, s2) -> Integer.compare(s2.getWins(), s1.getWins()));

                listener.onStatsLoaded(activeStats, trashStats);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void listenToTeamStats(String teamCode, OnStatsDataListener listener) {
        if (teamCode == null || teamCode.isEmpty()) return;

        mDatabase.child("teams").child(teamCode).child("stats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<StatModel> statList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.getKey();
                    int wins = getIntVal(child.child("wins"));
                    int losses = getIntVal(child.child("losses"));
                    int games = getIntVal(child.child("games_played"));
                    int times107 = getIntVal(child.child("times_107"));
                    int times77 = getIntVal(child.child("times_77"));
                    int totalPoints = getIntVal(child.child("total_points"));
                    int rankSum = getIntVal(child.child("total_rank"));
                    String rankCountsStr = child.child("rank_counts").getValue(String.class);

                    Map<Integer, Integer> rankMap = DbSQL.parseRankMap(rankCountsStr);
                    statList.add(new StatModel(name, wins, losses, games, times107, times77, totalPoints, rankSum, rankMap));
                }

                // Sort statList by Wins descending (highest wins first at top)
                statList.sort((s1, s2) -> Integer.compare(s2.getWins(), s1.getWins()));

                listener.onStatsLoaded(statList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void listenToTeamHistory(String teamCode, OnHistoryDataListener listener) {
        if (teamCode == null || teamCode.isEmpty()) return;

        mDatabase.child("teams").child(teamCode).child("history").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<GameHistoryModel> list = new ArrayList<>();
                int idCounter = 1;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String dateStr = child.child("dateStr").getValue(String.class);
                    String winnerName = child.child("winnerName").getValue(String.class);
                    String details = child.child("details").getValue(String.class);

                    list.add(new GameHistoryModel(idCounter++, dateStr != null ? dateStr : "", winnerName != null ? winnerName : "", details != null ? details : ""));
                }
                Collections.reverse(list); // Reverse so newest games appear FIRST at the top!
                listener.onHistoryLoaded(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private int getIntVal(DataSnapshot snap) {
        if (snap.exists() && snap.getValue() != null) {
            try { return Integer.parseInt(snap.getValue().toString()); } catch (Exception ignored) {}
        }
        return 0;
    }
}
