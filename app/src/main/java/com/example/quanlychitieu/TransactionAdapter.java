package com.example.quanlychitieu;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.data.dao.TransactionDao;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TransactionAdapter: Hiển thị danh sách giao dịch kèm biểu tượng và màu sắc của danh mục.
 * Từng dòng (item) sẽ phản ánh đúng tính chất của giao dịch (Thu/Chi).
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    // Sử dụng TransactionWithCategory để có đầy đủ thông tin icon và màu sắc từ bảng categories
    private List<TransactionDao.TransactionWithCategory> transactions = new ArrayList<>();
    private final OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionDao.TransactionWithCategory transaction);
    }

    public TransactionAdapter(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    /**
     * setTransactions: Cập nhật danh sách mới từ Dashboard hoặc Lịch sử.
     */
    public void setTransactions(List<TransactionDao.TransactionWithCategory> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp layout item_transaction cho từng dòng
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        // Gắn dữ liệu thực tế vào ViewHolder
        TransactionDao.TransactionWithCategory transaction = transactions.get(position);
        holder.bind(transaction, listener);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNote, tvDate, tvAmount;
        private final ImageView imgIcon;
        private final View iconContainer;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ View từ layout item_transaction.xml
            tvNote = itemView.findViewById(R.id.tv_note);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            imgIcon = itemView.findViewById(R.id.img_category_icon);
            iconContainer = itemView.findViewById(R.id.icon_container);
        }

        public void bind(TransactionDao.TransactionWithCategory transaction, OnTransactionClickListener listener) {
            // 1. Hiển thị Ghi chú, nếu rỗng thì hiện tên Danh mục
            String displayNote = (transaction.note != null && !transaction.note.isEmpty()) 
                    ? transaction.note : transaction.categoryName;
            tvNote.setText(displayNote);
            
            // 2. Hiển thị ngày tháng
            tvDate.setText(transaction.date);

            // 3. Định dạng số tiền VNĐ
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            String amountStr = formatter.format(transaction.amount);

            // 4. Xử lý hiển thị Icon và Màu sắc dựa trên Danh mục
            if (transaction.categoryIcon != null) {
                int resId = itemView.getContext().getResources().getIdentifier(
                        transaction.categoryIcon, "drawable", itemView.getContext().getPackageName());
                if (resId != 0) imgIcon.setImageResource(resId);
            }

            if (transaction.categoryColor != null) {
                try {
                    // Tạo màu nền tròn cho icon
                    GradientDrawable bg = (GradientDrawable) ContextCompat.getDrawable(
                            itemView.getContext(), R.drawable.circle_gray).mutate();
                    bg.setColor(Color.parseColor(transaction.categoryColor));
                    iconContainer.setBackground(bg);
                } catch (Exception e) {
                    iconContainer.setBackgroundResource(R.drawable.circle_gray);
                }
            }

            // 5. Phân biệt màu sắc Thu nhập (+) và Chi tiêu (-)
            if ("INCOME".equals(transaction.type)) {
                tvAmount.setText("+" + amountStr);
                tvAmount.setTextColor(Color.parseColor("#22C55E")); // Màu xanh dương/lá
            } else {
                tvAmount.setText("-" + amountStr);
                tvAmount.setTextColor(Color.parseColor("#EF4444")); // Màu đỏ
            }

            // Sự kiện click để sửa giao dịch
            itemView.setOnClickListener(v -> listener.onTransactionClick(transaction));
        }
    }
}
