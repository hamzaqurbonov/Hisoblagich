package com.example.hisob;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class StatisticsActivity extends AppCompatActivity {
    private RecyclerView rvStatistics, rvGameHistory;
    private StatAdapter statAdapter;
    private GameHistoryAdapter historyAdapter;
    private DbSQL dbSQL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        Toolbar toolbar = findViewById(R.id.toolbarStats);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Статистика ва Тарих");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvStatistics = findViewById(R.id.rvStatistics);
        rvGameHistory = findViewById(R.id.rvGameHistory);

        rvStatistics.setLayoutManager(new LinearLayoutManager(this));
        rvGameHistory.setLayoutManager(new LinearLayoutManager(this));

        dbSQL = new DbSQL(this);

        ArrayList<StatModel> statList = dbSQL.getPlayerStats();
        statAdapter = new StatAdapter(statList);
        rvStatistics.setAdapter(statAdapter);

        ArrayList<GameHistoryModel> historyList = dbSQL.getGameHistory();
        historyAdapter = new GameHistoryAdapter(historyList);
        rvGameHistory.setAdapter(historyAdapter);
    }
}
