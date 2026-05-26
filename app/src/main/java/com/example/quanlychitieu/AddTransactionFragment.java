package com.example.quanlychitieu;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.quanlychitieu.data.database.AppDatabase;
import com.example.quanlychitieu.data.entities.Account;
import com.example.quanlychitieu.data.entities.Budget;
import com.example.quanlychitieu.data.entities.Category;
import com.example.quanlychitieu.data.entities.Transaction;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddTransactionFragment extends Fragment {

    private EditText etAmount, etNote;
    private TextView tvDate, tvCategoryName, tvAccountName, tvTitle;
    private ImageView ivCategoryIcon, ivReceiptPreview;
    private MaterialButtonToggleGroup toggleType;
    private Button btnSave;
    private View btnSelectDate, btnSelectCategory, btnSelectAccount, btnClose, btnScan, containerReceipt, btnRemoveImage;

    private Calendar calendar = Calendar.getInstance();
    private String selectedType = "EXPENSE";
    private int selectedCategoryId = -1;
    private int selectedAccountId = -1;
    private int transactionId = -1;
    private Uri localImageUri = null;
    private String existingImageUrl = null;

    private String currentPickingIcon = "outline_article_person_24";
    private String currentPickingColor = "#3B82F6";

    private List<Account> accountList = new ArrayList<>();
    private List<Category> incomeCategories = new ArrayList<>();
    private List<Category> expenseCategories = new ArrayList<>();

    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    public AddTransactionFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        takePhotoLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && localImageUri != null) {
                showImagePreview(localImageUri);
                processReceiptImage(localImageUri);
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                localImageUri = uri;
                showImagePreview(uri);
            }
        });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) openCamera();
            else Toast.makeText(getContext(), "Cần quyền Camera", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        if (getArguments() != null) {
            transactionId = getArguments().getInt("transactionId", -1);
        }
        setupUI();
        observeData();
        if (transactionId != -1) loadTransactionData();
    }

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tv_title);
        etAmount = view.findViewById(R.id.et_amount);
        etNote = view.findViewById(R.id.et_note);
        tvDate = view.findViewById(R.id.tv_date);
        tvCategoryName = view.findViewById(R.id.tv_category_name);
        ivCategoryIcon = view.findViewById(R.id.iv_category_icon);
        tvAccountName = view.findViewById(R.id.tv_account_name);
        toggleType = view.findViewById(R.id.toggle_type);
        btnSave = view.findViewById(R.id.btn_save);
        btnSelectDate = view.findViewById(R.id.btn_select_date);
        btnSelectCategory = view.findViewById(R.id.btn_select_category);
        btnSelectAccount = view.findViewById(R.id.btn_select_account);
        btnClose = view.findViewById(R.id.btn_close);
        btnScan = view.findViewById(R.id.btn_scan_receipt);
        ivReceiptPreview = view.findViewById(R.id.iv_receipt_preview);
        containerReceipt = view.findViewById(R.id.container_receipt_image);
        btnRemoveImage = view.findViewById(R.id.btn_remove_image);
    }

    private void setupUI() {
        updateDateLabel();
        if (transactionId == -1) {
            toggleType.check(R.id.btn_type_expense);
            updateToggleColors(R.id.btn_type_expense);
        }
        btnClose.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        btnSelectDate.setOnClickListener(v -> {
            new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateLabel();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                selectedType = (checkedId == R.id.btn_type_income) ? "INCOME" : "EXPENSE";
                updateToggleColors(checkedId);
            }
        });
        btnSelectCategory.setOnClickListener(v -> showCategorySelectionDialog());
        btnSelectAccount.setOnClickListener(v -> showAccountSelectionDialog());
        btnSave.setOnClickListener(v -> validateAndSave());
        
        btnScan.setOnClickListener(v -> {
            String[] options = {"Chụp ảnh hóa đơn", "Chọn từ thư viện"};
            new AlertDialog.Builder(getContext())
                    .setTitle("Đính kèm hóa đơn")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera();
                            else requestPermissionLauncher.launch(Manifest.permission.CAMERA);
                        } else pickImageLauncher.launch("image/*");
                    }).show();
        });

        btnRemoveImage.setOnClickListener(v -> {
            localImageUri = null;
            existingImageUrl = null;
            containerReceipt.setVisibility(View.GONE);
        });
    }

    private void openCamera() {
        try {
            File photoFile = File.createTempFile("receipt_", ".jpg", requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            localImageUri = FileProvider.getUriForFile(requireContext(), "com.example.quanlychitieu.fileprovider", photoFile);
            takePhotoLauncher.launch(localImageUri);
        } catch (IOException e) {
            Toast.makeText(getContext(), "Lỗi Camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void showImagePreview(Uri uri) {
        containerReceipt.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).into(ivReceiptPreview);
    }

    private void processReceiptImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image).addOnSuccessListener(visionText -> extractAmountFromText(visionText.getText()));
        } catch (IOException ignored) {}
    }

    private void extractAmountFromText(String text) {
        Pattern pattern = Pattern.compile("(\\d{1,3}([.,]\\d{3})*|\\d+)");
        Matcher matcher = pattern.matcher(text);
        double maxAmount = 0;
        while (matcher.find()) {
            try {
                String clean = matcher.group().replace(".", "").replace(",", "");
                double val = Double.parseDouble(clean);
                if (val > maxAmount && val < 1000000000) maxAmount = val;
            } catch (Exception ignored) {}
        }
        if (maxAmount > 0) {
            etAmount.setText(String.valueOf((int)maxAmount));
            Toast.makeText(getContext(), "Đã nhận diện số tiền: " + formatCurrency(maxAmount), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateToggleColors(int checkedId) {
        Button btnExpense = getView().findViewById(R.id.btn_type_expense);
        Button btnIncome = getView().findViewById(R.id.btn_type_income);
        if (checkedId == R.id.btn_type_expense) {
            btnExpense.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.expense_red)));
            btnExpense.setTextColor(Color.WHITE);
            btnIncome.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray_light)));
            btnIncome.setTextColor(Color.GRAY);
        } else {
            btnIncome.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.income_green)));
            btnIncome.setTextColor(Color.WHITE);
            btnExpense.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray_light)));
            btnExpense.setTextColor(Color.GRAY);
        }
    }

    private void observeData() {
        String userId = FirebaseAuth.getInstance().getUid();
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        db.accountDao().getAllAccounts(userId).observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null && !accounts.isEmpty()) {
                accountList = accounts;
                if (selectedAccountId == -1 && transactionId == -1) {
                    selectedAccountId = accounts.get(0).getId();
                    tvAccountName.setText(accounts.get(0).getName());
                }
            }
        });
        db.categoryDao().getCategoriesByType(userId, "INCOME").observe(getViewLifecycleOwner(), categories -> { if (categories != null) incomeCategories = categories; });
        db.categoryDao().getCategoriesByType(userId, "EXPENSE").observe(getViewLifecycleOwner(), categories -> { if (categories != null) expenseCategories = categories; });
    }

    private void loadTransactionData() {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        db.transactionDao().getTransactionById(transactionId).observe(getViewLifecycleOwner(), transaction -> {
            if (transaction != null) {
                tvTitle.setText("Sửa giao dịch");
                etAmount.setText(String.valueOf((int)transaction.getAmount()));
                etNote.setText(transaction.getNote());
                selectedType = transaction.getType();
                selectedCategoryId = transaction.getCategoryID();
                selectedAccountId = transaction.getAccountId();
                existingImageUrl = transaction.getImageUrl();
                if (existingImageUrl != null) {
                    containerReceipt.setVisibility(View.VISIBLE);
                    Glide.with(this).load(existingImageUrl).into(ivReceiptPreview);
                }
                toggleType.check(selectedType.equals("INCOME") ? R.id.btn_type_income : R.id.btn_type_expense);
                updateToggleColors(selectedType.equals("INCOME") ? R.id.btn_type_income : R.id.btn_type_expense);
                try {
                    calendar.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(transaction.getDate()));
                    updateDateLabel();
                } catch (Exception ignored) {}
                db.categoryDao().getCategoryById(selectedCategoryId).observe(getViewLifecycleOwner(), c -> { if (c != null) { tvCategoryName.setText(c.getName()); ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(c.getColor()))); } });
                db.accountDao().getAccountById(selectedAccountId).observe(getViewLifecycleOwner(), a -> { if (a != null) tvAccountName.setText(a.getName()); });
            }
        });
    }

    private void validateAndSave() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty() || selectedCategoryId == -1 || selectedAccountId == -1) {
            Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount = Double.parseDouble(amountStr);
        String note = etNote.getText().toString();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        if (localImageUri != null) {
            uploadImageAndSave(amount, note, date);
        } else {
            performSave(amount, note, date, existingImageUrl);
        }
    }

    private void uploadImageAndSave(double amount, String note, String date) {
        Toast.makeText(getContext(), "Đang tải ảnh...", Toast.LENGTH_SHORT).show();
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("receipts/" + UUID.randomUUID().toString());
        ref.putFile(localImageUri).addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
            performSave(amount, note, date, uri.toString());
        })).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            performSave(amount, note, date, null);
        });
    }

    private void performSave(double amount, String note, String date, String imageUrl) {
        String userId = FirebaseAuth.getInstance().getUid();
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        Transaction t = new Transaction();
        if (transactionId != -1) t.setId(transactionId);
        t.setAmount(amount);
        t.setType(selectedType);
        t.setAccountId(selectedAccountId);
        t.setCategoryID(selectedCategoryId);
        t.setNote(note);
        t.setDate(date);
        t.setUserId(userId);
        t.setImageUrl(imageUrl);
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (transactionId == -1) db.transactionDao().insert(t);
            else db.transactionDao().update(t);
            BalanceWidget.updateWidget(requireContext());
            if (getActivity() != null) getActivity().runOnUiThread(() -> Navigation.findNavController(requireView()).navigateUp());
        });
    }

    private void showCategorySelectionDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_category_selection, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        RecyclerView rvCategories = dialogView.findViewById(R.id.rv_categories);
        MaterialButtonToggleGroup toggleFilter = dialogView.findViewById(R.id.toggle_filter_type);
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        CategoryAdapter adapter = new CategoryAdapter(category -> {
            selectedCategoryId = category.getId();
            tvCategoryName.setText(category.getName());
            ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(category.getColor())));
            dialog.dismiss();
        });
        rvCategories.setAdapter(adapter);
        toggleFilter.check("INCOME".equals(selectedType) ? R.id.btn_filter_income : R.id.btn_filter_expense);
        adapter.setCategories("INCOME".equals(selectedType) ? incomeCategories : expenseCategories);
        toggleFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> { if (isChecked) adapter.setCategories(checkedId == R.id.btn_filter_income ? incomeCategories : expenseCategories); });
        dialog.show();
    }

    private void showAccountSelectionDialog() {
        String[] names = new String[accountList.size()];
        for (int i = 0; i < accountList.size(); i++) names[i] = accountList.get(i).getName();
        new AlertDialog.Builder(requireContext()).setTitle("Chọn tài khoản").setItems(names, (dialog, which) -> {
            selectedAccountId = accountList.get(which).getId();
            tvAccountName.setText(accountList.get(which).getName());
        }).show();
    }

    private void updateDateLabel() { tvDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime())); }
    private String formatCurrency(double amount) { return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount); }
}
