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
import com.github.mikephil.charting.components.Legend;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class statisticalFragment extends Fragment {

    private PieChart pieChart;
    private BarChart barChart, compareChart;
    private TextView tvSummaryTotal, tvMaxCategory, tabIncome, tabExpense;
    private TransactionDao transactionDao;
    private String currentType = "EXPENSE"; // Mặc định xem chi tiêu
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
        // Giả sử fragment_statistical.xml có thêm compareChart hoặc ta dùng barChart để so sánh
        // Ở đây tôi sẽ dùng barChart hiện tại để hiển thị so sánh Tháng này vs Tháng trước nếu chọn tab tương ứng
        // Hoặc tạo một BarChart mới nếu layout cho phép. 
        // Để đơn giản và hiệu quả, tôi sẽ cập nhật barChart để hiển thị so sánh tháng.
        tvSummaryTotal = view.findViewById(R.id.tv_summary_total);
        tvMaxCategory = view.findViewById(R.id.tv_max_category);
        tabIncome = view.findViewById(R.id.tab_income);
        tabExpense = view.findViewById(R.id.tab_expense);
    }

    private void setupCharts() {
        // Cấu hình PieChart
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        // Cấu hình BarChart (So sánh tháng)
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.getAxisRight().setEnabled(false);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
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

        // 1. Tổng kết tháng này
        transactionDao.getMonthlyTotalByType(userId, currentType).observe(getViewLifecycleOwner(), total -> {
            tvSummaryTotal.setText(formatCurrency(total != null ? total : 0.0));
        });

        // 2. Phân bổ theo danh mục (Pie Chart)
        transactionDao.getCategoryStats(userId, currentType).observe(getViewLifecycleOwner(), stats -> {
            if (stats != null && !stats.isEmpty()) {
                updatePieChart(stats);
                tvMaxCategory.setText(stats.get(0).categoryName);
            } else {
                pieChart.clear();
                tvMaxCategory.setText("N/A");
            }
        });

        // 3. So sánh Tháng này với Tháng trước (Bar Chart)
        loadMonthlyComparison();
    }

    private void loadMonthlyComparison() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        
        String thisMonth = sdf.format(cal.getTime());
        cal.add(Calendar.MONTH, -1);
        String lastMonth = sdf.format(cal.getTime());

        transactionDao.getSpecificMonthlyTotal(userId, currentType, thisMonth).observe(getViewLifecycleOwner(), thisTotal -> {
            transactionDao.getSpecificMonthlyTotal(userId, currentType, lastMonth).observe(getViewLifecycleOwner(), lastTotal -> {
                updateComparisonChart(thisTotal != null ? thisTotal : 0.0, lastTotal != null ? lastTotal : 0.0);
            });
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
        
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.animateY(800);
        pieChart.invalidate();
    }

    private void updateComparisonChart(double thisMonth, double lastMonth) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, (float) lastMonth));
        entries.add(new BarEntry(1f, (float) thisMonth));

        BarDataSet dataSet = new BarDataSet(entries, "So sánh chi tiêu (Tháng trước vs Tháng này)");
        dataSet.setColors(new int[]{Color.LTGRAY, ContextCompat.getColor(requireContext(), 
                currentType.equals("INCOME") ? R.color.income_green : R.color.expense_red)});
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        barChart.setData(data);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(new String[]{"Tháng trước", "Tháng này"}));
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private String formatCurrency(Double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }
}
