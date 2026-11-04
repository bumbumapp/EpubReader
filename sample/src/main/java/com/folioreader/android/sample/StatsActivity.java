package com.folioreader.android.sample;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.folioreader.stats.ReaderStats;
import com.folioreader.stats.ReadingLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/** Reading statistics: today against the daily goal, streak, totals, last 7 days and time per book. */
public class StatsActivity extends AppCompatActivity {

    private static final int[] GOALS = {10, 20, 30, 45, 60, 90};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.stats_goal_change).setOnClickListener(v -> chooseGoal());
    }

    @Override
    protected void onResume() {
        super.onResume();
        bind();
    }

    private void chooseGoal() {
        String[] labels = new String[GOALS.length];
        int checked = -1;
        int current = ReaderStats.goalMinutes(this);
        for (int i = 0; i < GOALS.length; i++) {
            labels[i] = getString(R.string.stats_goal_option, GOALS[i]);
            if (GOALS[i] == current) {
                checked = i;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.stats_goal_dialog)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    ReaderStats.setGoalMinutes(this, GOALS[which]);
                    d.dismiss();
                    bind();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void bind() {
        Map<Long, Long> days = ReaderStats.days(this);
        long today = ReaderStats.today();
        long todayMs = ReadingLog.get(days, today);
        int goal = ReaderStats.goalMinutes(this);

        ((TextView) findViewById(R.id.stats_today)).setText(ReadingLog.format(todayMs));
        ProgressBar bar = findViewById(R.id.stats_goal_bar);
        bar.setProgress((int) Math.min(100, todayMs * 100 / (goal * 60_000L)));
        ((TextView) findViewById(R.id.stats_goal_text)).setText(todayMs >= goal * 60_000L
                ? getString(R.string.stats_goal_reached) : getString(R.string.stats_goal_text, goal));
        ((TextView) findViewById(R.id.stats_streak)).setText(String.valueOf(ReadingLog.streak(days, today)));
        ((TextView) findViewById(R.id.stats_total)).setText(ReadingLog.format(ReadingLog.total(days)));
        ((WeekBarsView) findViewById(R.id.stats_week)).setValues(ReadingLog.lastDays(days, today, 7));

        Map<String, String> titles = LibraryMeta.titles(this);
        int finished = 0;
        for (String key : titles.keySet()) {
            if (ReaderStats.progress(this, key) >= 1f) {
                finished++;
            }
        }
        ((TextView) findViewById(R.id.stats_finished)).setText(String.valueOf(finished));

        LinearLayout list = findViewById(R.id.stats_books);
        list.removeAllViews();
        ArrayList<Map.Entry<String, Long>> byBook = new ArrayList<>(ReaderStats.bookMs(this).entrySet());
        Collections.sort(byBook, (a, b) -> Long.compare(b.getValue(), a.getValue()));
        int shown = 0;
        for (Map.Entry<String, Long> e : byBook) {
            if (!titles.containsKey(e.getKey()) || shown >= 8) {
                continue;
            }
            shown++;
            list.addView(row(titles.get(e.getKey()), ReadingLog.format(e.getValue())));
        }
        if (shown == 0) {
            list.addView(row(getString(R.string.stats_no_books), ""));
        }
    }

    private View row(String left, String right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        TextView l = new TextView(this);
        l.setText(left);
        l.setMaxLines(2);
        l.setEllipsize(android.text.TextUtils.TruncateAt.END);
        l.setTextSize(14);
        l.setTextColor(getResources().getColor(R.color.brand_text, getTheme()));
        l.setTypeface(ResourcesCompat.getFont(this, R.font.jakarta_medium));
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView r = new TextView(this);
        r.setText(right);
        r.setTextSize(14);
        r.setTextColor(getResources().getColor(R.color.brand_accent, getTheme()));
        r.setTypeface(ResourcesCompat.getFont(this, R.font.jakarta_bold));
        row.addView(r);
        return row;
    }
}
