package com.example.quanlychitieu;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.data.database.AppDatabase;
import com.example.quanlychitieu.data.entities.DebtLoan;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DebtLoanFragment extends Fragment {

    private RecyclerView rvDebtLoan;
    private DebtLoanAdapter adapter;
    private TabLayout tabLayout;
    private FloatingActionButton fabAdd;
    private String userId;
    private AppDatabase db;

    public DebtLoanFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debt_loan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getDatabase(requireContext());
        userId = FirebaseAuth.getInstance().getUid();

        rvDebtLoan = view.findViewById(R.id.rv_debt_loan);
        tabLayout = view.findViewById(R.id.tab_layout);
        fabAdd = view.findViewById(R.id.fab_add_debt_loan);

        adapter = new DebtLoanAdapter(this::showEditStatusDialog);
        rvDebtLoan.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDebtLoan.setAdapter(adapter);

        view.findViewById(R.id.toolbar).setOnClickListener(v -> requireActivity().onBackPressed());

        setupTabs();
        loadData("ALL");

        fabAdd.setOnClickListener(v -> showAddDialog());
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: loadData("ALL"); break;
                    case 1: loadData("DEBT"); break;
                    case 2: loadData("LOAN"); break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadData(String filter) {
        if (filter.equals("ALL")) {
            db.debtLoanDao().getAllDebtLoans(userId).observe(getViewLifecycleOwner(), list -> adapter.setDebtLoans(list));
        } else {
            db.debtLoanDao().getDebtLoansByType(userId, filter).observe(getViewLifecycleOwner(), list -> adapter.setDebtLoans(list));
        }
    }

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_debt_loan, null);
        EditText etName = dialogView.findViewById(R.id.et_person_name);
        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        EditText etDate = dialogView.findViewById(R.id.et_due_date);
        EditText etNote = dialogView.findViewById(R.id.et_note);
        RadioGroup rgType = dialogView.findViewById(R.id.rg_type);

        Calendar calendar = Calendar.getInstance();
        etDate.setOnClickListener(v -> {
            new DatePickerDialog(getContext(), (view, year, month, day) -> {
                calendar.set(year, month, day);
                etDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("Thêm khoản Vay/Nợ")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String amountStr = etAmount.getText().toString().trim();
                    if (name.isEmpty() || amountStr.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DebtLoan item = new DebtLoan();
                    item.setUserId(userId);
                    item.setPersonName(name);
                    item.setAmount(Double.parseDouble(amountStr));
                    item.setType(rgType.getCheckedRadioButtonId() == R.id.rb_debt ? "DEBT" : "LOAN");
                    item.setDueDate(etDate.getText().toString());
                    item.setNote(etNote.getText().toString());
                    item.setPaid(false);

                    AppDatabase.databaseWriteExecutor.execute(() -> db.debtLoanDao().insert(item));
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditStatusDialog(DebtLoan debtLoan) {
        String action = debtLoan.isPaid() ? "Đánh dấu là chưa trả?" : "Đánh dấu là đã trả?";
        new AlertDialog.Builder(requireContext())
                .setTitle("Cập nhật trạng thái")
                .setMessage(action)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    debtLoan.setPaid(!debtLoan.isPaid());
                    debtLoan.setLastUpdated(System.currentTimeMillis());
                    AppDatabase.databaseWriteExecutor.execute(() -> db.debtLoanDao().update(debtLoan));
                })
                .setNeutralButton("Xóa", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> db.debtLoanDao().delete(debtLoan));
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
