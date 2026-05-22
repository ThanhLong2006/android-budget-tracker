package com.example.quanlychitieu;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * IconAdapter: Hiển thị danh sách các biểu tượng để người dùng lựa chọn.
 */
public class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconViewHolder> {

    private final List<String> iconNames; // Danh sách tên resource của icon
    private final List<String> colors; // Danh sách mã màu
    private final OnIconSelectedListener listener;

    public interface OnIconSelectedListener {
        void onIconSelected(String iconName, String color);
    }

    public IconAdapter(List<String> iconNames, List<String> colors, OnIconSelectedListener listener) {
        this.iconNames = iconNames;
        this.colors = colors;
        this.listener = listener;
    }

    @NonNull
    @Override
    public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp layout item cho từng ô icon
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon_picker, parent, false);
        return new IconViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
        String iconName = iconNames.get(position % iconNames.size());
        String color = colors.get(position % colors.size());
        holder.bind(iconName, color, listener);
    }

    @Override
    public int getItemCount() {
        // Trả về số lượng tổ hợp icon + màu (giới hạn thực tế)
        return Math.max(iconNames.size(), colors.size());
    }

    static class IconViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;

        public IconViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
        }

        public void bind(String iconName, String color, OnIconSelectedListener listener) {
            // Hiển thị icon từ tài nguyên hệ thống
            int resId = itemView.getContext().getResources().getIdentifier(iconName, "drawable", itemView.getContext().getPackageName());
            if (resId != 0) ivIcon.setImageResource(resId);
            
            // Đặt màu sắc cho icon
            ivIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            ivIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(color)));

            // Sự kiện chọn icon
            itemView.setOnClickListener(v -> listener.onIconSelected(iconName, color));
        }
    }
}
