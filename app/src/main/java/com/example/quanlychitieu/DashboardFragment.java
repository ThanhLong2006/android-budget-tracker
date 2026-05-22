package com.example.quanlychitieu;

// Import các thư viện Android và thư viện biểu đồ cần thiết
import android.content.Context; // Cung cấp ngữ cảnh để truy cập tài nguyên hệ thống
import android.content.SharedPreferences; // Dùng để lưu trữ các cài đặt nhỏ (như tên người dùng)
import android.graphics.Color; // Thư viện xử lý màu sắc cho văn bản và biểu đồ
import android.os.Bundle; // Đối tượng dùng để truyền dữ liệu giữa các màn hình (Fragment)
import android.view.LayoutInflater; // Dùng để nạp file giao diện XML vào mã Java
import android.view.View; // Lớp cơ sở cho tất cả các thành phần giao diện
import android.view.ViewGroup; // Container chứa các View, dùng làm gốc cho Fragment
import android.widget.TextView; // Thành phần hiển thị văn bản

import androidx.annotation.NonNull; // Ràng buộc tham số không được phép null
import androidx.annotation.Nullable; // Cho phép tham số có thể là null
import androidx.fragment.app.Fragment; // Mảnh giao diện giúp tái sử dụng và quản lý màn hình linh hoạt
import androidx.navigation.Navigation; // Thư viện điều hướng chuẩn của Android Jetpack
import androidx.recyclerview.widget.LinearLayoutManager; // Quản lý bố cục danh sách (cuộn dọc/ngang)
import androidx.recyclerview.widget.RecyclerView; // Hiển thị danh sách dữ liệu lớn một cách tối ưu

import com.example.quanlychitieu.data.SyncManager; // Lớp xử lý đồng bộ dữ liệu với Firebase
import com.example.quanlychitieu.data.dao.TransactionDao; // Interface thực hiện các lệnh SQL cho Giao dịch
import com.example.quanlychitieu.data.database.AppDatabase; // Lớp khởi tạo cơ sở dữ liệu Room cục bộ
import com.github.mikephil.charting.charts.PieChart; // Thư viện vẽ biểu đồ tròn (MPAndroidChart)
import com.github.mikephil.charting.data.PieData; // Đối tượng chứa toàn bộ dữ liệu của biểu đồ tròn
import com.github.mikephil.charting.data.PieDataSet; // Tập hợp các phần (miếng bánh) của biểu đồ
import com.github.mikephil.charting.data.PieEntry; // Một điểm dữ liệu cụ thể (nhãn và giá trị) trên biểu đồ
import com.github.mikephil.charting.utils.ColorTemplate; // Các bảng màu mẫu có sẵn của thư viện
import com.google.firebase.auth.FirebaseAuth;

import java.text.NumberFormat; // Định dạng số thành chuỗi tiền tệ chuyên nghiệp
import java.util.ArrayList; // Danh sách mảng linh hoạt để lưu trữ đối tượng
import java.util.List; // Giao diện danh sách chuẩn của Java
import java.util.Locale; // Cài đặt ngôn ngữ và khu vực (Việt Nam)

/**
 * DashboardFragment: Màn hình chính hiển thị tóm tắt tài chính, ngân sách và biểu đồ chi tiêu.
 */
public class DashboardFragment extends Fragment {

    private TextView tvTotalBalance, tvIncome, tvExpense, tvSeeAll, tvSeeAllBudget, tvUsername, tvGreeting;
    private RecyclerView recyclerRecent, recyclerBudget;
    private PieChart pieChart;
    private TransactionDao transactionDao;
    private TransactionAdapter adapter;
    private BudgetAdapter budgetAdapter;
    private String userId;

