package com.example.quanlychitieu;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.data.dao.TransactionDao;
import com.example.quanlychitieu.data.database.AppDatabase;
import com.example.quanlychitieu.data.entities.Transaction;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class historyFragment extends Fragment {

    private RecyclerView recyclerHistory;
    private TransactionAdapter adapter;
    private TransactionDao transactionDao;
    private TextView tvEmpty;
    private MaterialButtonToggleGroup toggleGroup;
    private ImageButton btnExport;
    private LiveData<List<TransactionDao.TransactionWithCategory>> currentLiveData;
    private String userId;

    public historyFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Navigation.findNavController(view).navigate(R.id.nav_start);
            return;
        }

        recyclerHistory = view.findViewById(R.id.recycler_history);
        tvEmpty = view.findViewById(R.id.tv_empty);
        toggleGroup = view.findViewById(R.id.toggle_group);
        btnExport = view.findViewById(R.id.btn_export);

        transactionDao = AppDatabase.getDatabase(requireContext()).transactionDao();

        setupRecyclerView();
        setupFilters();
        
        btnExport.setOnClickListener(v -> exportTransactionsToCSV());

        toggleGroup.check(R.id.btn_all);
        updateFilterUI(R.id.btn_all);
        loadTransactions("ALL");
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(transaction -> {
            Bundle bundle = new Bundle();
            bundle.putInt("transactionId", transaction.id);
            Navigation.findNavController(requireView()).navigate(R.id.action_nav_history_to_addTransactionFragment, bundle);
        });
        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerHistory.setAdapter(adapter);
    }

    private void setupFilters() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateFilterUI(checkedId);
                if (checkedId == R.id.btn_all) loadTransactions("ALL");
                else if (checkedId == R.id.btn_income) loadTransactions("INCOME");
                else if (checkedId == R.id.btn_expense) loadTransactions("EXPENSE");
            }
        });
    }

    private void updateFilterUI(int checkedId) {
        Button btnAll = getView().findViewById(R.id.btn_all);
        Button btnIncome = getView().findViewById(R.id.btn_income);
        Button btnExpense = getView().findViewById(R.id.btn_expense);

        btnAll.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        btnAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        btnIncome.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        btnIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        btnExpense.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        btnExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        if (checkedId == R.id.btn_all) {
            btnAll.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray_light)));
            btnAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
        } else if (checkedId == R.id.btn_income) {
            btnIncome.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.xanhlanhat)));
            btnIncome.setTextColor(Color.parseColor("#22C55E"));
        } else if (checkedId == R.id.btn_expense) {
            btnExpense.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.dohongnhat)));
            btnExpense.setTextColor(Color.parseColor("#EF4444"));
        }
    }

    private void loadTransactions(String filter) {
        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
        }

        currentLiveData = transactionDao.getAllTransactionsWithCategory(userId);
        currentLiveData.observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                List<TransactionDao.TransactionWithCategory> filteredList = transactions;
                if (!"ALL".equals(filter)) {
                    filteredList = new ArrayList<>();
                    for (TransactionDao.TransactionWithCategory t : transactions) {
                        if (filter.equals(t.type)) filteredList.add(t);
                    }
                }
                adapter.setTransactions(filteredList);
                tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void exportTransactionsToCSV() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Transaction> transactions = transactionDao.getAllTransactionsSync(userId);
            if (transactions == null || transactions.isEmpty()) {
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Không có dữ liệu để xuất", Toast.LENGTH_SHORT).show());
                return;
            }

            File exportDir = new File(requireContext().getExternalFilesDir(null), "Exports");
            if (!exportDir.exists()) exportDir.mkdirs();

            File file = new File(exportDir, "WealthFlow_History.csv");
            try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
                String[] header = {"ID", "Ngày", "Số tiền", "Loại", "Ghi chú"};
                writer.writeNext(header);

                for (Transaction t : transactions) {
                    writer.writeNext(new String[]{
                            String.valueOf(t.getId()),
                            t.getDate(),
                            String.valueOf(t.getAmount()),
                            t.getType(),
                            t.getNote()
                    });
                }

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đã xuất file: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    shareFile(file);
                });
            } catch (IOException e) {
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Lỗi khi xuất file", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(requireContext(), "com.example.quanlychitieu.fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Chia sẻ báo cáo"));
    }
}
