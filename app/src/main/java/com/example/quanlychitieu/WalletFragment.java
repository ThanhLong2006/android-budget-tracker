package com.example.quanlychitieu;

// Import các thư viện cần thiết cho UI, Dialog và Database
import android.app.AlertDialog; // Dùng để hiển thị hộp thoại thêm/sửa tài khoản
import android.os.Bundle; // Dùng để truyền dữ liệu giữa các Fragment
import android.view.LayoutInflater; // Chuyển đổi file XML layout thành đối tượng View
import android.view.View; // Lớp cơ sở cho các thành phần giao diện
import android.view.ViewGroup; // Lớp chứa các View khác
import android.widget.EditText; // Ô nhập liệu văn bản/số
import android.widget.Toast; // Hiển thị thông báo nhanh trên màn hình

import androidx.annotation.NonNull; // Đánh dấu tham số không được null
import androidx.annotation.Nullable; // Đánh dấu tham số có thể null
import androidx.fragment.app.Fragment; // Mảnh giao diện giúp tái sử dụng và quản lý UI
import androidx.navigation.Navigation; // Thư viện điều hướng Jetpack Navigation
import androidx.recyclerview.widget.LinearLayoutManager; // Quản lý bố cục danh sách (cuộn dọc)
import androidx.recyclerview.widget.RecyclerView; // Hiển thị danh sách dữ liệu lớn hiệu quả

import com.example.quanlychitieu.data.database.AppDatabase; // Lớp cơ sở dữ liệu chính
import com.example.quanlychitieu.data.entities.Account; // Thực thể đại diện cho bảng Tài khoản (Ví)
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Nút bấm nổi (+)
import com.google.firebase.auth.FirebaseAuth; // Quản lý xác thực người dùng

 // WalletFragment: Quản lý danh sách các ví tiền và tài khoản ngân hàng của người dùng.

public class WalletFragment extends Fragment {

    private RecyclerView rvAccounts; // Danh sách hiển thị các tài khoản
    private AccountAdapter adapter; // Bộ lọc và đổ dữ liệu vào RecyclerView
    private FloatingActionButton fabAddAccount; // Nút thêm tài khoản mới

    public WalletFragment() {} // Constructor mặc định bắt buộc

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Nạp layout fragment_wallet vào Fragment
        return inflater.inflate(R.layout.fragment_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState); // Khởi tạo các thành phần giao diện sau khi View đã tạo xong

        // Ánh xạ các thành phần từ XML sang Java bằng ID
        rvAccounts = view.findViewById(R.id.rv_accounts); // RecyclerView hiển thị danh sách ví
        fabAddAccount = view.findViewById(R.id.fab_add_account); // Nút dấu cộng nổi
        
        // Nút quay lại: Sử dụng navigateUp() để quay về màn hình trước đó trong luồng điều hướng
        view.findViewById(R.id.btn_back).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Khởi tạo Adapter với callback khi click vào item để mở hộp thoại chỉnh sửa
        adapter = new AccountAdapter(this::showEditAccountDialog);
        // Thiết lập bố cục dọc cho danh sách ví
        rvAccounts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAccounts.setAdapter(adapter); // Gắn adapter vào view

        // Lấy dữ liệu tài khoản từ Room Database theo UID của người dùng hiện tại
        String userId = FirebaseAuth.getInstance().getUid();
        AppDatabase.getDatabase(requireContext()).accountDao().getAllAccounts(userId)
                .observe(getViewLifecycleOwner(), accounts -> adapter.setAccounts(accounts)); // Tự động cập nhật UI khi DB thay đổi

        // Gán sự kiện cho nút thêm mới tài khoản
        fabAddAccount.setOnClickListener(v -> showAddAccountDialog());
    }


     // showAddAccountDialog: Hiển thị hộp thoại để người dùng nhập tên ví và số dư ban đầu.

    private void showAddAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Thêm tài khoản mới");

        // Nạp giao diện tùy chỉnh cho Dialog từ file XML dialog_add_account
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_account, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.et_account_name); // Ô nhập tên ví (VD: Tiền mặt)
        EditText etBalance = dialogView.findViewById(R.id.et_account_balance); // Ô nhập số dư (VD: 1000000)

        // Nút xác nhận thêm tài khoản
        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String balanceStr = etBalance.getText().toString().trim();
            
            // Kiểm tra dữ liệu không được để trống trước khi lưu vào DB
            if (!name.isEmpty() && !balanceStr.isEmpty()) {
                // Tạo đối tượng Account mới với dữ liệu đã nhập
                Account account = new Account(name, Double.parseDouble(balanceStr), "wallet");
                account.setUserId(FirebaseAuth.getInstance().getUid()); // Gán quyền sở hữu cho user hiện tại
                // Lưu vào database ở luồng nền (background thread) để không gây giật lag giao diện
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase.getDatabase(requireContext()).accountDao().insert(account);
                });
            } else {
                // Thông báo lỗi nếu người dùng để trống thông tin
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", null); // Đóng dialog mà không làm gì
        builder.show(); // Hiển thị Dialog lên màn hình
    }


     // showEditAccountDialog: Hiển thị hộp thoại để chỉnh sửa hoặc xóa một ví hiện có.
     //@param account Đối tượng tài khoản cần được thay đổi hoặc xóa.
    private void showEditAccountDialog(Account account) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Sửa tài khoản");

        // Sử dụng lại layout của dialog thêm mới để tiết kiệm tài nguyên và đồng bộ giao diện
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_account, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.et_account_name);
        EditText etBalance = dialogView.findViewById(R.id.et_account_balance);

        // Điền dữ liệu cũ vào các ô nhập để người dùng chỉnh sửa dễ dàng
        etName.setText(account.getName());
        etBalance.setText(String.valueOf(account.getBalance()));

        // Nút Lưu thay đổi: Cập nhật thông tin vào DB
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            account.setName(etName.getText().toString().trim());
            account.setBalance(Double.parseDouble(etBalance.getText().toString().trim()));
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase.getDatabase(requireContext()).accountDao().update(account);
            });
        });
        
        // Nút Xóa: Cho phép người dùng loại bỏ ví này khỏi danh sách quản lý
        builder.setNeutralButton("Xóa", (dialog, which) -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase.getDatabase(requireContext()).accountDao().delete(account);
            });
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
}
