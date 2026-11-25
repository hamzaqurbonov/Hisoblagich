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

import java.util.ArrayList;
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

    public interface OnHistoryDataListener {
        void onHistoryLoaded(List<GameHistoryModel> historyList);
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
                saveCurrentTeam(teamCode, teamName);
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
                    saveCurrentTeam(teamCode, teamName);
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

                    if (name != null) {
                        if (amount == null) amount = "0";
                        if (amountplus == null) amountplus = "0";
                        list.add(new ItemModel(idCounter++, name, amount, amountplus));
                    }
                }
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

    public void addPlayerToGame(String teamCode, String playerName) {
        if (teamCode == null || teamCode.isEmpty()) return;
        DatabaseReference pRef = mDatabase.child("teams").child(teamCode).child("activeGame").child("players").child(playerName);

        Map<String, Object> data = new HashMap<>();
        data.put("name", playerName);
        data.put("amount", "0");
        data.put("amountplus", "0");
        data.put("elim_order", 0);
        pRef.setValue(data);
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
