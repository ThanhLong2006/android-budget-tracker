package com.example.quanlychitieu;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.quanlychitieu.data.dao.TransactionDao;
import com.example.quanlychitieu.data.database.AppDatabase;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class statisticalFragment extends Fragment {

    private PieChart pieChart;
    private BarChart barChart;
    private TextView tvSummaryTotal, tvMaxCategory, tabIncome, tabExpense;
    private TransactionDao transactionDao;
    private String currentType = "INCOME";
    private String userId;

    public statisticalFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistical, container, false);
        userId = FirebaseAuth.getInstance().getUid();
        initViews(view);
        transactionDao = AppDatabase.getDatabase(requireContext()).transactionDao();
        
        setupCharts();
        setupListeners();
        updateTabUI();
        observeData();

        return view;
    }

    private void initViews(View view) {
        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        tvSummaryTotal = view.findViewById(R.id.tv_summary_total);
        tvMaxCategory = view.findViewById(R.id.tv_max_category);
        tabIncome = view.findViewById(R.id.tab_income);
        tabExpense = view.findViewById(R.id.tab_expense);
    }

    private void setupCharts() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setHoleRadius(58f); 
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(10f);
        pieChart.getLegend().setEnabled(true);

        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);
    }

    private void setupListeners() {
        tabIncome.setOnClickListener(v -> {
            currentType = "INCOME";
            updateTabUI();
            observeData();
        });

        tabExpense.setOnClickListener(v -> {
            currentType = "EXPENSE";
            updateTabUI();
            observeData();
        });
    }

    private void updateTabUI() {
        if (currentType.equals("INCOME")) {
            tabIncome.setBackgroundResource(R.color.xanhlanhat);
            tabIncome.setTextColor(Color.parseColor("#22C55E"));
            tabExpense.setBackgroundResource(android.R.color.transparent);
            tabExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvSummaryTotal.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        } else {
            tabExpense.setBackgroundResource(R.color.dohongnhat);
            tabExpense.setTextColor(Color.parseColor("#EF4444"));
            tabIncome.setBackgroundResource(android.R.color.transparent);
            tabIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvSummaryTotal.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        }
    }

    private void observeData() {
        if (userId == null) return;

        transactionDao.getMonthlyTotalByType(userId, currentType).observe(getViewLifecycleOwner(), total -> {
            tvSummaryTotal.setText(formatCurrency(total != null ? total : 0.0));
        });

        transactionDao.getCategoryStats(userId, currentType).observe(getViewLifecycleOwner(), stats -> {
            if (stats != null && !stats.isEmpty()) {
                updatePieChart(stats);
                tvMaxCategory.setText(stats.get(0).categoryName);
            } else {
                pieChart.clear();
                tvMaxCategory.setText("N/A");
            }
        });

        // Cập nhật biểu đồ xu hướng (So sánh Thu vs Chi)
        transactionDao.getTrendStats(userId).observe(getViewLifecycleOwner(), trendData -> {
            if (trendData != null && !trendData.isEmpty()) {
                updateTrendChart(trendData);
            } else {
                barChart.clear();
            }
        });
    }

    private void updatePieChart(List<TransactionDao.CategoryStats> stats) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        for (TransactionDao.CategoryStats stat : stats) {
            entries.add(new PieEntry((float) stat.totalAmount, stat.categoryName));
            if (stat.categoryColor != null) {
                try { colors.add(Color.parseColor(stat.categoryColor)); } 
                catch (Exception e) { colors.add(ColorTemplate.MATERIAL_COLORS[entries.size() % ColorTemplate.MATERIAL_COLORS.length]); }
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        if (colors.isEmpty()) dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        else dataSet.setColors(colors);
        
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);

        pieChart.setData(data);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void updateTrendChart(List<TransactionDao.TrendData> trendData) {
        ArrayList<BarEntry> incomeEntries = new ArrayList<>();
        ArrayList<BarEntry> expenseEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < trendData.size(); i++) {
            TransactionDao.TrendData data = trendData.get(i);
            incomeEntries.add(new BarEntry(i, (float) data.incomeValue));
            expenseEntries.add(new BarEntry(i, (float) data.expenseValue));
            labels.add(data.label.substring(5)); // Chỉ lấy MM-DD
        }

        BarDataSet incomeSet = new BarDataSet(incomeEntries, "Thu nhập");
        incomeSet.setColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        
        BarDataSet expenseSet = new BarDataSet(expenseEntries, "Chi tiêu");
        expenseSet.setColor(ContextCompat.getColor(requireContext(), R.color.expense_red));

        BarData data = new BarData(incomeSet, expenseSet);
        data.setBarWidth(0.35f);

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.setData(data);
        
        // Group bars
        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        barChart.groupBars(0f, groupSpace, barSpace);

        barChart.animateY(1000);
        barChart.invalidate();
    }

    private String formatCurrency(Double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }
}
