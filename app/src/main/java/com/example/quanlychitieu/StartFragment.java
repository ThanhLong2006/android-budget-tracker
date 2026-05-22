package com.example.quanlychitieu;

// Import các thư viện Android và Firebase cần thiết
import android.os.Bundle; // Đối tượng dùng để truyền dữ liệu giữa các thành phần
import android.view.LayoutInflater; // Chuyển đổi file XML layout thành đối tượng View
import android.view.View; // Lớp cơ sở cho các thành phần giao diện
import android.view.ViewGroup; // Chứa các View khác
import android.widget.Button; // Thành phần nút bấm

import androidx.annotation.NonNull; // Đánh dấu tham số không được null
import androidx.annotation.Nullable; // Đánh dấu tham số có thể null
import androidx.fragment.app.Fragment; // Mảnh giao diện giúp quản lý UI linh hoạt
import androidx.navigation.Navigation; // Thành phần điều hướng giữa các màn hình

import com.google.firebase.auth.FirebaseAuth; // Quản lý xác thực người dùng từ Firebase
import com.google.firebase.auth.FirebaseUser; // Đại diện cho người dùng hiện tại trong Firebase


 // StartFragment: Màn hình chào mừng và kiểm tra trạng thái đăng nhập khi khởi động ứng dụng.
 // Chọn Fragment thay vì Activity để đồng bộ với luồng Navigation toàn ứng dụng.

public class StartFragment extends Fragment {

    public StartFragment() {
        // Constructor mặc định bắt buộc cho Fragment để hệ thống có thể tái tạo lại khi cần
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Nạp layout XML fragment_start cho màn hình bắt đầu
        // Chọn false để không gắn vào container ngay lập tức, hệ thống sẽ tự làm việc này
        return inflater.inflate(R.layout.fragment_start, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState); // Gọi hàm lớp cha để đảm bảo Fragment khởi tạo đúng

        // Khởi tạo instance của Firebase Auth để kiểm tra phiên đăng nhập
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        // Lấy thông tin người dùng hiện tại nếu có (phiên đăng nhập vẫn còn hiệu lực)
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Ánh xạ nút "Bắt đầu" từ file layout XML
        Button btnStart = view.findViewById(R.id.btn_start);
        
        // Thiết lập sự kiện click cho nút bắt đầu
        btnStart.setOnClickListener(v -> {
            // Kiểm tra trạng thái đăng nhập để quyết định màn hình tiếp theo
            // Việc này giúp người dùng không phải đăng nhập lại mỗi khi mở app (tăng trải nghiệm)
            if (currentUser != null) {
                // Nếu đã đăng nhập, điều hướng trực tiếp vào trang chủ (Dashboard)
                Navigation.findNavController(v).navigate(R.id.action_start_to_dashboard);
            } else {
                // Nếu chưa đăng nhập, điều hướng đến màn hình Đăng nhập
                Navigation.findNavController(v).navigate(R.id.action_start_to_login);
            }
        });
    }
}
