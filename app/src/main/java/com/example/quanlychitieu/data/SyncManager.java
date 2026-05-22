package com.example.quanlychitieu.data;

// Import các thư viện Android và Firebase cần thiết cho việc đồng bộ dữ liệu
import android.content.Context; // Cung cấp ngữ cảnh ứng dụng để truy cập Room Database
import android.util.Log; // Hỗ trợ ghi nhật ký hệ thống (log) để theo dõi quá trình chạy và gỡ lỗi

import com.example.quanlychitieu.data.database.AppDatabase; // Lớp quản lý cơ sở dữ liệu Room cục bộ
import com.example.quanlychitieu.data.entities.Account; // Thực thể đại diện cho bảng Tài khoản (Ví)
import com.example.quanlychitieu.data.entities.Budget; // Thực thể đại diện cho bảng Ngân sách
import com.example.quanlychitieu.data.entities.Category; // Thực thể đại diện cho bảng Danh mục
import com.example.quanlychitieu.data.entities.Transaction; // Thực thể đại diện cho bảng Giao dịch
import com.google.firebase.auth.FirebaseAuth; // Thư viện xác thực người dùng của Firebase
import com.google.firebase.firestore.DocumentSnapshot; // Đại diện cho dữ liệu của một tài liệu trên Firestore
import com.google.firebase.firestore.FirebaseFirestore; // Thư viện cơ sở dữ liệu NoSQL đám mây của Firebase
import com.google.firebase.firestore.WriteBatch; // Công cụ thực hiện nhiều lệnh ghi cùng lúc (giảm thiểu yêu cầu mạng)

import java.util.List; // Giao diện danh sách chuẩn của Java


 // SyncManager: Lớp chịu trách nhiệm đồng bộ hóa dữ liệu hai chiều giữa Database cục bộ (Room)
 // và Database đám mây (Firestore). Giúp bảo toàn dữ liệu người dùng khi đổi thiết bị.

public class SyncManager {
    private static final String TAG = "SyncManager"; // Thẻ nhận diện lớp này trong hệ thống Log
    private final AppDatabase db; // Đối tượng truy cập cơ sở dữ liệu Room cục bộ
    private final FirebaseFirestore firestore; // Đối tượng tương tác với cơ sở dữ liệu Firestore trên mây
    private final String userId; // ID duy nhất của người dùng hiện tại (lấy từ Firebase Auth)


     // Khởi tạo SyncManager.
     // Chọn truyền Context vào để có thể lấy được instance của AppDatabase.

    public SyncManager(Context context) {
        // Khởi tạo database local thông qua mô hình Singleton (đảm bảo chỉ có một kết nối)
        this.db = AppDatabase.getDatabase(context);
        // Lấy instance duy nhất của Firestore để giao tiếp với server
        this.firestore = FirebaseFirestore.getInstance();
        // Lấy UID của người dùng đang đăng nhập để lọc dữ liệu cá nhân tương ứng
        this.userId = FirebaseAuth.getInstance().getUid();
    }


     // getUserId: Trả về ID của người dùng.
     // Dùng để kiểm tra quyền truy cập ở các thành phần khác nếu cần.

    public String getUserId() { return userId; }


     // syncAll: Hàm tổng hợp để bắt đầu đồng bộ hóa tất cả các loại dữ liệu quan trọng.

    public void syncAll() {
        // Nếu chưa đăng nhập (userId rỗng), thoát ngay để tránh lỗi bảo mật/truy cập
        if (userId == null) return;
        
        // Thực hiện đồng bộ từng bảng theo thứ tự logic (Tài khoản -> Danh mục -> Giao dịch -> Ngân sách)
        syncAccounts(); 
        syncCategories();
        syncTransactions();
        syncBudgets();
    }


     // syncAccounts: Đồng bộ dữ liệu các Ví tiền/Tài khoản ngân hàng.

