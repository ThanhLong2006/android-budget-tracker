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
    private String currentType = "INCOME"; // Mặc định ban đầu là Thu nhập
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
        
        // Cập nhật giao diện tab ngay khi khởi tạo
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
            // Sáng đèn tab Thu nhập (Xanh)
            tabIncome.setBackgroundResource(R.color.xanhlanhat);
            tabIncome.setTextColor(Color.parseColor("#22C55E"));
            // Tắt đèn tab Chi tiêu
            tabExpense.setBackgroundResource(android.R.color.transparent);
            tabExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvSummaryTotal.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        } else {
            // Sáng đèn tab Chi tiêu (Đỏ)
            tabExpense.setBackgroundResource(R.color.dohongnhat);
            tabExpense.setTextColor(Color.parseColor("#EF4444"));
            // Tắt đèn tab Thu nhập
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

        transactionDao.getDailyStats(userId, currentType).observe(getViewLifecycleOwner(), dailyData -> {
            if (dailyData != null && !dailyData.isEmpty()) {
                updateBarChart(dailyData);
            } else {
                barChart.clear();
            }
        });
    }

    private void updatePieChart(List<TransactionDao.CategoryStats> stats) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (TransactionDao.CategoryStats stat : stats) {
            entries.add(new PieEntry((float) stat.totalAmount, stat.categoryName));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.DKGRAY);

        pieChart.setData(data);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void updateBarChart(List<TransactionDao.ChartData> dailyData) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < dailyData.size(); i++) {
            entries.add(new BarEntry(i, (float) dailyData.get(i).value));
            labels.add(dailyData.get(i).label);
        }

        BarDataSet dataSet = new BarDataSet(entries, currentType.equals("INCOME") ? "Thu nhập" : "Chi tiêu");
        int color = currentType.equals("INCOME") ? 
                ContextCompat.getColor(requireContext(), R.color.income_green) : 
                ContextCompat.getColor(requireContext(), R.color.expense_red);
        dataSet.setColor(color);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.setData(data);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private String formatCurrency(Double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }
}
