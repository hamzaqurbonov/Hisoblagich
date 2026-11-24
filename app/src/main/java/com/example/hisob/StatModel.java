package com.example.hisob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StatModel {
    private String name;
    private int wins;
    private int losses;
    private int gamesPlayed;
    private int times107;
    private int times77;
    private int totalPoints;
    private int rankSum;
    private Map<Integer, Integer> rankCountsMap;

    public StatModel(String name, int wins, int losses, int gamesPlayed, int times107, int times77, int totalPoints, int rankSum, Map<Integer, Integer> rankCountsMap) {
        this.name = name;
        this.wins = wins;
        this.losses = losses;
        this.gamesPlayed = gamesPlayed;
        this.times107 = times107;
        this.times77 = times77;
        this.totalPoints = totalPoints;
        this.rankSum = rankSum;
        this.rankCountsMap = rankCountsMap;
    }

    public String getName() {
        return name;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getTimes107() {
        return times107;
    }

    public int getTimes77() {
        return times77;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public String getRankBreakdownStr() {
        if (rankCountsMap == null || rankCountsMap.isEmpty()) {
            return "Маълумот йўқ";
        }
        List<Integer> sortedRanks = new ArrayList<>(rankCountsMap.keySet());
        Collections.sort(sortedRanks);

        StringBuilder sb = new StringBuilder();
        for (int r : sortedRanks) {
            Integer count = rankCountsMap.get(r);
            if (count != null && count > 0) {
                if (sb.length() > 0) sb.append("  |  ");
                sb.append(r).append("-ўрин: ").append(count);
            }
        }
        return sb.length() > 0 ? sb.toString() : "Маълумот йўқ";
    }

    public int getWinRate() {
        if (gamesPlayed == 0) return 0;
        int win1stCount = 0;
        if (rankCountsMap != null && rankCountsMap.containsKey(1)) {
            Integer count = rankCountsMap.get(1);
            if (count != null) win1stCount = count;
        } else {
            win1stCount = wins;
        }
        return (int) Math.round(((double) win1stCount / gamesPlayed) * 100);
    }
}
