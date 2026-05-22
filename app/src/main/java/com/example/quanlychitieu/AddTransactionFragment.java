package com.example.quanlychitieu;

// Import các thư viện Android, giao diện Material và Firebase cần thiết
import android.app.AlertDialog; // Thư viện hiển thị hộp thoại lựa chọn
import android.app.DatePickerDialog; // Hộp thoại chọn ngày tháng năm
import android.content.res.ColorStateList; // Quản lý danh sách màu theo trạng thái (như sáng/tối)
import android.graphics.Color; // Thư viện xử lý mã màu sắc (Hex, RGB)
import android.os.Bundle; // Đối tượng đóng gói dữ liệu truyền giữa các Fragment
import android.view.LayoutInflater; // Chuyển đổi file XML layout thành đối tượng View trong Java
import android.view.View; // Lớp cơ sở cho mọi thành phần giao diện
import android.view.ViewGroup; // Lớp chứa các View khác (Container)
import android.widget.Button; // Thành phần nút bấm
import android.widget.EditText; // Ô nhập liệu văn bản/số
import android.widget.ImageView; // Thành phần hiển thị hình ảnh/biểu tượng
import android.widget.RadioGroup; // Nhóm các nút chọn duy nhất (RadioButton)
import android.widget.TextView; // Thành phần hiển thị văn bản tĩnh/động
import android.widget.Toast; // Hiển thị thông báo nhanh (Pop-up)

import androidx.annotation.NonNull; // Ràng buộc tham số không được null
import androidx.annotation.Nullable; // Cho phép tham số có thể null
import androidx.core.content.ContextCompat; // Lấy màu sắc/tài nguyên an toàn từ Context
import androidx.fragment.app.Fragment; // Mảnh giao diện tái sử dụng và quản lý linh hoạt
import androidx.navigation.Navigation; // Thư viện điều hướng chuẩn Android Jetpack
import androidx.recyclerview.widget.GridLayoutManager; // Bố cục lưới cho Icon Picker
import androidx.recyclerview.widget.LinearLayoutManager; // Bố cục hàng dọc cho danh sách chọn
import androidx.recyclerview.widget.RecyclerView; // Hiển thị danh sách dữ liệu lớn hiệu quả

import com.example.quanlychitieu.data.database.AppDatabase; // Khởi tạo Room Database cục bộ
import com.example.quanlychitieu.data.entities.Account; // Thực thể Tài khoản (Ví)
import com.example.quanlychitieu.data.entities.Budget; // Thực thể Ngân sách
import com.example.quanlychitieu.data.entities.Category; // Thực thể Danh mục
import com.example.quanlychitieu.data.entities.Transaction; // Thực thể Giao dịch
import com.google.android.material.button.MaterialButtonToggleGroup; // Nút gạt chọn Thu nhập/Chi tiêu
import com.google.firebase.auth.FirebaseAuth; // Quản lý xác thực người dùng từ Firebase

import java.text.NumberFormat;
import java.text.SimpleDateFormat; // Định dạng hiển thị ngày tháng
import java.util.ArrayList; // Danh sách mảng động
import java.util.Arrays; // Công cụ thao tác với mảng cố định
import java.util.Calendar; // Quản lý thời gian hệ thống
import java.util.List; // Giao diện danh sách chuẩn
import java.util.Locale; // Cài đặt ngôn ngữ hiển thị (VN)

/**
 * AddTransactionFragment: Màn hình thêm mới/chỉnh sửa giao dịch và danh mục.
 */
public class AddTransactionFragment extends Fragment {

    private EditText etAmount, etNote;
    private TextView tvDate, tvCategoryName, tvAccountName, tvTitle;
    private ImageView ivCategoryIcon;
    private MaterialButtonToggleGroup toggleType;
    private Button btnSave;
    private View btnSelectDate, btnSelectCategory, btnSelectAccount, btnClose;

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

    public AddTransactionFragment() {}

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

        // Kiểm tra ngân sách ở luồng nền
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Budget budget = db.budgetDao().getBudgetSync(userId, selectedCategoryId, month);
            if (budget != null && "EXPENSE".equals(selectedType)) {
                // Lấy tổng chi tiêu hiện tại (trừ đi chính giao dịch này nếu đang sửa)
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
            // Nếu không có ngân sách hoặc còn dư nhiều, tiến hành lưu luôn
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
        
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Navigation.findNavController(requireView()).navigateUp());
        }
    }

    private String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }
}
