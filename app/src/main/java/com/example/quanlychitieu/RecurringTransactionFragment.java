package com.example.quanlychitieu;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.data.database.AppDatabase;
import com.example.quanlychitieu.data.entities.Account;
import com.example.quanlychitieu.data.entities.Category;
import com.example.quanlychitieu.data.entities.RecurringTransaction;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecurringTransactionFragment extends Fragment {

    private RecyclerView rvRecurring;
    private RecurringTransactionAdapter adapter;
    private FloatingActionButton fabAdd;
    private AppDatabase db;
    private String userId;

    public RecurringTransactionFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recurring_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getDatabase(requireContext());
        userId = FirebaseAuth.getInstance().getUid();

        rvRecurring = view.findViewById(R.id.rv_recurring);
        fabAdd = view.findViewById(R.id.fab_add_recurring);

        adapter = new RecurringTransactionAdapter(new RecurringTransactionAdapter.OnItemClickListener() {
            @Override
            public void onToggleActive(RecurringTransaction item, boolean active) {
                item.setActive(active);
                item.setLastUpdated(System.currentTimeMillis());
                AppDatabase.databaseWriteExecutor.execute(() -> db.recurringTransactionDao().update(item));
            }

            @Override
            public void onDelete(RecurringTransaction item) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Xóa giao dịch định kỳ")
                        .setMessage("Bạn có chắc chắn muốn xóa mục này?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            AppDatabase.databaseWriteExecutor.execute(() -> db.recurringTransactionDao().delete(item));
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });

        rvRecurring.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecurring.setAdapter(adapter);

        view.findViewById(R.id.toolbar).setOnClickListener(v -> requireActivity().onBackPressed());

        db.recurringTransactionDao().getAllRecurringTransactions(userId).observe(getViewLifecycleOwner(), list -> {
            adapter.setList(list);
        });

        fabAdd.setOnClickListener(v -> showAddDialog());
    }

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_recurring, null);
        EditText etNote = dialogView.findViewById(R.id.et_recurring_note);
        EditText etAmount = dialogView.findViewById(R.id.et_recurring_amount);
        Spinner spFrequency = dialogView.findViewById(R.id.sp_frequency);
        Spinner spCategory = dialogView.findViewById(R.id.sp_category);
        Spinner spAccount = dialogView.findViewById(R.id.sp_account);
        EditText etStartDate = dialogView.findViewById(R.id.et_start_date);

        // Setup Frequency Spinner
        String[] frequencies = {"DAILY", "WEEKLY", "MONTHLY"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, frequencies);
        spFrequency.setAdapter(freqAdapter);

        // Load Categories and Accounts for Spinners
        List<Category> allCategories = new ArrayList<>();
        List<Account> allAccounts = new ArrayList<>();

        db.categoryDao().getAllCategoriesSync().forEach(c -> { if(c.getUserId() == null || c.getUserId().equals(userId)) allCategories.add(c); });
        db.accountDao().getAllAccounts(userId).observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null) {
                allAccounts.clear();
                allAccounts.addAll(accounts);
                List<String> accountNames = new ArrayList<>();
                for (Account a : accounts) accountNames.add(a.getName());
                spAccount.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, accountNames));
            }
        });

        List<String> categoryNames = new ArrayList<>();
        for (Category c : allCategories) categoryNames.add(c.getName());
        spCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categoryNames));

        Calendar calendar = Calendar.getInstance();
        etStartDate.setOnClickListener(v -> {
            new DatePickerDialog(getContext(), (view, year, month, day) -> {
                calendar.set(year, month, day);
                etStartDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("Thêm giao dịch định kỳ")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String note = etNote.getText().toString();
                    String amountStr = etAmount.getText().toString();
                    String date = etStartDate.getText().toString();
                    
                    if (amountStr.isEmpty() || date.isEmpty() || spCategory.getSelectedItem() == null || spAccount.getSelectedItem() == null) {
                        Toast.makeText(getContext(), "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    RecurringTransaction rt = new RecurringTransaction();
                    rt.setUserId(userId);
                    rt.setNote(note);
                    rt.setAmount(Double.parseDouble(amountStr));
                    rt.setFrequency(spFrequency.getSelectedItem().toString());
                    rt.setStartDate(date);
                    rt.setNextExecutionDate(date);
                    rt.setCategoryId(allCategories.get(spCategory.getSelectedItemPosition()).getId());
                    rt.setAccountId(allAccounts.get(spAccount.getSelectedItemPosition()).getId());
                    rt.setType(allCategories.get(spCategory.getSelectedItemPosition()).getType());

                    AppDatabase.databaseWriteExecutor.execute(() -> db.recurringTransactionDao().insert(rt));
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
