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

import com.example.quanlychitieu.data.entities.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * CategoryAdapter: Hiển thị danh sách danh mục với Icon và Màu sắc tương ứng.
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categories = new ArrayList<>();
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_select, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category, listener);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final ImageView ivIcon;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_category_name);
            ivIcon = itemView.findViewById(R.id.iv_category_icon);
        }

        public void bind(Category category, OnCategoryClickListener listener) {
            tvName.setText(category.getName());
            
            // Xử lý hiển thị Icon từ tên Resource
            if (category.getIcon() != null && !category.getIcon().isEmpty()) {
                int resId = itemView.getContext().getResources().getIdentifier(
                        category.getIcon(), "drawable", itemView.getContext().getPackageName());
                if (resId != 0) {
                    ivIcon.setImageResource(resId);
                } else {
                    ivIcon.setImageResource(R.drawable.outline_article_person_24); // Fallback icon
                }
            }

            // Xử lý hiển thị màu sắc background của Icon
            if (category.getColor() != null && !category.getColor().isEmpty()) {
                try {
                    GradientDrawable bg = (GradientDrawable) ContextCompat.getDrawable(
                            itemView.getContext(), R.drawable.bg_circle_white).mutate();
                    bg.setColor(Color.parseColor(category.getColor()));
                    ivIcon.setBackground(bg);
                    ivIcon.setColorFilter(Color.WHITE); // Icon luôn hiện màu trắng trên nền màu
                } catch (Exception e) {
                    ivIcon.setBackgroundResource(R.drawable.circle_gray);
                }
            }

            itemView.setOnClickListener(v -> listener.onCategoryClick(category));
        }
    }
}
