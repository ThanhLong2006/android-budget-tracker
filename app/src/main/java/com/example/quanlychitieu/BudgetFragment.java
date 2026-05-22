package com.example.quanlychitieu;

// Import các thư viện cần thiết cho giao diện, cơ sở dữ liệu và Firebase
import android.app.AlertDialog; // Dùng để hiển thị hộp thoại thiết lập ngân sách
import android.os.Bundle; // Dùng để đóng gói dữ liệu truyền giữa các Fragment
import android.view.LayoutInflater; // Chuyển đổi file XML layout thành đối tượng View
import android.view.View; // Lớp cơ sở cho các thành phần giao diện
import android.view.ViewGroup; // Lớp chứa các View khác
import android.widget.EditText; // Ô nhập số tiền ngân sách
import android.widget.TextView; // Thành phần hiển thị văn bản (Tháng, tên danh mục)
import android.widget.Toast; // Hiển thị thông báo nhanh

import androidx.annotation.NonNull; // Ràng buộc tham số không được null
import androidx.annotation.Nullable; // Cho phép tham số có thể null
import androidx.fragment.app.Fragment; // Mảnh giao diện giúp quản lý UI linh hoạt
import androidx.navigation.Navigation; // Thư viện điều hướng Jetpack Navigation
import androidx.recyclerview.widget.LinearLayoutManager; // Quản lý bố cục danh sách (cuộn dọc)
import androidx.recyclerview.widget.RecyclerView; // Hiển thị danh sách dữ liệu lớn hiệu quả

import com.example.quanlychitieu.data.dao.BudgetDao;
import com.example.quanlychitieu.data.database.AppDatabase; // Lớp cơ sở dữ liệu chính của ứng dụng
import com.example.quanlychitieu.data.entities.Budget; // Thực thể đại diện cho bảng Ngân sách
import com.example.quanlychitieu.data.entities.Category; // Thực thể đại diện cho bảng Danh mục
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Nút bấm nổi (+)
import com.google.firebase.auth.FirebaseAuth; // Quản lý xác thực người dùng

import java.text.SimpleDateFormat; // Định dạng thời gian (VD: 10-2024)
import java.util.Calendar; // Quản lý thời gian hệ thống
import java.util.Locale; // Xác định ngôn ngữ và khu vực


 // BudgetFragment: Quản lý và hiển thị hạn mức chi tiêu (ngân sách) cho từng danh mục theo tháng.
 // Giúp người dùng kiểm soát việc chi tiêu không vượt quá kế hoạch.

public class BudgetFragment extends Fragment {

    private RecyclerView rvBudgets; // Danh sách hiển thị các ngân sách đã đặt
    private BudgetAdapter adapter; // Bộ lọc và đổ dữ liệu vào RecyclerView
    private FloatingActionButton fabAddBudget; // Nút thêm ngân sách mới
    private TextView tvCurrentMonth; // Hiển thị tháng hiện tại đang xem
    private int selectedCategoryId = -1; // ID danh mục được chọn để đặt ngân sách

    public BudgetFragment() {} // Constructor mặc định bắt buộc

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Nạp layout fragment_budget vào Fragment
        return inflater.inflate(R.layout.fragment_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các View từ XML sang Java bằng ID
        rvBudgets = view.findViewById(R.id.rv_budgets);
        fabAddBudget = view.findViewById(R.id.fab_add_budget);
        tvCurrentMonth = view.findViewById(R.id.tv_current_month);
        
        // Nút quay lại màn hình trước đó
        view.findViewById(R.id.btn_back).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Lấy tháng và năm hiện tại theo định dạng MM-yyyy để truy vấn ngân sách
        String currentMonth = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
        tvCurrentMonth.setText("Tháng " + currentMonth);

        // Khởi tạo cơ sở dữ liệu và Adapter
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        // Adapter cần DB và LifecycleOwner để tự động tính toán số tiền đã chi tiêu so với hạn mức
        adapter = new BudgetAdapter(db, getViewLifecycleOwner());
        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBudgets.setAdapter(adapter);

        // Lấy UID người dùng hiện tại để bảo mật và lọc dữ liệu
        String userId = FirebaseAuth.getInstance().getUid();
        // Quan sát dữ liệu ngân sách kèm thông tin danh mục từ DB.
        db.budgetDao().getBudgetsByMonthWithCategory(userId, currentMonth).observe(getViewLifecycleOwner(), budgets -> {
            adapter.setBudgets(budgets);
        });

        // Thiết lập sự kiện nhấn cho nút thêm ngân sách
        fabAddBudget.setOnClickListener(v -> showAddBudgetDialog(currentMonth));
    }


     // showAddBudgetDialog: Hiển thị hộp thoại để người dùng chọn danh mục và nhập hạn mức chi tiêu.
     // @param currentMonth Tháng hiện tại đang được thiết lập ngân sách

    private void showAddBudgetDialog(String currentMonth) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Đặt ngân sách chi tiêu");

        // Nạp giao diện tùy chỉnh cho hộp thoại từ XML
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_budget, null);
        builder.setView(dialogView);

        TextView tvCategory = dialogView.findViewById(R.id.tv_select_category); // Nhãn chọn danh mục
        EditText etAmount = dialogView.findViewById(R.id.et_budget_amount); // Ô nhập số tiền hạn mức

        // Khi nhấn vào vùng chọn danh mục, hiện ra danh sách các danh mục chi tiêu
        tvCategory.setOnClickListener(v -> {
            String userId = FirebaseAuth.getInstance().getUid();
            // Chỉ lấy các danh mục thuộc loại CHI TIÊU (EXPENSE) vì ngân sách chỉ dành cho chi tiêu
            AppDatabase.getDatabase(requireContext()).categoryDao().getCategoriesByType(userId, "EXPENSE")
                    .observe(getViewLifecycleOwner(), categories -> {
                        if (categories == null) return;
                        // Chuyển danh sách sang mảng tên để hiển thị trong List của Dialog
                        String[] names = new String[categories.size()];
                        for (int i = 0; i < categories.size(); i++) names[i] = categories.get(i).getName();
                        
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Chọn danh mục chi tiêu")
                                .setItems(names, (dialog, which) -> {
                                    Category selected = categories.get(which);
                                    selectedCategoryId = selected.getId(); // Lưu ID danh mục được chọn
                                    tvCategory.setText(selected.getName()); // Hiển thị tên lên Dialog
                                }).show();
                    });
        });

        // Nút Lưu: Thực hiện kiểm tra và lưu ngân sách vào cơ sở dữ liệu
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String amountStr = etAmount.getText().toString().trim();
            // Kiểm tra xem đã chọn danh mục và nhập số tiền chưa
            if (selectedCategoryId != -1 && !amountStr.isEmpty()) {
                Budget budget = new Budget(); // Tạo thực thể ngân sách mới
                budget.setUserId(FirebaseAuth.getInstance().getUid());
                budget.setCategoryId(selectedCategoryId);
                budget.setAmountLimit(Double.parseDouble(amountStr));
                budget.setMonth(currentMonth);
                budget.setPeriod("Tháng"); // Hiện tại hỗ trợ ngân sách theo tháng

                // Thực thi tác vụ ghi vào DB ở luồng nền để tránh gây đơ ứng dụng
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase.getDatabase(requireContext()).budgetDao().insert(budget);
                });
            } else {
                // Thông báo nếu người dùng nhập thiếu thông tin
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
}
