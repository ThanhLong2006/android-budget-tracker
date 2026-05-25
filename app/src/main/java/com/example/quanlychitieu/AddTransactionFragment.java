package com.example.quanlychitieu;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import com.example.quanlychitieu.data.database.AppDatabase;
import com.example.quanlychitieu.data.entities.Account;
import com.example.quanlychitieu.data.entities.Budget;
import com.example.quanlychitieu.data.entities.Category;
import com.example.quanlychitieu.data.entities.Transaction;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddTransactionFragment extends Fragment {

    private EditText etAmount, etNote;
    private TextView tvDate, tvCategoryName, tvAccountName, tvTitle;
    private ImageView ivCategoryIcon;
    private MaterialButtonToggleGroup toggleType;
    private Button btnSave;
    private View btnSelectDate, btnSelectCategory, btnSelectAccount, btnClose, btnScan;

    private Calendar calendar = Calendar.getInstance();
    private String selectedType = "EXPENSE";
    private int selectedCategoryId = -1;
    private int selectedAccountId = -1;
    private int transactionId = -1;

    private String currentPickingIcon = "outline_article_person_24";
    private String currentPickingColor = "#3B82F6";

    private List<Account> accountList = new ArrayList<>();
    private List<Category> incomeCategories = new ArrayList<>();
    private List<Category> expenseCategories = new ArrayList<>();

    private Uri photoUri;
    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    public AddTransactionFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Khởi tạo trình chụp ảnh
        takePhotoLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && photoUri != null) {
                processReceiptImage(photoUri);
            }
        });

        // Khởi tạo trình yêu cầu quyền
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isChecked(isGranted)) {
                openCamera();
            } else {
                Toast.makeText(getContext(), "Cần quyền Camera để quét hóa đơn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isChecked(Boolean bool) { return bool != null && bool; }

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
        if (transactionId != -1) {
            loadTransactionData();
        }
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
                selectedCategoryId = -1;
                tvCategoryName.setText("Chọn danh mục");
                ivCategoryIcon.setImageResource(R.drawable.outline_add_shopping_cart_24);
                ivCategoryIcon.setBackgroundTintList(null);
            }
        });
        btnSelectCategory.setOnClickListener(v -> showCategorySelectionDialog());
        btnSelectAccount.setOnClickListener(v -> showAccountSelectionDialog());
        btnSave.setOnClickListener(v -> validateAndSave());
        
        btnScan.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
    }

    private void openCamera() {
        try {
            File photoFile = File.createTempFile("receipt_", ".jpg", requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            photoUri = FileProvider.getUriForFile(requireContext(), "com.example.quanlychitieu.fileprovider", photoFile);
            takePhotoLauncher.launch(photoUri);
        } catch (IOException e) {
            Toast.makeText(getContext(), "Lỗi khởi tạo Camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void processReceiptImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            
            Toast.makeText(getContext(), "Đang phân tích hóa đơn...", Toast.LENGTH_SHORT).show();
            
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String resultText = visionText.getText();
                        extractAmountFromText(resultText);
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Không thể nhận diện văn bản", Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extractAmountFromText(String text) {
        // Tìm các chuỗi có định dạng số tiền (VD: 50.000, 1.200.000, 150000)
        Pattern pattern = Pattern.compile("(\\d{1,3}([.,]\\d{3})*|\\d+)");
        Matcher matcher = pattern.matcher(text);
        
        double maxAmount = 0;
        while (matcher.find()) {
            try {
                String clean = matcher.group().replace(".", "").replace(",", "");
                double val = Double.parseDouble(clean);
                // Thường tổng tiền là số lớn nhất trên hóa đơn
                if (val > maxAmount && val < 1000000000) { // Giới hạn 1 tỷ để tránh rác
                    maxAmount = val;
                }
            } catch (Exception ignored) {}
        }

        if (maxAmount > 0) {
            etAmount.setText(String.valueOf((int)maxAmount));
            Toast.makeText(getContext(), "Đã tìm thấy số tiền: " + formatCurrency(maxAmount), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Không tìm thấy số tiền rõ ràng", Toast.LENGTH_SHORT).show();
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
            if (accounts != null) {
                accountList = accounts;
                if (selectedAccountId == -1 && !accounts.isEmpty() && transactionId == -1) {
                    selectedAccountId = accounts.get(0).getId();
                    tvAccountName.setText(accounts.get(0).getName());
                }
            }
        });
        db.categoryDao().getCategoriesByType(userId, "INCOME").observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) incomeCategories = categories;
        });
        db.categoryDao().getCategoriesByType(userId, "EXPENSE").observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) expenseCategories = categories;
        });
    }

    private void loadTransactionData() {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        db.transactionDao().getTransactionById(transactionId)
                .observe(getViewLifecycleOwner(), transaction -> {
                    if (transaction != null) {
                        tvTitle.setText("Sửa giao dịch");
                        etAmount.setText(String.valueOf((int)transaction.getAmount()));
                        etNote.setText(transaction.getNote());
                        selectedType = transaction.getType();
                        selectedCategoryId = transaction.getCategoryID();
                        selectedAccountId = transaction.getAccountId();
                        int checkedId = ("INCOME".equals(selectedType)) ? R.id.btn_type_income : R.id.btn_type_expense;
                        toggleType.check(checkedId);
                        updateToggleColors(checkedId);
                        try {
                            calendar.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(transaction.getDate()));
                            updateDateLabel();
                        } catch (Exception e) {}
                        db.categoryDao().getCategoryById(selectedCategoryId)
                                .observe(getViewLifecycleOwner(), category -> {
                                    if (category != null) {
                                        tvCategoryName.setText(category.getName());
                                        if (category.getColor() != null) {
                                            ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(category.getColor())));
                                        }
                                    }
                                });
                        db.accountDao().getAccountById(selectedAccountId)
                                .observe(getViewLifecycleOwner(), account -> {
                                    if (account != null) tvAccountName.setText(account.getName());
                                });
                    }
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
            if (category.getColor() != null) {
                ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(category.getColor())));
            }
            dialog.dismiss();
        });
        rvCategories.setAdapter(adapter);
        if ("INCOME".equals(selectedType)) {
            toggleFilter.check(R.id.btn_filter_income);
            adapter.setCategories(incomeCategories);
        } else {
            toggleFilter.check(R.id.btn_filter_expense);
            adapter.setCategories(expenseCategories);
        }
        toggleFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_filter_income) adapter.setCategories(incomeCategories);
                else adapter.setCategories(expenseCategories);
            }
        });
        dialogView.findViewById(R.id.btn_add_custom_category).setOnClickListener(v -> {
            dialog.dismiss();
            showAddCategoryDialog();
        });
        dialog.show();
    }

    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        EditText etName = dialogView.findViewById(R.id.et_category_name);
        RadioGroup rgType = dialogView.findViewById(R.id.rg_category_type);
        ImageView ivIconPreview = dialogView.findViewById(R.id.iv_selected_icon);
        View btnPickIcon = dialogView.findViewById(R.id.btn_pick_icon);
        currentPickingIcon = "outline_article_person_24";
        currentPickingColor = "#3B82F6";
        ivIconPreview.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(currentPickingColor)));
        btnPickIcon.setOnClickListener(v -> showIconPickerDialog(ivIconPreview));
        new AlertDialog.Builder(requireContext())
                .setTitle("Thêm danh mục mới")
                .setView(dialogView)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) return;
                    String type = (rgType.getCheckedRadioButtonId() == R.id.rb_type_income) ? "INCOME" : "EXPENSE";
                    Category category = new Category();
                    category.setName(name);
                    category.setType(type);
                    category.setIcon(currentPickingIcon);
                    category.setColor(currentPickingColor);
                    category.setUserId(FirebaseAuth.getInstance().getUid());
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase.getDatabase(requireContext()).categoryDao().insert(category);
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showIconPickerDialog(ImageView preview) {
        String[] icons = {"baseline_coffee_24", "outline_directions_car_24", "outline_account_balance_24", "outline_add_shopping_cart_24", "outline_bolt_24", "outline_article_person_24", "outline_calendar_month_24", "outline_account_balance_wallet_24", "baseline_home_24", "baseline_history_24"};
        String[] colors = {"#FF9800", "#2196F3", "#9C27B0", "#E91E63", "#4CAF50", "#00BCD4", "#F44336", "#607D8B", "#FF5722", "#795548"};
        View pickerView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_icon_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(pickerView).create();
        RecyclerView rvIcons = pickerView.findViewById(R.id.rv_icons);
        rvIcons.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        IconAdapter iconAdapter = new IconAdapter(Arrays.asList(icons), Arrays.asList(colors), (icon, color) -> {
            currentPickingIcon = icon;
            currentPickingColor = color;
            int resId = getResources().getIdentifier(icon, "drawable", requireContext().getPackageName());
            if (resId != 0) preview.setImageResource(resId);
            preview.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(color)));
            dialog.dismiss();
        });
        rvIcons.setAdapter(iconAdapter);
        dialog.show();
    }

    private void updateDateLabel() {
        tvDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));
    }

    private void showAccountSelectionDialog() {
        if (accountList.isEmpty()) return;
        String[] names = new String[accountList.size()];
        for (int i = 0; i < accountList.size(); i++) names[i] = accountList.get(i).getName();
        new AlertDialog.Builder(requireContext()).setTitle("Chọn tài khoản").setItems(names, (dialog, which) -> {
            selectedAccountId = accountList.get(which).getId();
            tvAccountName.setText(accountList.get(which).getName());
        }).show();
    }

    private void validateAndSave() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty() || selectedCategoryId == -1 || selectedAccountId == -1) {
            Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        final double amount = Double.parseDouble(amountStr);
        final String note = etNote.getText().toString();
        final String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
        final String month = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(calendar.getTime());
        final String userId = FirebaseAuth.getInstance().getUid();
        final AppDatabase db = AppDatabase.getDatabase(requireContext());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            Budget budget = db.budgetDao().getBudgetSync(userId, selectedCategoryId, month);
            if (budget != null && "EXPENSE".equals(selectedType)) {
                Double currentSpent = db.transactionDao().getCategoryTotalByMonthExcludeSync(userId, selectedCategoryId, month, transactionId);
                double totalAfter = (currentSpent != null ? currentSpent : 0.0) + amount;

                if (totalAfter > budget.getAmountLimit()) {
                    showBudgetWarning("Vượt quá ngân sách!", 
                        "Khoản chi này (" + formatCurrency(amount) + ") sẽ làm bạn vượt quá ngân sách " + formatCurrency(budget.getAmountLimit()) + " của mục này. Bạn vẫn muốn lưu?", 
                        amount, note, date);
                    return;
                } else if (totalAfter >= budget.getAmountLimit() * 0.8) {
                    showBudgetWarning("Sắp hết ngân sách!", 
                        "Khoản chi này (" + formatCurrency(amount) + ") sẽ khiến bạn tiêu hết hơn 80% ngân sách tháng của mục này. Bạn vẫn muốn tiếp tục?",
                        amount, note, date);
                    return;
                }
            }
            performSave(amount, note, date);
        });
    }

    private void showBudgetWarning(String title, String message, double amount, String note, String date) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("Vẫn lưu", (dialog, which) -> {
                            AppDatabase.databaseWriteExecutor.execute(() -> performSave(amount, note, date));
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        }
    }

    private void performSave(double amount, String note, String date) {
        String userId = FirebaseAuth.getInstance().getUid();
        AppDatabase db = AppDatabase.getDatabase(requireContext());

        Transaction transaction = new Transaction();
        if (transactionId != -1) transaction.setId(transactionId);
        transaction.setAmount(amount);
        transaction.setType(selectedType);
        transaction.setAccountId(selectedAccountId);
        transaction.setCategoryID(selectedCategoryId);
        transaction.setNote(note);
        transaction.setDate(date);
        transaction.setUserId(userId);
        
        if (transactionId == -1) db.transactionDao().insert(transaction);
        else db.transactionDao().update(transaction);
        
        BalanceWidget.updateWidget(requireContext());

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Navigation.findNavController(requireView()).navigateUp());
        }
    }

    private String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }
}
