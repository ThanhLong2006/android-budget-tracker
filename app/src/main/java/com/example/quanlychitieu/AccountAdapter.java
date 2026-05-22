package com.example.quanlychitieu;

// Import các thư viện hỗ trợ giao diện danh sách và định dạng
import android.view.LayoutInflater; // Dùng để chuyển đổi file XML layout thành đối tượng View
import android.view.View; // Lớp cơ sở cho các thành phần giao diện
import android.view.ViewGroup; // Lớp chứa các View khác, dùng làm container cho item
import android.widget.TextView; // Thành phần hiển thị văn bản (tên ví, số dư)

import androidx.annotation.NonNull; // Đảm bảo tham số không được null
import androidx.recyclerview.widget.RecyclerView; // Thư viện hiển thị danh sách hiệu năng cao

import com.example.quanlychitieu.data.entities.Account; // Thực thể dữ liệu tài khoản (ví)

import java.text.NumberFormat; // Định dạng số thành chuỗi tiền tệ
import java.util.ArrayList; // Danh sách mảng linh hoạt
import java.util.List; // Giao diện danh sách chuẩn
import java.util.Locale; // Xác định khu vực (Việt Nam) cho định dạng tiền tệ


 // AccountAdapter: Bộ điều hợp để hiển thị danh sách các tài khoản/ví tiền.
 // Sử dụng RecyclerView.Adapter để tối ưu hiệu suất hiển thị và tái sử dụng View.

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    // Danh sách chứa các tài khoản sẽ hiển thị
    private List<Account> accounts = new ArrayList<>();
    // Listener xử lý sự kiện khi người dùng nhấn vào một tài khoản (để sửa/xóa)
    private OnAccountClickListener listener;


     // Giao diện callback để thông báo sự kiện click ra bên ngoài.
     // Chọn giải pháp interface giúp tách biệt logic hiển thị và logic xử lý dữ liệu.

    public interface OnAccountClickListener {
        void onAccountClick(Account account);
    }

    public AccountAdapter(OnAccountClickListener listener) {
        this.listener = listener;
    }


     // setAccounts: Cập nhật danh sách tài khoản mới và yêu cầu làm mới giao diện.
     // @param accounts Danh sách tài khoản mới từ cơ sở dữ liệu

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
        // Thông báo cho RecyclerView rằng dữ liệu đã thay đổi để vẽ lại danh sách
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp layout item_account.xml cho mỗi dòng trong danh sách ví
        // LayoutInflater giúp tạo ra đối tượng View từ file XML thiết kế sẵn
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        // Lấy dữ liệu tài khoản tại vị trí hiện tại trong danh sách
        Account account = accounts.get(position);
        
        // Hiển thị tên tài khoản (VD: Ví MoMo, Tiền mặt)
        holder.tvName.setText(account.getName());
        
        // Định dạng số dư theo chuẩn tiền tệ Việt Nam (VD: 1.000.000 ₫)
        // Chọn Locale("vi", "VN") để đảm bảo đúng ký hiệu và cách phân cách phần nghìn
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvBalance.setText(formatter.format(account.getBalance()));

        // Thiết lập sự kiện click cho toàn bộ vùng item của ví
        // Khi người dùng nhấn vào, gọi hàm callback đã được định nghĩa ở Fragment
        holder.itemView.setOnClickListener(v -> listener.onAccountClick(account));
    }

    @Override
    public int getItemCount() {
        // Trả về tổng số lượng tài khoản có trong danh sách
        return accounts.size();
    }


     // AccountViewHolder: Lớp nắm giữ các View của một item ví để tăng tốc độ truy cập.
     // Tránh việc phải gọi findViewById nhiều lần khi cuộn danh sách.

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBalance; // Các thành phần hiển thị thông tin ví

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ các thành phần UI từ layout item_account
            tvName = itemView.findViewById(R.id.tv_account_name);
            tvBalance = itemView.findViewById(R.id.tv_account_balance);
        }
    }
}
