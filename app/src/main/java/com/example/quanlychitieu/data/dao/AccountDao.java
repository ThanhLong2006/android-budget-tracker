package com.example.quanlychitieu.data.dao;

// Import các thành phần của Room để định nghĩa thao tác với cơ sở dữ liệu
import androidx.lifecycle.LiveData; // Dùng để quan sát dữ liệu tự động cập nhật lên giao diện
import androidx.room.Dao; // Đánh dấu đây là lớp truy cập dữ liệu (Data Access Object)
import androidx.room.Delete; // Định nghĩa thao tác xóa bản ghi
import androidx.room.Insert; // Định nghĩa thao tác thêm bản ghi mới
import androidx.room.OnConflictStrategy; // Quy tắc xử lý khi dữ liệu bị trùng khóa chính
import androidx.room.Query; // Định nghĩa các câu lệnh SQL tùy chỉnh
import androidx.room.Update; // Định nghĩa thao tác cập nhật bản ghi

import com.example.quanlychitieu.data.entities.Account; // Thực thể Tài khoản (Ví)

import java.util.List; // Giao diện danh sách chuẩn

/**
 * AccountDao: Chứa các phương thức tương tác với bảng 'accounts' trong cơ sở dữ liệu Room.
 * Mỗi phương thức ở đây tương ứng với một hành động cụ thể trên các ví tiền của người dùng.
 */
@Dao
public interface AccountDao {
    
    /**
     * insert: Thêm một tài khoản (ví) mới.
     * Chọn OnConflictStrategy.REPLACE để nếu có tài khoản trùng ID thì sẽ cập nhật nội dung mới nhất.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Account account);

    /**
     * update: Cập nhật thông tin của ví hiện tại (như đổi tên hoặc thay đổi số dư).
     */
    @Update
    void update(Account account);

    /**
     * delete: Xóa hoàn toàn một ví khỏi ứng dụng.
     */
    @Delete
    void delete(Account account);

    /**
     * getAllAccounts: Lấy danh sách tất cả các ví của một người dùng cụ thể.
     * Trả về cả các ví hệ thống (userId IS NULL) và ví cá nhân.
     */
    @Query("SELECT * FROM accounts WHERE userId = :userId OR userId IS NULL")
    LiveData<List<Account>> getAllAccounts(String userId);

    /**
     * getAccountById: Tìm thông tin ví dựa trên ID.
     */
    @Query("SELECT * FROM accounts WHERE id = :id")
    LiveData<Account> getAccountById(int id);

    /**
     * getAllAccountsSync: Lấy danh sách ví ở chế độ đồng bộ (không dùng LiveData).
     */
    @Query("SELECT * FROM accounts WHERE userId = :userId")
    List<Account> getAllAccountsSync(String userId);

    /**
     * getAllAccountsSync: Lấy toàn bộ ví trong máy.
     */
    @Query("SELECT * FROM accounts")
    List<Account> getAllAccountsSync();

    /**
     * getAccountBySyncId: Tìm ví dựa trên ID đồng bộ từ Firebase.
     */
    @Query("SELECT * FROM accounts WHERE syncId = :syncId LIMIT 1")
    Account getAccountBySyncId(String syncId);
}
