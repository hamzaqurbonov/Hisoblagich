package com.example.hisob;

public class GameHistoryModel {
    private int id;
    private String dateStr;

    private String winnerName;
    private String details;

    public GameHistoryModel(int id, String dateStr, String winnerName, String details) {
        this.id = id;
        this.dateStr = dateStr;
        this.winnerName = winnerName;
        this.details = details;
    }

    public int getId() {
        return id;
    }

    public String getDateStr() {
        return dateStr;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public String getDetails() {
        return details;
    }
}
