package com.ivan.rotapago;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "rotapago_finance_prefs";
    private static final String PREF_DARK = "dark_theme";

    private final String[] MONTHS = {
            "январь","февраль","март","апрель","май","июнь",
            "июль","август","сентябрь","октябрь","ноябрь","декабрь"
    };
    private final String[] MONTHS_SHORT = {
            "янв","фев","мар","апр","май","июн",
            "июл","авг","сен","окт","ноя","дек"
    };

    private Db db;
    private SharedPreferences prefs;
    private boolean dark;
    private int shownYear;
    private int shownMonth;
    private Palette p;

    private final DecimalFormat money = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new Db(this);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        dark = prefs.getBoolean(PREF_DARK, false);
        Calendar now = Calendar.getInstance();
        shownYear = now.get(Calendar.YEAR);
        shownMonth = now.get(Calendar.MONTH);
        renderDashboard();
    }

    private void renderDashboard() {
        p = dark ? Palette.dark() : Palette.light();
        applyWindow();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(p.bg);

        LinearLayout root = vCol();
        root.setPadding(dp(18), dp(14), dp(18), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(buildHeader());
        root.addView(spacer(14));
        root.addView(buildMonthNavigation());
        root.addView(spacer(14));
        root.addView(buildBalanceCard());
        root.addView(spacer(12));
        root.addView(buildSummaryCards());
        root.addView(spacer(12));
        root.addView(buildRecentCard());
        root.addView(spacer(14));
        root.addView(buildAddButtons());
        root.addView(spacer(10));
        root.addView(buildBottomButtons());

        setContentView(scroll);
    }

    private void applyWindow() {
        Window w = getWindow();
        w.setStatusBarColor(p.bg);
        w.setNavigationBarColor(p.bg);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            w.getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private View buildHeader() {
        LinearLayout row = hRow();
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = vCol();
        titles.addView(text("RotaPago", 34, p.text, true));
        titles.addView(text("Доходы и расходы", 17, p.muted, false));
        row.addView(titles, weighted(1f));

        Button theme = button(dark ? "☀ Светлая" : "☾ Тёмная", p.primarySoft, p.primary, 14, 18);
        theme.setOnClickListener(v -> {
            dark = !dark;
            prefs.edit().putBoolean(PREF_DARK, dark).apply();
            renderDashboard();
        });
        row.addView(theme, wrap());
        return row;
    }

    private View buildMonthNavigation() {
        LinearLayout row = hRow();
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button left = button("‹", p.surface2, p.text, 28, 14);
        left.setOnClickListener(v -> changeMonth(-1));
        row.addView(left, fixed(dp(58), dp(58)));

        TextView label = text(MONTHS[shownMonth] + " " + shownYear, 24, p.text, true);
        label.setGravity(Gravity.CENTER);
        label.setOnClickListener(v -> pickMonth());
        LinearLayout.LayoutParams center = weighted(1f);
        center.setMargins(dp(10), 0, dp(10), 0);
        row.addView(label, center);

        Button right = button("›", p.surface2, p.text, 28, 14);
        right.setOnClickListener(v -> changeMonth(1));
        row.addView(right, fixed(dp(58), dp(58)));
        return row;
    }

    private View buildBalanceCard() {
        double[] totals = db.monthTotals(shownYear, shownMonth);
        double monthNet = totals[0] - totals[1];
        double balance = db.allTimeBalance();

        LinearLayout card = card(p.surface, 22);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));
        card.addView(text("Текущий баланс", 16, p.muted, true));
        TextView value = text(formatMoneyPlain(balance), 40, p.text, true);
        value.setPadding(0, dp(3), 0, dp(2));
        card.addView(value);

        LinearLayout deltaRow = hRow();
        deltaRow.setGravity(Gravity.CENTER_VERTICAL);
        deltaRow.addView(text((monthNet >= 0 ? "↑  +" : "↓  -") + formatMoneyPlain(Math.abs(monthNet)),
                17, monthNet >= 0 ? p.income : p.expense, true));
        TextView cap = text("   за этот месяц", 16, p.muted, false);
        deltaRow.addView(cap);
        card.addView(deltaRow);
        return card;
    }

    private View buildSummaryCards() {
        double[] totals = db.monthTotals(shownYear, shownMonth);
        int[] counts = db.monthCounts(shownYear, shownMonth);

        LinearLayout row = hRow();
        LinearLayout.LayoutParams a = weighted(1f);
        a.setMargins(0, 0, dp(6), 0);
        LinearLayout.LayoutParams b = weighted(1f);
        b.setMargins(dp(6), 0, 0, 0);

        row.addView(miniStat("↑", "Доходы", totals[0], counts[0], true), a);
        row.addView(miniStat("↓", "Расходы", totals[1], counts[1], false), b);
        return row;
    }

    private View miniStat(String icon, String title, double amount, int count, boolean income) {
        int tint = income ? p.income : p.expense;
        int soft = income ? p.incomeSoft : p.expenseSoft;
        LinearLayout card = card(soft, 20);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setMinimumHeight(dp(120));

        LinearLayout row = hRow();
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = text(icon, 28, Color.WHITE, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(tint, 999));
        row.addView(badge, fixed(dp(52), dp(52)));

        LinearLayout info = vCol();
        info.setPadding(dp(12), 0, 0, 0);
        info.addView(text(title, 17, p.text, true));
        info.addView(text(formatMoneyPlain(amount), 23, tint, true));
        info.addView(text(count + " " + operationWord(count), 14, p.muted, false));
        row.addView(info, weighted(1f));

        card.addView(row);
        return card;
    }

    private View buildRecentCard() {
        LinearLayout card = card(p.surface, 22);
        card.setPadding(dp(16), dp(14), dp(16), dp(10));

        LinearLayout head = hRow();
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text("Последние операции", 20, p.text, true), weighted(1f));
        TextView all = text("Все операции  ›", 14, p.primary, true);
        all.setPadding(dp(8), dp(8), 0, dp(8));
        all.setOnClickListener(v -> showAllTransactions());
        head.addView(all);
        card.addView(head);

        List<Tx> list = db.transactionsForMonth(shownYear, shownMonth, 7);
        if (list.isEmpty()) {
            TextView empty = text("Пока нет операций за этот месяц.\nДобавьте первый доход или расход ниже.", 15, p.muted, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(8), dp(28), dp(8), dp(28));
            card.addView(empty);
            return card;
        }

        for (int i = 0; i < list.size(); i++) {
            card.addView(transactionRow(list.get(i)));
            if (i < list.size() - 1) card.addView(divider());
        }
        return card;
    }

    private View transactionRow(Tx tx) {
        LinearLayout row = hRow();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), dp(11), dp(2), dp(11));

        boolean income = "income".equals(tx.type);
        int tint = income ? p.income : p.expense;
        TextView badge = text(categoryIcon(tx.category, income), 21, tint, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(income ? p.incomeSoft : p.expenseSoft, 999));
        row.addView(badge, fixed(dp(46), dp(46)));

        LinearLayout info = vCol();
        info.setPadding(dp(12), 0, dp(8), 0);
        info.addView(text(tx.category, 16, p.text, true));

        String detail = displayDate(tx.date);
        if (tx.note != null && tx.note.trim().length() > 0) detail += "  ·  " + tx.note.trim();
        TextView sub = text(detail, 13, p.muted, false);
        sub.setSingleLine(true);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(sub);
        row.addView(info, weighted(1f));

        TextView amount = text((income ? "+" : "-") + formatMoneyPlain(tx.amount), 17, tint, true);
        amount.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(amount, wrap());

        row.setOnClickListener(v -> showTransactionActions(tx));
        return row;
    }

    private View buildAddButtons() {
        LinearLayout row = hRow();
        Button in = button("＋  Добавить доход", dark ? p.income : p.primary, Color.WHITE, 16, 18);
        Button out = button("＋  Добавить расход", p.expense, Color.WHITE, 16, 18);
        in.setOnClickListener(v -> showTransactionDialog("income", null));
        out.setOnClickListener(v -> showTransactionDialog("expense", null));

        LinearLayout.LayoutParams a = weighted(1f);
        a.height = dp(64);
        a.setMargins(0, 0, dp(6), 0);
        LinearLayout.LayoutParams b = weighted(1f);
        b.height = dp(64);
        b.setMargins(dp(6), 0, 0, 0);
        row.addView(in, a);
        row.addView(out, b);
        return row;
    }

    private View buildBottomButtons() {
        LinearLayout col = vCol();

        LinearLayout row = hRow();
        Button reports = button("▰  Аналитика", p.surface2, p.text, 16, 18);
        Button cats = button("▦  Категории", p.surface2, p.text, 16, 18);
        reports.setOnClickListener(v -> renderAnalytics());
        cats.setOnClickListener(v -> showCategories());

        LinearLayout.LayoutParams a = weighted(1f);
        a.height = dp(58);
        a.setMargins(0, 0, dp(6), 0);
        LinearLayout.LayoutParams b = weighted(1f);
        b.height = dp(58);
        b.setMargins(dp(6), 0, 0, 0);
        row.addView(reports, a);
        row.addView(cats, b);
        col.addView(row);

        col.addView(spacer(10));

        Button export = button("⇧  Экспорт в Google Таблицы", p.primarySoft, p.primary, 16, 18);
        export.setOnClickListener(v -> showExportMenu());
        col.addView(export, fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));
        return col;
    }

    private void renderAnalytics() {
        p = dark ? Palette.dark() : Palette.light();
        applyWindow();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(p.bg);

        LinearLayout root = vCol();
        root.setPadding(dp(18), dp(14), dp(18), dp(28));
        scroll.addView(root);

        LinearLayout header = hRow();
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", p.surface2, p.text, 28, 14);
        back.setOnClickListener(v -> renderDashboard());
        header.addView(back, fixed(dp(54), dp(54)));
        LinearLayout titles = vCol();
        titles.setPadding(dp(12), 0, 0, 0);
        titles.addView(text("Аналитика", 30, p.text, true));
        titles.addView(text("Доходы, расходы и динамика", 15, p.muted, false));
        header.addView(titles, weighted(1f));
        root.addView(header);

        root.addView(spacer(14));
        root.addView(buildMonthNavigationAnalytics());
        root.addView(spacer(14));

        MonthPoint[] six = db.lastMonths(shownYear, shownMonth, 6);
        double[] totals = db.monthTotals(shownYear, shownMonth);
        double net = totals[0] - totals[1];

        LinearLayout kpi = hRow();
        kpi.addView(kpiCard("Доходы", formatMoneyPlain(totals[0]), p.income), weightMargin(1f, 0, 5));
        kpi.addView(kpiCard("Расходы", formatMoneyPlain(totals[1]), p.expense), weightMargin(1f, 5, 5));
        kpi.addView(kpiCard("Итог", (net >= 0 ? "+" : "-") + formatMoneyPlain(Math.abs(net)), net >= 0 ? p.income : p.expense), weightMargin(1f, 5, 0));
        root.addView(kpi);

        root.addView(spacer(14));
        root.addView(chartCard("Доходы и расходы за 6 месяцев",
                new FinanceChartView(this, FinanceChartView.MODE_BARS, six, null, p)));
        root.addView(spacer(12));
        root.addView(chartCard("Динамика результата по месяцам",
                new FinanceChartView(this, FinanceChartView.MODE_LINE, six, null, p)));
        root.addView(spacer(12));

        List<CategoryTotal> expenses = db.categoryTotals(shownYear, shownMonth, "expense");
        root.addView(chartCard("Структура расходов · " + MONTHS[shownMonth],
                new FinanceChartView(this, FinanceChartView.MODE_PIE, null, expenses, p)));

        root.addView(spacer(12));
        root.addView(buildAnalyticsDetails());

        setContentView(scroll);
    }

    private View buildMonthNavigationAnalytics() {
        LinearLayout row = hRow();
        row.setGravity(Gravity.CENTER_VERTICAL);
        Button left = button("‹", p.surface2, p.text, 26, 14);
        Button right = button("›", p.surface2, p.text, 26, 14);
        TextView label = text(MONTHS[shownMonth] + " " + shownYear, 20, p.text, true);
        label.setGravity(Gravity.CENTER);
        left.setOnClickListener(v -> { changeMonthOnly(-1); renderAnalytics(); });
        right.setOnClickListener(v -> { changeMonthOnly(1); renderAnalytics(); });
        label.setOnClickListener(v -> pickMonthForAnalytics());
        row.addView(left, fixed(dp(54), dp(54)));
        row.addView(label, weighted(1f));
        row.addView(right, fixed(dp(54), dp(54)));
        return row;
    }

    private View kpiCard(String label, String value, int accent) {
        LinearLayout card = card(p.surface, 18);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.addView(text(label, 13, p.muted, true));
        card.addView(text(value, 18, accent, true));
        return card;
    }

    private LinearLayout.LayoutParams weightMargin(float weight, int left, int right) {
        LinearLayout.LayoutParams lp = weighted(weight);
        lp.setMargins(dp(left), 0, dp(right), 0);
        return lp;
    }

    private View chartCard(String title, FinanceChartView chart) {
        LinearLayout card = card(p.surface, 22);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(text(title, 18, p.text, true));
        TextView hint = text(chart.mode == FinanceChartView.MODE_PIE ? "Доля категорий" :
                chart.mode == FinanceChartView.MODE_LINE ? "Чистый итог: доходы минус расходы" : "Зелёный — доходы · красный — расходы",
                13, p.muted, false);
        hint.setPadding(0, dp(3), 0, dp(8));
        card.addView(hint);
        card.addView(chart, fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(250)));
        return card;
    }

    private View buildAnalyticsDetails() {
        LinearLayout card = card(p.surface, 22);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(text("Ключевые показатели", 18, p.text, true));

        double[] totals = db.monthTotals(shownYear, shownMonth);
        double avg = totals[1] / daysInMonth(shownYear, shownMonth);
        Tx biggest = db.biggestTransaction(shownYear, shownMonth, "expense");
        Tx biggestIncome = db.biggestTransaction(shownYear, shownMonth, "income");

        card.addView(metricRow("Средний расход в день", formatMoneyPlain(avg)));
        card.addView(metricRow("Самая большая трата",
                biggest == null ? "—" : biggest.category + " · " + formatMoneyPlain(biggest.amount)));
        card.addView(metricRow("Самый большой доход",
                biggestIncome == null ? "—" : biggestIncome.category + " · " + formatMoneyPlain(biggestIncome.amount)));

        List<CategoryTotal> cats = db.categoryTotals(shownYear, shownMonth, "expense");
        if (!cats.isEmpty()) card.addView(metricRow("Главная категория расходов",
                cats.get(0).category + " · " + formatMoneyPlain(cats.get(0).total)));
        return card;
    }

    private View metricRow(String label, String value) {
        LinearLayout row = hRow();
        row.setPadding(0, dp(10), 0, dp(10));
        row.addView(text(label, 14, p.muted, false), weighted(1f));
        TextView v = text(value, 14, p.text, true);
        v.setGravity(Gravity.RIGHT);
        row.addView(v, wrap());
        return row;
    }

    private void showTransactionActions(Tx tx) {
        String[] actions = {"✎  Редактировать", "🗑  Удалить"};
        new AlertDialog.Builder(this, dark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setTitle(tx.category + " · " + formatMoneyPlain(tx.amount))
                .setItems(actions, (d, which) -> {
                    if (which == 0) showTransactionDialog(tx.type, tx);
                    else confirmDeleteTransaction(tx);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showTransactionDialog(String initialType, Tx existing) {
        final Calendar selected = Calendar.getInstance();
        if (existing != null) setCalendarFromIso(selected, existing.date);
        else {
            selected.set(Calendar.YEAR, shownYear);
            selected.set(Calendar.MONTH, shownMonth);
            selected.set(Calendar.DAY_OF_MONTH, Math.min(selected.get(Calendar.DAY_OF_MONTH), daysInMonth(shownYear, shownMonth)));
        }

        LinearLayout box = dialogBox();

        Spinner type = new Spinner(this);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Расход", "Доход"});
        type.setAdapter(typeAdapter);
        type.setSelection("income".equals(initialType) ? 1 : 0);

        box.addView(label("Тип операции"));
        box.addView(type, lpMatchWrap());

        box.addView(spacer(10));
        box.addView(label("Сумма"));
        EditText amount = field(existing == null ? "" : money.format(existing.amount), "Сумма, €",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setTextSize(28);
        amount.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        box.addView(amount, lpMatchWrap());

        box.addView(spacer(10));
        box.addView(label("Категория"));
        Spinner category = new Spinner(this);
        box.addView(category, lpMatchWrap());

        final String[] currentDbType = {type.getSelectedItemPosition() == 1 ? "income" : "expense"};
        final Runnable refillCategories = () -> {
            currentDbType[0] = type.getSelectedItemPosition() == 1 ? "income" : "expense";
            List<String> cats = db.categories(currentDbType[0]);
            ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats);
            category.setAdapter(ad);
            if (existing != null) {
                for (int i = 0; i < cats.size(); i++) {
                    if (cats.get(i).equals(existing.category)) {
                        category.setSelection(i);
                        break;
                    }
                }
            }
        };
        refillCategories.run();
        type.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                refillCategories.run();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        box.addView(spacer(10));
        box.addView(label("Дата"));
        Button date = button(displayDate(isoDate(selected)), p.surface2, p.text, 15, 14);
        date.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        date.setOnClickListener(v -> new DatePickerDialog(this, (DatePicker view, int y, int m, int day) -> {
            selected.set(Calendar.YEAR, y);
            selected.set(Calendar.MONTH, m);
            selected.set(Calendar.DAY_OF_MONTH, day);
            date.setText(displayDate(isoDate(selected)));
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show());
        box.addView(date, fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        box.addView(spacer(10));
        box.addView(label("Комментарий"));
        EditText note = field(existing == null ? "" : safe(existing.note), "Например: Continente, штраф, аренда…",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        note.setSingleLine(false);
        note.setMinLines(2);
        box.addView(note, lpMatchWrap());

        AlertDialog dialog = new AlertDialog.Builder(this, dark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setTitle(existing == null ? "Новая операция" : "Редактировать операцию")
                .setView(box)
                .setNegativeButton("Отмена", null)
                .setPositiveButton(existing == null ? "Сохранить" : "Сохранить изменения", null)
                .create();

        dialog.setOnShowListener(x -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor("income".equals(initialType) ? p.income : p.primary);
            save.setOnClickListener(v -> {
                String s = amount.getText().toString().trim().replace(',', '.');
                if (s.length() == 0) {
                    amount.setError("Введите сумму");
                    return;
                }
                double value;
                try {
                    value = Double.parseDouble(s);
                } catch (Exception e) {
                    amount.setError("Неверная сумма");
                    return;
                }
                if (value <= 0) {
                    amount.setError("Сумма должна быть больше нуля");
                    return;
                }
                if (category.getSelectedItem() == null) {
                    toast("Добавьте категорию");
                    return;
                }
                String dbType = type.getSelectedItemPosition() == 1 ? "income" : "expense";
                String cat = category.getSelectedItem().toString();
                String n = note.getText().toString().trim();
                String dateIso = isoDate(selected);

                if (existing == null) db.addTransaction(dbType, value, cat, n, dateIso);
                else db.updateTransaction(existing.id, dbType, value, cat, n, dateIso);

                shownYear = selected.get(Calendar.YEAR);
                shownMonth = selected.get(Calendar.MONTH);
                dialog.dismiss();
                renderDashboard();
            });
        });

        dialog.show();
        amount.requestFocus();
    }

    private void confirmDeleteTransaction(Tx tx) {
        new AlertDialog.Builder(this, dark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setTitle("Удалить операцию?")
                .setMessage(tx.category + " · " + displayDate(tx.date) + " · " + formatMoneyPlain(tx.amount))
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", (d, w) -> {
                    db.deleteTransaction(tx.id);
                    renderDashboard();
                }).show();
    }

    private void showAllTransactions() {
        List<Tx> list = db.transactionsForMonth(shownYear, shownMonth, 0);
        LinearLayout box = dialogBox();
        if (list.isEmpty()) {
            box.addView(text("Нет операций", 15, p.muted, false));
        } else {
            for (int i = 0; i < list.size(); i++) {
                box.addView(transactionRow(list.get(i)));
                if (i < list.size() - 1) box.addView(divider());
            }
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);
        AlertDialog d = new AlertDialog.Builder(this, dark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setTitle("Операции · " + MONTHS[shownMonth] + " " + shownYear)
                .setView(scroll)
                .setPositiveButton("Готово", null)
                .create();
        d.show();
    }

    private void showCategories() {
        LinearLayout box = dialogBox();
        box.addView(text("Доходы", 17, p.income, true));
        addCategoryRows(box, "income");
        box.addView(spacer(12));
        box.addView(text("Расходы", 17, p.expense, true));
        addCategoryRows(box, "expense");
        box.addView(spacer(14));

        Button add = button("＋ Добавить категорию", p.primarySoft, p.primary, 15, 16);
        box.addView(add, fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);
        AlertDialog d = new AlertDialog.Builder(this, dark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setTitle("Категории")
                .setView(scroll)
                .setPositiveButton("Готово", null)
                .create();
        add.setOnClickListener(v -> {
            d.dismiss();
            addCategoryDialog();
        });
        d.show();
    }

    private void addCategoryRows(LinearLayout box, String type) {
        List<String> cats = db.categories(type);
        for (String cat : cats) {
            LinearLayout row = hRow();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            row.addView(text(categoryIcon(cat, "income".equals(type)) + "  " + cat, 15, p.text, false), weighted(1f));
            TextView del = text("×", 22, p.muted, true);
            del.setGravity(Gravity.CENTER);
            del.setOnClickListener(v -> confirmDeleteCategory(type, cat));
            row.addView(del, fixed(dp(42), dp(42)));
            box.addView(row);
        }
    }

    private void addCategoryDialog() {
        LinearLayout box = dialogBox();
        Spinner type = new Spinner(this);
        type.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Расход", "Доход"}));
        EditText name = field("", "Например: Обучение", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        box.addView(label("Тип"));
        box.addView(type, lpMatchWrap());
        box.addView(spacer(10));
        box.addView(label("Название"));
        box.addView(name, lpMatchWrap());

        AlertDialog d = new AlertDialog.Builder(this, dark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setTitle("Новая категория").setView(box)
                .setNegativeButton("Отмена", (x, w) -> showCategories())
                .setPositiveButton("Добавить", null).create();

        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            if (n.length() < 2) {
                name.setError("Введите название");
                return;
            }
            String t = type.getSelectedItemPosition() == 0 ? "expense" : "income";
            if (!db.addCategory(t, n)) {
                name.setError("Такая категория уже есть");
                return;
            }
            d.dismiss();
            showCategories();
        }));
        d.show();
    }

    private void confirmDeleteCategory(String type, String cat) {
        new AlertDialog.Builder(this, dark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setTitle("Удалить категорию?")
                .setMessage(cat + "\nСтарые операции сохранят своё название категории.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", (d, w) -> {
                    db.deleteCategory(type, cat);
                    renderDashboard();
                    showCategories();
                }).show();
    }

    private void showExportMenu() {
        String[] items = {
                "Текущий месяц → Google Таблицы",
                "Все операции → Google Таблицы",
                "Текущий месяц → поделиться CSV",
                "Все операции → поделиться CSV"
        };
        new AlertDialog.Builder(this, dark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setTitle("Экспорт данных")
                .setItems(items, (d, which) -> {
                    boolean all = which == 1 || which == 3;
                    boolean preferSheets = which == 0 || which == 1;
                    exportCsv(all, preferSheets);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void exportCsv(boolean all, boolean preferSheets) {
        try {
            List<Tx> list = all ? db.allTransactions() : db.transactionsForMonth(shownYear, shownMonth, 0);
            if (list.isEmpty()) {
                toast("Нет данных для экспорта");
                return;
            }

            StringBuilder csv = new StringBuilder();
            csv.append('\uFEFF');
            csv.append("Дата,Тип,Категория,Сумма EUR,Комментарий\n");
            for (Tx tx : list) {
                csv.append(csvCell(tx.date)).append(",");
                csv.append(csvCell("income".equals(tx.type) ? "Доход" : "Расход")).append(",");
                csv.append(csvCell(tx.category)).append(",");
                csv.append(String.format(Locale.US, "%.2f", tx.amount)).append(",");
                csv.append(csvCell(safe(tx.note))).append("\n");
            }

            File dir = new File(getCacheDir(), "exports");
            if (!dir.exists()) dir.mkdirs();
            String name = all ? "RotaPago-all.csv" :
                    String.format(Locale.US, "RotaPago-%04d-%02d.csv", shownYear, shownMonth + 1);
            File file = new File(dir, name);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            }

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/csv");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (preferSheets) {
                try {
                    send.setPackage("com.google.android.apps.docs.editors.sheets");
                    startActivity(send);
                    return;
                } catch (Exception ignored) {
                    send.setPackage(null);
                }
            }
            startActivity(Intent.createChooser(send, "Экспортировать CSV"));
        } catch (Exception e) {
            toast("Ошибка экспорта: " + e.getMessage());
        }
    }

    private String csvCell(String value) {
        String v = safe(value).replace("\"", "\"\"");
        return "\"" + v + "\"";
    }

    private void changeMonth(int delta) {
        changeMonthOnly(delta);
        renderDashboard();
    }

    private void changeMonthOnly(int delta) {
        shownMonth += delta;
        if (shownMonth < 0) {
            shownMonth = 11;
            shownYear--;
        } else if (shownMonth > 11) {
            shownMonth = 0;
            shownYear++;
        }
    }

    private void pickMonth() {
        showMonthPicker(() -> renderDashboard());
    }

    private void pickMonthForAnalytics() {
        showMonthPicker(() -> renderAnalytics());
    }

    private void showMonthPicker(Runnable after) {
        LinearLayout box = dialogBox();
        Spinner month = new Spinner(this);
        month.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, MONTHS));
        month.setSelection(shownMonth);
        EditText year = field(String.valueOf(shownYear), "Год", InputType.TYPE_CLASS_NUMBER);
        box.addView(month, lpMatchWrap());
        box.addView(spacer(10));
        box.addView(year, lpMatchWrap());

        AlertDialog d = new AlertDialog.Builder(this, dark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setTitle("Выберите месяц").setView(box)
                .setNegativeButton("Отмена", null).setPositiveButton("Открыть", null).create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String y = year.getText().toString().trim();
            if (y.length() != 4) {
                year.setError("Введите год");
                return;
            }
            shownYear = Integer.parseInt(y);
            shownMonth = month.getSelectedItemPosition();
            d.dismiss();
            after.run();
        }));
        d.show();
    }

    private View categoryBar(CategoryTotal c, double max, int tint) {
        LinearLayout wrap = vCol();
        wrap.setPadding(0, dp(8), 0, dp(4));

        LinearLayout top = hRow();
        top.addView(text(c.category, 14, p.text, true), weighted(1f));
        top.addView(text(formatMoneyPlain(c.total), 14, tint, true));
        wrap.addView(top);

        FrameLayout track = new FrameLayout(this);
        track.setBackground(round(p.surface2, 999));
        LinearLayout.LayoutParams trackLp = fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        trackLp.setMargins(0, dp(5), 0, 0);
        wrap.addView(track, trackLp);

        View fill = new View(this);
        fill.setBackground(round(tint, 999));
        float ratio = max <= 0 ? 0 : (float)(c.total / max);
        int width = Math.max(dp(12), (int)(dp(260) * ratio));
        track.addView(fill, new FrameLayout.LayoutParams(width, dp(8)));
        return wrap;
    }

    private String categoryIcon(String category, boolean income) {
        String s = safe(category).toLowerCase(Locale.ROOT);
        if (s.contains("зарп")) return "€";
        if (s.contains("фрил")) return "◆";
        if (s.contains("продаж")) return "↗";
        if (s.contains("продукт") || s.contains("еда") || s.contains("рест")) return "●";
        if (s.contains("жил") || s.contains("аренд")) return "⌂";
        if (s.contains("транспорт") || s.contains("топлив")) return "▰";
        if (s.contains("здоров") || s.contains("апт")) return "+";
        if (s.contains("покуп")) return "■";
        if (s.contains("штраф")) return "!";
        return income ? "↑" : "↓";
    }

    private LinearLayout dialogBox() {
        LinearLayout box = vCol();
        box.setPadding(dp(20), dp(8), dp(20), dp(8));
        return box;
    }

    private TextView label(String s) {
        TextView t = text(s, 14, p.muted, true);
        t.setPadding(0, 0, 0, dp(5));
        return t;
    }

    private EditText field(String value, String hint, int inputType) {
        EditText e = new EditText(this);
        e.setText(value);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setTextColor(p.text);
        e.setHintTextColor(p.muted);
        e.setTextSize(16);
        e.setPadding(dp(14), dp(11), dp(14), dp(11));
        e.setBackground(round(p.surface2, 14));
        return e;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setIncludeFontPadding(false);
        return t;
    }

    private Button button(String value, int bg, int fg, int sp, int radius) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(value);
        b.setTextSize(sp);
        b.setTextColor(fg);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setBackground(round(bg, radius));
        b.setGravity(Gravity.CENTER);
        b.setStateListAnimator(null);
        return b;
    }

    private LinearLayout card(int color, int radius) {
        LinearLayout v = vCol();
        v.setBackground(round(color, radius));
        if (android.os.Build.VERSION.SDK_INT >= 21) v.setElevation(dp(2));
        return v;
    }

    private android.graphics.drawable.GradientDrawable round(int color, int radiusDp) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(p.border);
        v.setAlpha(dark ? 0.55f : 0.8f);
        v.setLayoutParams(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        return v;
    }

    private View spacer(int h) {
        View v = new View(this);
        v.setLayoutParams(fixed(1, dp(h)));
        return v;
    }

    private LinearLayout hRow() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private LinearLayout vCol() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams weighted(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams fixed(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    private LinearLayout.LayoutParams lpMatchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private String formatMoneyPlain(double v) {
        return money.format(Math.abs(v)) + " €";
    }

    private String operationWord(int n) {
        int mod10 = n % 10, mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) return "операция";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "операции";
        return "операций";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private String isoDate(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private String displayDate(String iso) {
        try {
            String[] x = iso.split("-");
            int y = Integer.parseInt(x[0]);
            int m = Integer.parseInt(x[1]) - 1;
            int d = Integer.parseInt(x[2]);
            return String.format(Locale.getDefault(), "%02d %s %d", d, MONTHS_SHORT[m], y);
        } catch (Exception e) {
            return iso;
        }
    }

    private void setCalendarFromIso(Calendar c, String iso) {
        try {
            String[] x = iso.split("-");
            c.set(Integer.parseInt(x[0]), Integer.parseInt(x[1]) - 1, Integer.parseInt(x[2]));
        } catch (Exception ignored) {}
    }

    private int daysInMonth(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, 1);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private static class Tx {
        long id;
        String type;
        double amount;
        String category;
        String note;
        String date;
    }

    private static class CategoryTotal {
        String category;
        double total;
        CategoryTotal(String category, double total) {
            this.category = category;
            this.total = total;
        }
    }

    private static class MonthPoint {
        String label;
        double income;
        double expense;
        MonthPoint(String label, double income, double expense) {
            this.label = label;
            this.income = income;
            this.expense = expense;
        }
    }

    private static class Db extends SQLiteOpenHelper {
        private static final String DB_NAME = "rotapago_finance.db";
        private static final int DB_VERSION = 1;

        Db(Context c) {
            super(c, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, amount REAL NOT NULL, category TEXT NOT NULL, note TEXT, date TEXT NOT NULL, created_at INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX idx_transactions_date ON transactions(date)");
            db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, name TEXT NOT NULL, sort_order INTEGER NOT NULL DEFAULT 0, UNIQUE(type,name))");

            seed(db, "income", new String[]{"Зарплата","Фриланс","Продажа","Возврат","Прочее"});
            seed(db, "expense", new String[]{"Продукты","Жильё","Транспорт","Топливо","Здоровье","Ресторан","Покупки","Связь","Обучение","Другое"});
        }

        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

        private void seed(SQLiteDatabase db, String type, String[] names) {
            for (int i = 0; i < names.length; i++) {
                ContentValues cv = new ContentValues();
                cv.put("type", type);
                cv.put("name", names[i]);
                cv.put("sort_order", i);
                db.insert("categories", null, cv);
            }
        }

        void addTransaction(String type, double amount, String category, String note, String date) {
            ContentValues cv = new ContentValues();
            cv.put("type", type);
            cv.put("amount", amount);
            cv.put("category", category);
            cv.put("note", note);
            cv.put("date", date);
            cv.put("created_at", System.currentTimeMillis());
            getWritableDatabase().insert("transactions", null, cv);
        }

        void updateTransaction(long id, String type, double amount, String category, String note, String date) {
            ContentValues cv = new ContentValues();
            cv.put("type", type);
            cv.put("amount", amount);
            cv.put("category", category);
            cv.put("note", note);
            cv.put("date", date);
            getWritableDatabase().update("transactions", cv, "id=?", new String[]{String.valueOf(id)});
        }

        void deleteTransaction(long id) {
            getWritableDatabase().delete("transactions", "id=?", new String[]{String.valueOf(id)});
        }

        double[] monthTotals(int year, int month) {
            String start = monthStart(year, month);
            String end = nextMonthStart(year, month);
            double income = 0, expense = 0;
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT type,COALESCE(SUM(amount),0) FROM transactions WHERE date>=? AND date<? GROUP BY type",
                    new String[]{start, end});
            while (c.moveToNext()) {
                if ("income".equals(c.getString(0))) income = c.getDouble(1);
                else if ("expense".equals(c.getString(0))) expense = c.getDouble(1);
            }
            c.close();
            return new double[]{income, expense};
        }

        int[] monthCounts(int year, int month) {
            String start = monthStart(year, month);
            String end = nextMonthStart(year, month);
            int income = 0, expense = 0;
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT type,COUNT(*) FROM transactions WHERE date>=? AND date<? GROUP BY type",
                    new String[]{start, end});
            while (c.moveToNext()) {
                if ("income".equals(c.getString(0))) income = c.getInt(1);
                else if ("expense".equals(c.getString(0))) expense = c.getInt(1);
            }
            c.close();
            return new int[]{income, expense};
        }

        double allTimeBalance() {
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT COALESCE(SUM(CASE WHEN type='income' THEN amount ELSE -amount END),0) FROM transactions", null);
            double v = 0;
            if (c.moveToFirst()) v = c.getDouble(0);
            c.close();
            return v;
        }

        List<Tx> transactionsForMonth(int year, int month, int limit) {
            String start = monthStart(year, month);
            String end = nextMonthStart(year, month);
            String sql = "SELECT id,type,amount,category,note,date FROM transactions WHERE date>=? AND date<? ORDER BY date DESC,created_at DESC";
            if (limit > 0) sql += " LIMIT " + limit;
            Cursor c = getReadableDatabase().rawQuery(sql, new String[]{start, end});
            ArrayList<Tx> out = new ArrayList<>();
            while (c.moveToNext()) out.add(readTx(c));
            c.close();
            return out;
        }

        List<Tx> allTransactions() {
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT id,type,amount,category,note,date FROM transactions ORDER BY date DESC,created_at DESC", null);
            ArrayList<Tx> out = new ArrayList<>();
            while (c.moveToNext()) out.add(readTx(c));
            c.close();
            return out;
        }

        private Tx readTx(Cursor c) {
            Tx t = new Tx();
            t.id = c.getLong(0);
            t.type = c.getString(1);
            t.amount = c.getDouble(2);
            t.category = c.getString(3);
            t.note = c.getString(4);
            t.date = c.getString(5);
            return t;
        }

        List<String> categories(String type) {
            Cursor c = getReadableDatabase().query("categories", new String[]{"name"}, "type=?",
                    new String[]{type}, null, null, "sort_order,id");
            ArrayList<String> out = new ArrayList<>();
            while (c.moveToNext()) out.add(c.getString(0));
            c.close();
            return out;
        }

        boolean addCategory(String type, String name) {
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT 1 FROM categories WHERE type=? AND lower(name)=lower(?) LIMIT 1",
                    new String[]{type, name});
            boolean exists = c.moveToFirst();
            c.close();
            if (exists) return false;
            ContentValues cv = new ContentValues();
            cv.put("type", type);
            cv.put("name", name);
            cv.put("sort_order", 999);
            return getWritableDatabase().insert("categories", null, cv) != -1;
        }

        void deleteCategory(String type, String name) {
            getWritableDatabase().delete("categories", "type=? AND name=?", new String[]{type, name});
        }

        List<CategoryTotal> categoryTotals(int year, int month, String type) {
            String start = monthStart(year, month);
            String end = nextMonthStart(year, month);
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT category,SUM(amount) total FROM transactions WHERE type=? AND date>=? AND date<? GROUP BY category ORDER BY total DESC",
                    new String[]{type, start, end});
            ArrayList<CategoryTotal> out = new ArrayList<>();
            while (c.moveToNext()) out.add(new CategoryTotal(c.getString(0), c.getDouble(1)));
            c.close();
            return out;
        }

        Tx biggestTransaction(int year, int month, String type) {
            String start = monthStart(year, month);
            String end = nextMonthStart(year, month);
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT id,type,amount,category,note,date FROM transactions WHERE type=? AND date>=? AND date<? ORDER BY amount DESC LIMIT 1",
                    new String[]{type, start, end});
            Tx t = null;
            if (c.moveToFirst()) t = readTx(c);
            c.close();
            return t;
        }

        MonthPoint[] lastMonths(int endYear, int endMonth, int count) {
            MonthPoint[] out = new MonthPoint[count];
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(endYear, endMonth, 1);
            c.add(Calendar.MONTH, -(count - 1));
            String[] shortRu = {"янв","фев","мар","апр","май","июн","июл","авг","сен","окт","ноя","дек"};
            for (int i = 0; i < count; i++) {
                int y = c.get(Calendar.YEAR);
                int m = c.get(Calendar.MONTH);
                double[] totals = monthTotals(y, m);
                out[i] = new MonthPoint(shortRu[m], totals[0], totals[1]);
                c.add(Calendar.MONTH, 1);
            }
            return out;
        }

        private static String monthStart(int year, int month) {
            return String.format(Locale.US, "%04d-%02d-01", year, month + 1);
        }

        private static String nextMonthStart(int year, int month) {
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(year, month, 1);
            c.add(Calendar.MONTH, 1);
            return String.format(Locale.US, "%04d-%02d-01", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
        }
    }

    private static class FinanceChartView extends View {
        static final int MODE_BARS = 1;
        static final int MODE_LINE = 2;
        static final int MODE_PIE = 3;

        final int mode;
        final MonthPoint[] months;
        final List<CategoryTotal> categories;
        final Palette p;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        FinanceChartView(Context context, int mode, MonthPoint[] months, List<CategoryTotal> categories, Palette p) {
            super(context);
            this.mode = mode;
            this.months = months;
            this.categories = categories;
            this.p = p;
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (mode == MODE_BARS) drawBars(canvas);
            else if (mode == MODE_LINE) drawLine(canvas);
            else drawPie(canvas);
        }

        private void drawBars(Canvas c) {
            if (months == null || months.length == 0) return;
            float w = getWidth(), h = getHeight();
            float left = 12, right = w - 12, top = 24, bottom = h - 34;
            double max = 1;
            for (MonthPoint m : months) max = Math.max(max, Math.max(m.income, m.expense));

            paint.setStrokeWidth(1);
            paint.setColor(p.border);
            for (int i = 0; i <= 4; i++) {
                float y = top + (bottom - top) * i / 4f;
                c.drawLine(left, y, right, y, paint);
            }

            float groupW = (right - left) / months.length;
            float barW = Math.max(10, groupW * 0.24f);
            for (int i = 0; i < months.length; i++) {
                MonthPoint m = months[i];
                float cx = left + groupW * i + groupW / 2f;
                float ih = (float)((bottom - top) * (m.income / max));
                float eh = (float)((bottom - top) * (m.expense / max));

                paint.setColor(p.income);
                c.drawRoundRect(cx - barW - 2, bottom - ih, cx - 2, bottom, 10, 10, paint);
                paint.setColor(p.expense);
                c.drawRoundRect(cx + 2, bottom - eh, cx + barW + 2, bottom, 10, 10, paint);

                drawLabel(c, m.label, cx, h - 10, p.muted, 12, Paint.Align.CENTER);
            }
        }

        private void drawLine(Canvas c) {
            if (months == null || months.length == 0) return;
            float w = getWidth(), h = getHeight();
            float left = 18, right = w - 18, top = 24, bottom = h - 34;

            double maxAbs = 1;
            for (MonthPoint m : months) maxAbs = Math.max(maxAbs, Math.abs(m.income - m.expense));
            float zero = (top + bottom) / 2f;

            paint.setStrokeWidth(1);
            paint.setColor(p.border);
            c.drawLine(left, zero, right, zero, paint);

            Path path = new Path();
            for (int i = 0; i < months.length; i++) {
                MonthPoint m = months[i];
                float x = months.length == 1 ? (left + right) / 2f : left + (right - left) * i / (months.length - 1f);
                double net = m.income - m.expense;
                float y = zero - (float)((bottom - top) * 0.43 * (net / maxAbs));
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);

                paint.setColor(net >= 0 ? p.income : p.expense);
                c.drawCircle(x, y, 7, paint);
                drawLabel(c, m.label, x, h - 10, p.muted, 12, Paint.Align.CENTER);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(p.primary);
            c.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawPie(Canvas c) {
            float w = getWidth(), h = getHeight();
            if (categories == null || categories.isEmpty()) {
                drawLabel(c, "Нет расходов за этот месяц", w / 2, h / 2, p.muted, 15, Paint.Align.CENTER);
                return;
            }

            double total = 0;
            for (CategoryTotal ct : categories) total += ct.total;
            if (total <= 0) return;

            float size = Math.min(w * 0.52f, h * 0.72f);
            RectF oval = new RectF(10, (h - size) / 2f, 10 + size, (h + size) / 2f);
            float start = -90;
            int[] palette = new int[]{p.primary, p.expense, p.income, Color.rgb(255,169,77),
                    Color.rgb(145,107,255), Color.rgb(40,190,200), Color.rgb(235,90,160)};

            int shown = Math.min(categories.size(), 7);
            for (int i = 0; i < shown; i++) {
                CategoryTotal ct = categories.get(i);
                float sweep = (float)(360.0 * ct.total / total);
                paint.setColor(palette[i % palette.length]);
                c.drawArc(oval, start, sweep, true, paint);
                start += sweep;
            }

            float inner = size * 0.52f;
            paint.setColor(p.surface);
            c.drawCircle(oval.centerX(), oval.centerY(), inner / 2f, paint);

            drawLabel(c, "Расходы", oval.centerX(), oval.centerY() - 4, p.muted, 13, Paint.Align.CENTER);
            drawLabel(c, euro(total), oval.centerX(), oval.centerY() + 20, p.text, 16, Paint.Align.CENTER);

            float x = oval.right + 18;
            float y = 42;
            for (int i = 0; i < shown; i++) {
                CategoryTotal ct = categories.get(i);
                paint.setColor(palette[i % palette.length]);
                c.drawCircle(x, y - 5, 6, paint);
                String label = ct.category.length() > 13 ? ct.category.substring(0, 12) + "…" : ct.category;
                drawLabel(c, label, x + 14, y, p.text, 12, Paint.Align.LEFT);
                drawLabel(c, String.format(Locale.US, "%.0f%%", ct.total * 100.0 / total), x + 14, y + 16, p.muted, 11, Paint.Align.LEFT);
                y += 36;
            }
        }

        private void drawLabel(Canvas c, String value, float x, float y, int color, float sp, Paint.Align align) {
            textPaint.setColor(color);
            textPaint.setTextSize(sp * getResources().getDisplayMetrics().scaledDensity);
            textPaint.setTextAlign(align);
            c.drawText(value, x, y, textPaint);
        }

        private static String euro(double v) {
            return String.format(Locale.US, "%,.2f €", v);
        }
    }

    private static class Palette {
        int bg, surface, surface2, text, muted, primary, primarySoft, income, incomeSoft, expense, expenseSoft, border;

        static Palette light() {
            Palette p = new Palette();
            p.bg = Color.parseColor("#F4F7FB");
            p.surface = Color.WHITE;
            p.surface2 = Color.parseColor("#EAF1FA");
            p.text = Color.parseColor("#0D1B3A");
            p.muted = Color.parseColor("#718096");
            p.primary = Color.parseColor("#1877F2");
            p.primarySoft = Color.parseColor("#E6F0FF");
            p.income = Color.parseColor("#10A96B");
            p.incomeSoft = Color.parseColor("#E2F7EE");
            p.expense = Color.parseColor("#EF3F45");
            p.expenseSoft = Color.parseColor("#FDEBEC");
            p.border = Color.parseColor("#DCE5EF");
            return p;
        }

        static Palette dark() {
            Palette p = new Palette();
            p.bg = Color.parseColor("#0B1018");
            p.surface = Color.parseColor("#151D29");
            p.surface2 = Color.parseColor("#1D2736");
            p.text = Color.parseColor("#F7FAFF");
            p.muted = Color.parseColor("#98A8C0");
            p.primary = Color.parseColor("#7583FF");
            p.primarySoft = Color.parseColor("#202B4C");
            p.income = Color.parseColor("#20DF8A");
            p.incomeSoft = Color.parseColor("#103B2C");
            p.expense = Color.parseColor("#FF5B55");
            p.expenseSoft = Color.parseColor("#3B2024");
            p.border = Color.parseColor("#2A374A");
            return p;
        }
    }
}