    private void syncAccounts() {
        // Sử dụng luồng nền (Write Executor) để không gây treo giao diện người dùng (UI)
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // --- BƯỚC 1: Đẩy thay đổi từ máy (Local) lên mây (Cloud) ---
            // Lấy danh sách ví hiện có trong máy của người dùng này
            List<Account> localAccounts = db.accountDao().getAllAccountsSync(userId);
            for (Account account : localAccounts) {
                if (account.getSyncId() == null) {
                    // Nếu chưa có syncId, nghĩa là đây là ví mới tạo ở máy mà chưa có trên mây
                    firestore.collection("users").document(userId)
                            .collection("accounts").add(account) // Thêm mới tài liệu vào Cloud
                            .addOnSuccessListener(docRef -> {
                                // Sau khi thêm thành công, Cloud sẽ trả về một ID tự động
                                account.setSyncId(docRef.getId()); // Gán ID đó vào máy
                                // Cập nhật lại ví trong máy để lưu giữ mối liên kết (syncId)
                                AppDatabase.databaseWriteExecutor.execute(() -> db.accountDao().update(account));
                            });
                } else {
                    // Nếu đã có syncId, nghĩa là ví này đã tồn tại trên mây, chỉ cần cập nhật nội dung mới
                    firestore.collection("users").document(userId)
                            .collection("accounts").document(account.getSyncId()).set(account);
                }
            }
            
            // --- BƯỚC 2: Tải dữ liệu mới từ mây (Cloud) về máy (Local) ---
            firestore.collection("users").document(userId).collection("accounts")
                    .get().addOnSuccessListener(queryDocumentSnapshots -> {
                        // Tiếp tục xử lý trong luồng nền của database local
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                // Chuyển đổi dữ liệu JSON từ Cloud sang đối tượng Java Account
                                Account remote = doc.toObject(Account.class);
                                if (remote != null) {
                                    remote.setSyncId(doc.getId()); // Đảm bảo gán đúng ID từ mây
                                    remote.setUserId(userId); // Gắn đúng UID để không bị lẫn lộn dữ liệu
                                    // Kiểm tra xem ví này đã có ở máy chưa dựa trên SyncId
                                    Account local = db.accountDao().getAccountBySyncId(doc.getId());
                                    if (local == null) {
                                        // Nếu máy chưa có, tiến hành chèn mới (Insert)
                                        db.accountDao().insert(remote);
                                    } else {
                                        // Nếu máy đã có, tiến hành cập nhật (Update) để đồng bộ nội dung mới nhất
                                        remote.setId(local.getId()); // Giữ nguyên ID gốc của SQLite
                                        db.accountDao().update(remote);
                                    }
                                }
                            }
                        });
                    });
        });
    }


      // syncCategories: Đồng bộ hóa các danh mục chi tiêu (Ăn uống, Lương...).
     // Quy trình tương tự như đồng bộ Tài khoản.

    private void syncCategories() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 1. Local to Cloud: Duyệt các danh mục ở máy
            List<Category> localItems = db.categoryDao().getAllCategoriesSync(userId);
            for (Category item : localItems) {
                if (item.getSyncId() == null) {
                    // Thêm mới lên mây nếu chưa từng đồng bộ
                    firestore.collection("users").document(userId).collection("categories").add(item)
                            .addOnSuccessListener(ref -> {
                                item.setSyncId(ref.getId()); // Lưu ID mây vào máy
                                AppDatabase.databaseWriteExecutor.execute(() -> db.categoryDao().update(item));
                            });
                } else {
                    // Cập nhật lên mây nếu đã tồn tại
                    firestore.collection("users").document(userId).collection("categories").document(item.getSyncId()).set(item);
                }
            }

            // 2. Cloud to Local: Tải danh sách danh mục từ mây về
            firestore.collection("users").document(userId).collection("categories")
                    .get().addOnSuccessListener(snapshots -> {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (DocumentSnapshot doc : snapshots) {
                                Category remote = doc.toObject(Category.class);
                                if (remote != null) {
                                    remote.setSyncId(doc.getId());
                                    remote.setUserId(userId);
                                    // Kiểm tra sự tồn tại ở local để quyết định Insert hay Update
                                    Category local = db.categoryDao().getCategoryBySyncId(doc.getId());
                                    if (local == null) db.categoryDao().insert(remote);
                                    else { remote.setId(local.getId()); db.categoryDao().update(remote); }
                                }
                            }
                        });
                    });
        });
    }


     // syncTransactions: Đồng bộ hóa lịch sử Giao dịch (Thu/Chi).
     // Đây là bảng dữ liệu lớn nhất và quan trọng nhất đối với người dùng.

    private void syncTransactions() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 1. Local to Cloud: Đẩy các bản ghi giao dịch mới hoặc thay đổi lên mây
            List<Transaction> localItems = db.transactionDao().getAllTransactionsSync(userId);
            for (Transaction item : localItems) {
                if (item.getSyncId() == null) {
                    firestore.collection("users").document(userId).collection("transactions").add(item)
                            .addOnSuccessListener(ref -> {
                                item.setSyncId(ref.getId());
                                AppDatabase.databaseWriteExecutor.execute(() -> db.transactionDao().update(item));
                            });
                } else {
                    firestore.collection("users").document(userId).collection("transactions").document(item.getSyncId()).set(item);
                }
            }

            // 2. Cloud to Local: Lấy lại toàn bộ lịch sử giao dịch khi người dùng đăng nhập thiết bị mới
            firestore.collection("users").document(userId).collection("transactions")
                    .get().addOnSuccessListener(snapshots -> {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (DocumentSnapshot doc : snapshots) {
                                Transaction remote = doc.toObject(Transaction.class);
                                if (remote != null) {
                                    remote.setSyncId(doc.getId());
                                    remote.setUserId(userId);
                                    Transaction local = db.transactionDao().getTransactionBySyncId(doc.getId());
                                    if (local == null) db.transactionDao().insert(remote);
                                    else { remote.setId(local.getId()); db.transactionDao().update(remote); }
                                }
                            }
                        });
                    });
        });
    }


     // syncBudgets: Đồng bộ hóa các kế hoạch Ngân sách hàng tháng.
    private void syncBudgets() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 1. Local to Cloud
            List<Budget> localItems = db.budgetDao().getAllBudgetsSync(userId);
            for (Budget item : localItems) {
                if (item.getSyncId() == null) {
                    firestore.collection("users").document(userId).collection("budgets").add(item)
                            .addOnSuccessListener(ref -> {
                                item.setSyncId(ref.getId());
                                AppDatabase.databaseWriteExecutor.execute(() -> db.budgetDao().update(item));
                            });
                } else {
                    firestore.collection("users").document(userId).collection("budgets").document(item.getSyncId()).set(item);
                }
            }

            // 2. Cloud to Local
            firestore.collection("users").document(userId).collection("budgets")
                    .get().addOnSuccessListener(snapshots -> {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (DocumentSnapshot doc : snapshots) {
                                Budget remote = doc.toObject(Budget.class);
                                if (remote != null) {
                                    remote.setSyncId(doc.getId());
                                    remote.setUserId(userId);
                                    Budget local = db.budgetDao().getBudgetBySyncId(doc.getId());
                                    if (local == null) db.budgetDao().insert(remote);
                                    else { remote.setId(local.getId()); db.budgetDao().update(remote); }
                                }
                            }
                        });
                    });
        });
    }
}
