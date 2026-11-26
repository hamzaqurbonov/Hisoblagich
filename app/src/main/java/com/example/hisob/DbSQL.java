package com.example.hisob;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DbSQL extends SQLiteOpenHelper {

    private static final String DB_NAME = "DBMain_v4";
    private static final int DB_VERSION = 4;

    // Active Game Table
    private static final String TABLE_NAME = "mycourses";
    private static final String ID_COL = "id";
    private static final String NAME = "name";
    private static final String AMOUNT = "amount";
    private static final String AMOUNTPLUS = "amountplus";
    private static final String ELIM_ORDER = "elim_order";

    // Player Stats Table
    private static final String STAT_TABLE = "player_stats";
    private static final String STAT_NAME = "name";
    private static final String STAT_WINS = "wins";
    private static final String STAT_LOSSES = "losses";
    private static final String STAT_GAMES = "games_played";
    private static final String STAT_107 = "times_107";
    private static final String STAT_77 = "times_77";
    private static final String STAT_POINTS = "total_points";
    private static final String STAT_RANK_SUM = "total_rank";
    private static final String STAT_RANK_COUNTS = "rank_counts";

    // Game History Table
    private static final String HISTORY_TABLE = "game_history";
    private static final String HIST_ID = "id";
    private static final String HIST_DATE = "date_str";
    private static final String HIST_WINNER = "winner_name";
    private static final String HIST_DETAILS = "details";

    private String lastWinnerName = "";

    public DbSQL(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String queryActive = "CREATE TABLE " + TABLE_NAME + " (" + ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + NAME + " TEXT,"
                + AMOUNT + " TEXT,"
                + AMOUNTPLUS + " TEXT,"
                + ELIM_ORDER + " INTEGER DEFAULT 0)";
        db.execSQL(queryActive);

        String queryStats = "CREATE TABLE " + STAT_TABLE + " ("
                + STAT_NAME + " TEXT PRIMARY KEY, "
                + STAT_WINS + " INTEGER DEFAULT 0, "
                + STAT_LOSSES + " INTEGER DEFAULT 0, "
                + STAT_GAMES + " INTEGER DEFAULT 0, "
                + STAT_107 + " INTEGER DEFAULT 0, "
                + STAT_77 + " INTEGER DEFAULT 0, "
                + STAT_POINTS + " INTEGER DEFAULT 0, "
                + STAT_RANK_SUM + " INTEGER DEFAULT 0, "
                + STAT_RANK_COUNTS + " TEXT DEFAULT '')";
        db.execSQL(queryStats);

        String queryHistory = "CREATE TABLE " + HISTORY_TABLE + " ("
                + HIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HIST_DATE + " TEXT, "
                + HIST_WINNER + " TEXT, "
                + HIST_DETAILS + " TEXT)";
        db.execSQL(queryHistory);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + STAT_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + HISTORY_TABLE);
        onCreate(db);
    }

    public void addNewCourse(String name, String amount, String amountplus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NAME, name);
        values.put(AMOUNT, amount);
        values.put(AMOUNTPLUS, amountplus);
        values.put(ELIM_ORDER, 0);
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public void updateNull(String id, String newAmount, String newAmountPlus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AMOUNT, newAmount);
        values.put(AMOUNTPLUS, newAmountPlus);

        String selection = ID_COL + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        db.update(TABLE_NAME, values, selection, selectionArgs);
        db.close();
    }

    public void updateCourse(String id, int addedAmount) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + NAME + ", " + AMOUNT + ", " + AMOUNTPLUS + " FROM " + TABLE_NAME + " WHERE " + ID_COL + " = ?", new String[]{id});
        if (cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(NAME));
            int currentAmount = cursor.getInt(cursor.getColumnIndexOrThrow(AMOUNT));
            String currentAmountPlus = cursor.getString(cursor.getColumnIndexOrThrow(AMOUNTPLUS));

            if (addedAmount == -77) {
                if (currentAmountPlus == null) currentAmountPlus = "";
                if (!currentAmountPlus.contains("(7)")) {
                    String newAmountPlus = currentAmountPlus.isEmpty() ? "(7)" : currentAmountPlus + " (7)";
                    ContentValues values = new ContentValues();
                    values.put(AMOUNTPLUS, newAmountPlus);

                    String selection = ID_COL + " = ?";
                    String[] selectionArgs = {id};
                    db.update(TABLE_NAME, values, selection, selectionArgs);

                    incrementStatCount(name, STAT_77);
                }
                cursor.close();
                db.close();
                return;
            }

            int newAmount = currentAmount + addedAmount;
            String newAmountPlus;

            if (currentAmountPlus == null || currentAmountPlus.isEmpty()) {
                newAmountPlus = String.valueOf(addedAmount);
            } else {
                int equalIndex = currentAmountPlus.lastIndexOf("=");
                if (equalIndex != -1) {
                    newAmountPlus = currentAmountPlus.substring(0, equalIndex).trim() + "+" + addedAmount;
                } else {
                    newAmountPlus = currentAmountPlus + "+" + addedAmount;
                }
            }

            newAmountPlus += "=" + newAmount;

            ContentValues values = new ContentValues();
            values.put(AMOUNT, newAmount);
            values.put(AMOUNTPLUS, newAmountPlus);

            String selection = ID_COL + " = ?";
            String[] selectionArgs = {id};

            db.update(TABLE_NAME, values, selection, selectionArgs);

            if (newAmount == 107) {
                incrementStatCount(name, STAT_107);
            }
        }
        cursor.close();
        db.close();
    }

    public void recordTurnEliminations(List<Integer> newlyEliminatedIds) {
        if (newlyEliminatedIds == null || newlyEliminatedIds.isEmpty()) return;

        SQLiteDatabase db = this.getWritableDatabase();
        int maxOrder = 0;
        Cursor cursor = db.rawQuery("SELECT MAX(" + ELIM_ORDER + ") FROM " + TABLE_NAME, null);
        if (cursor.moveToFirst()) {
            maxOrder = cursor.getInt(0);
        }
        cursor.close();

        int nextOrder = maxOrder + 1;
        ContentValues values = new ContentValues();
        values.put(ELIM_ORDER, nextOrder);

        for (int id : newlyEliminatedIds) {
            db.update(TABLE_NAME, values, ID_COL + " = ? AND " + ELIM_ORDER + " = 0", new String[]{String.valueOf(id)});
        }
        db.close();
    }

    public ArrayList<ItemModel> readCourses() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursorCourses = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        ArrayList<ItemModel> courseModalArrayList = new ArrayList<>();

        if (cursorCourses.moveToFirst()) {
            do {
                int id = Integer.parseInt(cursorCourses.getString(0));
                String name = cursorCourses.getString(1);
                String amount = cursorCourses.getString(2);
                String amountplus = cursorCourses.getString(3);

                courseModalArrayList.add(new ItemModel(id, name, amount, amountplus));
            } while (cursorCourses.moveToNext());
        }
        cursorCourses.close();

        courseModalArrayList.sort((item1, item2) -> {
            int amount1 = 0, amount2 = 0;
            try { amount1 = Integer.parseInt(item1.getAmount()); } catch (Exception ignored) {}
            try { amount2 = Integer.parseInt(item2.getAmount()); } catch (Exception ignored) {}

            if (amount1 != amount2) {
                return Integer.compare(amount2, amount1);
            }
            if (!lastWinnerName.isEmpty()) {
                if (item1.getName().equals(lastWinnerName)) return -1;
                if (item2.getName().equals(lastWinnerName)) return 1;
            }
            return 0;
        });

        return courseModalArrayList;
    }

    public void deleteAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
        db.execSQL("DELETE FROM sqlite_sequence WHERE name='" + TABLE_NAME + "'");
        lastWinnerName = "";
        db.close();
    }

    public void deleteSelect(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, ID_COL + " = ?", new String[]{id});
        db.close();
    }

    public void incrementStatCount(String playerName, String column) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + column + " FROM " + STAT_TABLE + " WHERE " + STAT_NAME + " = ?", new String[]{playerName});
        if (cursor.moveToFirst()) {
            int currentVal = cursor.getInt(0);
            ContentValues values = new ContentValues();
            values.put(column, currentVal + 1);
            db.update(STAT_TABLE, values, STAT_NAME + " = ?", new String[]{playerName});
        } else {
            ContentValues values = new ContentValues();
            values.put(STAT_NAME, playerName);
            values.put(column, 1);
            db.insert(STAT_TABLE, null, values);
        }
        cursor.close();
        db.close();
    }

    public static class PlayerGameStatus {
        public String name;
        public int amount;
        public String amountPlus;
        public int elimOrder;
        public int computedRank;

        public PlayerGameStatus(String name, int amount, String amountPlus, int elimOrder) {
            this.name = name;
            this.amount = amount;
            this.amountPlus = amountPlus;
            this.elimOrder = elimOrder;
        }
    }

    public static Map<Integer, Integer> parseRankMap(String str) {
        Map<Integer, Integer> map = new HashMap<>();
        if (str == null || str.trim().isEmpty()) return map;
        String[] parts = str.split(",");
        for (String part : parts) {
            String[] kv = part.split(":");
            if (kv.length == 2) {
                try {
                    int rank = Integer.parseInt(kv[0]);
                    int count = Integer.parseInt(kv[1]);
                    map.put(rank, count);
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    public static String serializeRankMap(Map<Integer, Integer> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }

    public void finishGameAndRecordStats(List<ItemModel> currentPlayers) {
        if (currentPlayers == null || currentPlayers.isEmpty()) return;

        SQLiteDatabase db = this.getWritableDatabase();

        List<PlayerGameStatus> statuses = new ArrayList<>();
        Cursor cursorActive = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        if (cursorActive.moveToFirst()) {
            do {
                String name = cursorActive.getString(cursorActive.getColumnIndexOrThrow(NAME));
                int amt = 0;
                try {
                    amt = Integer.parseInt(cursorActive.getString(cursorActive.getColumnIndexOrThrow(AMOUNT)));
                } catch (Exception ignored) {}
                String amtPlus = cursorActive.getString(cursorActive.getColumnIndexOrThrow(AMOUNTPLUS));
                if (amtPlus == null) amtPlus = "";
                int order = cursorActive.getInt(cursorActive.getColumnIndexOrThrow(ELIM_ORDER));
                statuses.add(new PlayerGameStatus(name, amt, amtPlus, order));
            } while (cursorActive.moveToNext());
        }
        cursorActive.close();

        if (statuses.isEmpty()) {
            db.close();
            return;
        }

        List<PlayerGameStatus> activePlayers = new ArrayList<>();
        List<PlayerGameStatus> eliminatedPlayers = new ArrayList<>();

        for (PlayerGameStatus p : statuses) {
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
                if (order1 == 0) order1 = 999;
                if (order2 == 0) order2 = 999;
                if (order1 != order2) {
                    return Integer.compare(order2, order1);
                }
            }
            return Integer.compare(p1.amount, p2.amount);
        });

        int currentRank = 1;
        String winnerName = "Аниқланмади";

        for (PlayerGameStatus p : activePlayers) {
            p.computedRank = currentRank++;
            if (p.computedRank == 1) {
                winnerName = p.name;
            }
        }

        for (PlayerGameStatus p : eliminatedPlayers) {
            p.computedRank = currentRank++;
            if (winnerName.equals("Аниқланмади") && p.computedRank == 1) {
                winnerName = p.name;
            }
        }

        // Build history details
        String dateStr = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());
        StringBuilder detailsBuilder = new StringBuilder();

        for (PlayerGameStatus p : statuses) {
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

        // Update player_stats table
        for (PlayerGameStatus p : statuses) {
            boolean isWin = (p.computedRank == 1);

            Cursor cursor = db.rawQuery("SELECT * FROM " + STAT_TABLE + " WHERE " + STAT_NAME + " = ?", new String[]{p.name});
            if (cursor.moveToFirst()) {
                int wins = cursor.getInt(cursor.getColumnIndexOrThrow(STAT_WINS));
                int losses = cursor.getInt(cursor.getColumnIndexOrThrow(STAT_LOSSES));
                int games = cursor.getInt(cursor.getColumnIndexOrThrow(STAT_GAMES));
                int points = cursor.getInt(cursor.getColumnIndexOrThrow(STAT_POINTS));
                int rankSum = cursor.getInt(cursor.getColumnIndexOrThrow(STAT_RANK_SUM));
                String oldRankCounts = cursor.getString(cursor.getColumnIndexOrThrow(STAT_RANK_COUNTS));

                Map<Integer, Integer> rankMap = parseRankMap(oldRankCounts);
                rankMap.put(p.computedRank, rankMap.getOrDefault(p.computedRank, 0) + 1);
                String newRankCounts = serializeRankMap(rankMap);

                ContentValues values = new ContentValues();
                values.put(STAT_WINS, isWin ? wins + 1 : wins);
                values.put(STAT_LOSSES, !isWin ? losses + 1 : losses);
                values.put(STAT_GAMES, games + 1);
                values.put(STAT_POINTS, points + p.amount);
                values.put(STAT_RANK_SUM, rankSum + p.computedRank);
                values.put(STAT_RANK_COUNTS, newRankCounts);

                db.update(STAT_TABLE, values, STAT_NAME + " = ?", new String[]{p.name});
            } else {
                Map<Integer, Integer> rankMap = new HashMap<>();
                rankMap.put(p.computedRank, 1);
                String newRankCounts = serializeRankMap(rankMap);

                ContentValues values = new ContentValues();
                values.put(STAT_NAME, p.name);
                values.put(STAT_WINS, isWin ? 1 : 0);
                values.put(STAT_LOSSES, !isWin ? 1 : 0);
                values.put(STAT_GAMES, 1);
                values.put(STAT_POINTS, p.amount);
                values.put(STAT_RANK_SUM, p.computedRank);
                values.put(STAT_RANK_COUNTS, newRankCounts);

                db.insert(STAT_TABLE, null, values);
            }
            cursor.close();
        }

        // Save game history entry
        ContentValues histValues = new ContentValues();
        histValues.put(HIST_DATE, dateStr);
        histValues.put(HIST_WINNER, winnerName);
        histValues.put(HIST_DETAILS, detailsBuilder.toString().trim());
        db.insert(HISTORY_TABLE, null, histValues);

        // Find 1st place winner
        for (PlayerGameStatus p : statuses) {
            if (p.computedRank == 1) {
                lastWinnerName = p.name;
                break;
            }
        }

        // Reset scores to 0 for new match in SQLite without deleting players
        ContentValues resetValues = new ContentValues();
        resetValues.put(AMOUNT, "0");
        resetValues.put(AMOUNTPLUS, "0");
        resetValues.put(ELIM_ORDER, 0);
        db.update(TABLE_NAME, resetValues, null, null);

        db.close();
    }

    public ArrayList<StatModel> getPlayerStats() {
        ArrayList<StatModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + STAT_TABLE + " ORDER BY " + STAT_WINS + " DESC, " + STAT_GAMES + " ASC", null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(STAT_NAME));
                int wins = cursor.getInt(cursor.getColumnIndexOrThrow(STAT_WINS));
                int losses = cursor.getInt(cursor.getColumnIndexOrThrow(STAT_LOSSES));
                int games = cursor.getInt(cursor.getColumnIndexOrThrow(STAT_GAMES));
                int times107 = cursor.getInt(cursor.getColumnIndexOrThrow(STAT_107));
                int times77 = cursor.getInt(cursor.getColumnIndexOrThrow(STAT_77));
                int totalPoints = cursor.getInt(cursor.getColumnIndexOrThrow(STAT_POINTS));
                int rankSum = cursor.getInt(cursor.getColumnIndexOrThrow(STAT_RANK_SUM));
                String rankCountsStr = cursor.getString(cursor.getColumnIndexOrThrow(STAT_RANK_COUNTS));

                Map<Integer, Integer> rankMap = parseRankMap(rankCountsStr);
                list.add(new StatModel(name, wins, losses, games, times107, times77, totalPoints, rankSum, rankMap));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public ArrayList<GameHistoryModel> getGameHistory() {
        ArrayList<GameHistoryModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + HISTORY_TABLE + " ORDER BY " + HIST_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(HIST_ID));
                String dateStr = cursor.getString(cursor.getColumnIndexOrThrow(HIST_DATE));
                String winnerName = cursor.getString(cursor.getColumnIndexOrThrow(HIST_WINNER));
                String details = cursor.getString(cursor.getColumnIndexOrThrow(HIST_DETAILS));

                list.add(new GameHistoryModel(id, dateStr, winnerName, details));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}
