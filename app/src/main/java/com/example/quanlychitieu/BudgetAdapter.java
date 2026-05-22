package com.example.quanlychitieu;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.data.dao.BudgetDao;
import com.example.quanlychitieu.data.database.AppDatabase;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<BudgetDao.BudgetWithCategory> budgets = new ArrayList<>();
    private AppDatabase db;
    private LifecycleOwner lifecycleOwner;

    public BudgetAdapter(AppDatabase db, LifecycleOwner lifecycleOwner) {
        this.db = db;
        this.lifecycleOwner = lifecycleOwner;
    }

    public void setBudgets(List<BudgetDao.BudgetWithCategory> budgets) {
        this.budgets = budgets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        BudgetDao.BudgetWithCategory budget = budgets.get(position);
        holder.bind(budget);
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    private String formatCurrency(Double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount != null ? amount : 0.0);
    }

    class BudgetViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCategory, tvAmount, tvStatus;
        private final ImageView ivIcon;
        private final LinearProgressIndicator progressBar;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_budget_category);
            tvAmount = itemView.findViewById(R.id.tv_budget_amount);
            tvStatus = itemView.findViewById(R.id.tv_budget_status);
            ivIcon = itemView.findViewById(R.id.iv_budget_icon);
            progressBar = itemView.findViewById(R.id.progress_budget);
        }

        public void bind(BudgetDao.BudgetWithCategory budget) {
            tvCategory.setText(budget.categoryName);

            if (budget.categoryIcon != null) {
                int resId = itemView.getContext().getResources().getIdentifier(
                        budget.categoryIcon, "drawable", itemView.getContext().getPackageName());
                if (resId != 0) ivIcon.setImageResource(resId);
            }

            if (budget.categoryColor != null) {
                try {
                    GradientDrawable bg = (GradientDrawable) ContextCompat.getDrawable(
                            itemView.getContext(), R.drawable.bg_circle_white).mutate();
                    bg.setColor(Color.parseColor(budget.categoryColor));
                    ivIcon.setBackground(bg);
                    ivIcon.setColorFilter(Color.WHITE);
                } catch (Exception e) {
                    ivIcon.setBackgroundResource(R.drawable.circle_gray);
                }
            }

            double limit = budget.amountLimit;
            double spent = budget.currentSpent;
            tvAmount.setText(formatCurrency(spent) + " / " + formatCurrency(limit));

            int progress = (int) ((spent / limit) * 100);
            progressBar.setProgress(Math.min(progress, 100));

            // LOGIC CẢNH BÁO MỚI: 80% LÀ MÀU CAM, 100% LÀ MÀU ĐỎ
            if (spent >= limit) {
                progressBar.setIndicatorColor(Color.RED);
                tvStatus.setText("ĐÃ VƯỢT HẠN MỨC!");
                tvStatus.setTextColor(Color.RED);
                tvStatus.setTypeface(null, Typeface.BOLD);
            } else if (spent >= limit * 0.8) {
                int warningColor = Color.parseColor("#F59E0B"); // Màu vàng cam rực
                progressBar.setIndicatorColor(warningColor);
                tvStatus.setText("CẢNH BÁO: SẮP HẾT!");
                tvStatus.setTextColor(warningColor);
                tvStatus.setTypeface(null, Typeface.BOLD);
            } else {
                progressBar.setIndicatorColor(Color.parseColor("#22C55E")); // Màu xanh lá
                tvStatus.setText("Còn lại: " + formatCurrency(limit - spent));
                tvStatus.setTextColor(Color.GRAY);
                tvStatus.setTypeface(null, Typeface.NORMAL);
            }
        }
    }
}
