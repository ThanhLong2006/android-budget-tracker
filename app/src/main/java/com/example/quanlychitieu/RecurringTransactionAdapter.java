package com.example.quanlychitieu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.data.entities.RecurringTransaction;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecurringTransactionAdapter extends RecyclerView.Adapter<RecurringTransactionAdapter.RecurringViewHolder> {

    private List<RecurringTransaction> list = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onToggleActive(RecurringTransaction item, boolean active);
        void onDelete(RecurringTransaction item);
    }

    public RecurringTransactionAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setList(List<RecurringTransaction> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecurringViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recurring_transaction, parent, false);
        return new RecurringViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecurringViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class RecurringViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNote, tvAmount, tvFrequency, tvNextDate;
        private MaterialSwitch switchActive;

        public RecurringViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNote = itemView.findViewById(R.id.tv_note);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvFrequency = itemView.findViewById(R.id.tv_frequency);
            tvNextDate = itemView.findViewById(R.id.tv_next_date);
            switchActive = itemView.findViewById(R.id.switch_active);
        }

        public void bind(RecurringTransaction item) {
            tvNote.setText(item.getNote());
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            tvAmount.setText(formatter.format(item.getAmount()));
            
            String freqText = "Hàng tháng";
            if ("DAILY".equals(item.getFrequency())) freqText = "Hàng ngày";
            else if ("WEEKLY".equals(item.getFrequency())) freqText = "Hàng tuần";
            
            tvFrequency.setText(freqText);
            tvNextDate.setText("Ngày tới: " + item.getNextExecutionDate());
            
            switchActive.setOnCheckedChangeListener(null);
            switchActive.setChecked(item.isActive());
            switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onToggleActive(item, isChecked);
            });

            itemView.setOnLongClickListener(v -> {
                listener.onDelete(item);
                return true;
            });
        }
    }
}
