package com.example.quanlychitieu;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.data.entities.DebtLoan;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DebtLoanAdapter extends RecyclerView.Adapter<DebtLoanAdapter.DebtLoanViewHolder> {

    private List<DebtLoan> debtLoans = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DebtLoan debtLoan);
    }

    public DebtLoanAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setDebtLoans(List<DebtLoan> debtLoans) {
        this.debtLoans = debtLoans;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DebtLoanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_debt_loan, parent, false);
        return new DebtLoanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DebtLoanViewHolder holder, int position) {
        holder.bind(debtLoans.get(position));
    }

    @Override
    public int getItemCount() {
        return debtLoans.size();
    }

    class DebtLoanViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPersonName, tvAmount, tvNote, tvDueDate, tvStatus;

        public DebtLoanViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPersonName = itemView.findViewById(R.id.tv_person_name);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvNote = itemView.findViewById(R.id.tv_note);
            tvDueDate = itemView.findViewById(R.id.tv_due_date);
            tvStatus = itemView.findViewById(R.id.tv_status);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(debtLoans.get(pos));
                }
            });
        }

        public void bind(DebtLoan item) {
            tvPersonName.setText(item.getPersonName());
            
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            tvAmount.setText(formatter.format(item.getAmount()));
            
            // Màu sắc dựa trên loại: Vay (LOAN) màu xanh, Nợ (DEBT) màu đỏ
            if ("LOAN".equals(item.getType())) {
                tvAmount.setTextColor(Color.parseColor("#22C55E")); // Xanh lá
            } else {
                tvAmount.setTextColor(Color.RED);
            }

            tvNote.setText(item.getNote());
            tvDueDate.setText("Hạn trả: " + item.getDueDate());
            
            if (item.isPaid()) {
                tvStatus.setText("ĐÃ TRẢ");
                tvStatus.setTextColor(Color.GRAY);
            } else {
                tvStatus.setText("CHƯA TRẢ");
                tvStatus.setTextColor(item.getType().equals("LOAN") ? Color.parseColor("#22C55E") : Color.RED);
            }
        }
    }
}
