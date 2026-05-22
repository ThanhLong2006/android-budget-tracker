package com.example.quanlychitieu;

// Import các thư viện cần thiết cho UI, Data và Firebase
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle; // Dùng để truyền dữ liệu giữa các Fragment (VD: ID giao dịch để sửa)
import android.view.LayoutInflater; // Chuyển đổi file XML layout thành đối tượng View trong Java
import android.view.View; // Lớp cơ sở cho các thành phần giao diện người dùng
import android.view.ViewGroup; // Lớp chứa các View khác, dùng làm container cho Fragment
import android.widget.Button;
import android.widget.TextView; // Thành phần dùng để hiển thị văn bản (như thông báo "Không có giao dịch")

import androidx.annotation.NonNull; // Ràng buộc tham số truyền vào không được phép null
import androidx.annotation.Nullable; // Cho phép tham số truyền vào có thể là null
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment; // Mảnh giao diện giúp quản lý và tái sử dụng UI linh hoạt
import androidx.lifecycle.LiveData; // Thành phần quan sát dữ liệu có khả năng nhận biết vòng đời của Activity/Fragment
import androidx.navigation.Navigation; // Thư viện Jetpack Navigation để chuyển màn hình dễ dàng
import androidx.recyclerview.widget.LinearLayoutManager; // Quản lý cách hiển thị danh sách (cuộn dọc mặc định)
import androidx.recyclerview.widget.RecyclerView; // Hiển thị danh sách dữ liệu lớn hiệu quả, tiết kiệm bộ nhớ

import com.example.quanlychitieu.data.dao.TransactionDao; // Interface chứa các hàm truy vấn SQL cho bảng Giao dịch
import com.example.quanlychitieu.data.database.AppDatabase; // Lớp khởi tạo cơ sở dữ liệu cục bộ Room
import com.google.android.material.button.MaterialButtonToggleGroup; // Nhóm nút chuyển đổi (Tất cả/Thu/Chi)
import com.google.firebase.auth.FirebaseAuth; // Thư viện quản lý đăng nhập và định danh người dùng

import java.util.ArrayList; // Danh sách mảng linh hoạt để lưu trữ kết quả lọc
import java.util.List; // Giao diện chuẩn cho các tập hợp dữ liệu dạng danh sách

/**
 * historyFragment: Màn hình lịch sử giao dịch.
 * Cho phép người dùng xem lại toàn bộ giao dịch, lọc theo loại và sửa giao dịch khi cần.
 */
public class historyFragment extends Fragment {

    // Khai báo các thành phần UI để điều khiển trong mã Java
    private RecyclerView recyclerHistory; // View hiển thị danh sách giao dịch
    private TransactionAdapter adapter; // Cầu nối để đổ dữ liệu từ danh sách vào RecyclerView
    private TransactionDao transactionDao; // Đối tượng dùng để lấy dữ liệu từ cơ sở dữ liệu Room
    private TextView tvEmpty; // Thông báo hiển thị khi không tìm thấy giao dịch nào
    private MaterialButtonToggleGroup toggleGroup; // Nhóm nút lọc Tất cả, Thu nhập, Chi tiêu
    private LiveData<List<TransactionDao.TransactionWithCategory>> currentLiveData; // Lưu giữ đối tượng quan sát dữ liệu hiện tại
    private String userId; // ID duy nhất của người dùng để đảm bảo chỉ hiển thị đúng dữ liệu cá nhân

    public historyFragment() {} // Constructor mặc định (bắt buộc theo quy tắc của Fragment)

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Nạp layout XML fragment_history vào Fragment để hiển thị giao diện
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState); // Gọi hàm lớp cha để đảm bảo quá trình khởi tạo chuẩn

        // Lấy UID của người dùng từ Firebase. Chọn cách này vì Firebase là nguồn duy nhất xác thực người dùng.
        userId = FirebaseAuth.getInstance().getUid();
        
        // Nếu người dùng chưa đăng nhập, chuyển hướng về màn hình Start để bảo vệ tính riêng tư của dữ liệu.
        if (userId == null) {
            Navigation.findNavController(view).navigate(R.id.nav_start);
            return;
        }

        // Ánh xạ các View từ XML sang Java thông qua ID (findViewById).
        recyclerHistory = view.findViewById(R.id.recycler_history);
        tvEmpty = view.findViewById(R.id.tv_empty);
        toggleGroup = view.findViewById(R.id.toggle_group);

        // Khởi tạo đối tượng truy cập DB.
        transactionDao = AppDatabase.getDatabase(requireContext()).transactionDao();

        setupRecyclerView();
        setupFilters();
        
        // ĐÃ SỬA: Khởi tạo mặc định chọn nút "Tất cả" và làm sáng đèn nút này
        toggleGroup.check(R.id.btn_all);
        updateFilterUI(R.id.btn_all);
        loadTransactions("ALL");
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(transaction -> {
            Bundle bundle = new Bundle();
            // FIX: TransactionWithCategory uses public field 'id' instead of getId()
            bundle.putInt("transactionId", transaction.id);
            Navigation.findNavController(requireView()).navigate(R.id.action_nav_history_to_addTransactionFragment, bundle);
        });
        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerHistory.setAdapter(adapter);
    }

    private void setupFilters() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                // ĐÃ SỬA: Cập nhật giao diện "sáng đèn" cho nút được chọn
                updateFilterUI(checkedId);
                
                if (checkedId == R.id.btn_all) {
                    loadTransactions("ALL");
                } else if (checkedId == R.id.btn_income) {
                    loadTransactions("INCOME");
                } else if (checkedId == R.id.btn_expense) {
                    loadTransactions("EXPENSE");
                }
            }
        });
    }

    /**
     * updateFilterUI: Cập nhật màu sắc sáng/tối cho các nút lọc.
     */
    private void updateFilterUI(int checkedId) {
        Button btnAll = getView().findViewById(R.id.btn_all);
        Button btnIncome = getView().findViewById(R.id.btn_income);
        Button btnExpense = getView().findViewById(R.id.btn_expense);

        // Reset tất cả về trạng thái mờ/không chọn
        btnAll.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        btnAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        
        btnIncome.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        btnIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        
        btnExpense.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        btnExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        // Làm sáng nút được chọn
        if (checkedId == R.id.btn_all) {
            btnAll.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray_light)));
            btnAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
        } else if (checkedId == R.id.btn_income) {
            btnIncome.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.xanhlanhat)));
            btnIncome.setTextColor(Color.parseColor("#22C55E")); // Màu xanh lá
        } else if (checkedId == R.id.btn_expense) {
            btnExpense.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.dohongnhat)));
            btnExpense.setTextColor(Color.parseColor("#EF4444")); // Màu đỏ
        }
    }

    private void loadTransactions(String filter) {
        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
        }

        // FIX: Use getAllTransactionsWithCategory and match the LiveData type
        currentLiveData = transactionDao.getAllTransactionsWithCategory(userId);

        currentLiveData.observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                List<TransactionDao.TransactionWithCategory> filteredList = transactions;
                if (!"ALL".equals(filter)) {
                    filteredList = new ArrayList<>();
                    for (TransactionDao.TransactionWithCategory t : transactions) {
                        // FIX: Use public field 'type' instead of getType()
                        if (filter.equals(t.type)) {
                            filteredList.add(t);
                        }
                    }
                }
                
                adapter.setTransactions(filteredList);
                tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }
}