    public DashboardFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Navigation.findNavController(view).navigate(R.id.nav_start);
            return;
        }

        // Ánh xạ View an toàn
        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        tvIncome = view.findViewById(R.id.tv_income);
        tvExpense = view.findViewById(R.id.tv_expense);
        tvSeeAll = view.findViewById(R.id.tv_see_all);
        tvSeeAllBudget = view.findViewById(R.id.tv_see_all_budget);
        tvUsername = view.findViewById(R.id.tv_username);
        tvGreeting = view.findViewById(R.id.tv_greeting);
        recyclerRecent = view.findViewById(R.id.recycler_recent);
        recyclerBudget = view.findViewById(R.id.recycler_budget_dash);
        pieChart = view.findViewById(R.id.pie_chart_dash);

        transactionDao = AppDatabase.getDatabase(requireContext()).transactionDao();

        setupUI();
        setupRecyclerViews();
        setupPieChart();
        setupQuickActions(view);
        observeData();
        
        new SyncManager(requireContext()).syncAll();

        // Gán sự kiện click kèm kiểm tra null để tránh văng app
        if (tvSeeAll != null) {
            tvSeeAll.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.nav_history)
            );
        }
        
        if (tvSeeAllBudget != null) {
            tvSeeAllBudget.setOnClickListener(v -> 
                Navigation.findNavController(v).navigate(R.id.nav_budget)
            );
        }
    }

    private void setupUI() {
        String name = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                     FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : null;
        if (name == null || name.isEmpty()) {
            SharedPreferences prefs = requireContext().getSharedPreferences("wealthflow_prefs", Context.MODE_PRIVATE);
            name = prefs.getString("user_name", "Người dùng");
        }
        if (tvUsername != null) tvUsername.setText(name);
        
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (tvGreeting != null) {
            if (hour >= 5 && hour < 12) tvGreeting.setText("Chào buổi sáng,");
            else if (hour >= 12 && hour < 18) tvGreeting.setText("Chào buổi chiều,");
            else tvGreeting.setText("Chào buổi tối,");
        }
    }

    private void setupQuickActions(View view) {
        View btnAdd = view.findViewById(R.id.btn_quick_add);
        View btnWallet = view.findViewById(R.id.btn_quick_wallet);
        View btnBudget = view.findViewById(R.id.btn_quick_budget);

        if (btnAdd != null) btnAdd.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.addTransactionFragment));
        if (btnWallet != null) btnWallet.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_wallet));
        if (btnBudget != null) btnBudget.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_budget));
    }

    private void setupRecyclerViews() {
        adapter = new TransactionAdapter(transaction -> {
            Bundle bundle = new Bundle();
            bundle.putInt("transactionId", transaction.id);
            Navigation.findNavController(requireView()).navigate(R.id.action_nav_dashboard_to_addTransactionFragment, bundle);
        });
        if (recyclerRecent != null) {
            recyclerRecent.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerRecent.setAdapter(adapter);
        }

        AppDatabase db = AppDatabase.getDatabase(requireContext());
        budgetAdapter = new BudgetAdapter(db, getViewLifecycleOwner());
        if (recyclerBudget != null) {
            recyclerBudget.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            recyclerBudget.setAdapter(budgetAdapter);
        }
    }

    private void setupPieChart() {
        if (pieChart == null) return;
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setHoleRadius(65f);
        pieChart.setTransparentCircleRadius(70f);
        pieChart.getLegend().setEnabled(false);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(11f);
        pieChart.setDrawEntryLabels(true);
        pieChart.setCenterText("Chi tiêu");
        pieChart.setCenterTextSize(16f);
    }

    private void observeData() {
        transactionDao.getTotalBalance(userId).observe(getViewLifecycleOwner(), balance -> {
            if (tvTotalBalance != null) tvTotalBalance.setText(formatCurrency(balance != null ? balance : 0.0));
        });

        transactionDao.getMonthlyTotalByType(userId, "INCOME").observe(getViewLifecycleOwner(), income -> {
            if (tvIncome != null) tvIncome.setText(formatCurrency(income != null ? income : 0.0));
        });

        transactionDao.getMonthlyTotalByType(userId, "EXPENSE").observe(getViewLifecycleOwner(), expense -> {
            if (tvExpense != null) tvExpense.setText(formatCurrency(expense != null ? expense : 0.0));
        });

        transactionDao.getRecentTransactionsWithCategory(userId).observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) adapter.setTransactions(transactions);
        });

        String currentMonth = new java.text.SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(java.util.Calendar.getInstance().getTime());
        AppDatabase.getDatabase(requireContext()).budgetDao().getBudgetsByMonthWithCategory(userId, currentMonth).observe(getViewLifecycleOwner(), budgets -> {
            if (budgets != null) budgetAdapter.setBudgets(budgets);
        });

        transactionDao.getCategoryStats(userId, "EXPENSE").observe(getViewLifecycleOwner(), stats -> {
            if (pieChart == null) return;
            if (stats != null && !stats.isEmpty()) {
                List<PieEntry> entries = new ArrayList<>();
                List<Integer> colors = new ArrayList<>();
                for (TransactionDao.CategoryStats stat : stats) {
                    entries.add(new PieEntry((float) stat.totalAmount, stat.categoryName));
                    if (stat.categoryColor != null) {
                        try { colors.add(Color.parseColor(stat.categoryColor)); } 
                        catch (Exception e) { colors.add(Color.LTGRAY); }
                    } else { colors.add(Color.LTGRAY); }
                }
                PieDataSet dataSet = new PieDataSet(entries, "");
                dataSet.setColors(colors);
                dataSet.setSliceSpace(4f);
                dataSet.setValueTextSize(12f);
                dataSet.setValueTextColor(Color.WHITE);
                
                PieData data = new PieData(dataSet);
                pieChart.setData(data);
                pieChart.animateY(1200);
                pieChart.invalidate();
            } else {
                pieChart.clear();
            }
        });
    }

    private String formatCurrency(Double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }
}
