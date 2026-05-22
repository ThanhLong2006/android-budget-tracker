package com.example.quanlychitieu.data.dao;

// Import các thư viện của Room để định nghĩa các thao tác dữ liệu
import androidx.lifecycle.LiveData; // Dùng để quan sát sự thay đổi dữ liệu tự động cập nhật lên UI
import androidx.room.Dao; // Đánh dấu đây là một Data Access Object
import androidx.room.Delete; // Định nghĩa thao tác xóa dữ liệu
import androidx.room.Insert; // Định nghĩa thao tác thêm mới dữ liệu
import androidx.room.OnConflictStrategy; // Quy định cách xử lý khi dữ liệu bị trùng lặp
import androidx.room.Query; // Định nghĩa các câu lệnh truy vấn SQL tùy chỉnh
import androidx.room.Update; // Định nghĩa thao tác cập nhật dữ liệu

import com.example.quanlychitieu.data.entities.Category; // Thực thể Danh mục

import java.util.List; // Giao diện danh sách chuẩn của Java

/**
 * CategoryDao: Giao diện chứa các phương thức truy vấn cho bảng danh mục (categories).
 * Sử dụng @Dao để Room biết đây là nơi thực hiện các thao tác với cơ sở dữ liệu.
 */
@Dao
public interface CategoryDao {
    
    /**
     * insert: Thêm một danh mục mới vào bảng.
     * Sử dụng OnConflictStrategy.REPLACE để nếu trùng ID thì sẽ ghi đè dữ liệu mới nhất.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Category category);

    /**
     * update: Cập nhật thông tin của một danh mục đã tồn tại.
     * Room sẽ dựa vào khóa chính (primary key) để biết cần cập nhật dòng nào.
     */
    @Update
    void update(Category category);

    /**
     * delete: Xóa một danh mục khỏi cơ sở dữ liệu.
     */
    @Delete
    void delete(Category category);

    /**
     * getCategoriesByType: Lấy danh sách danh mục theo loại (Thu nhập/Chi tiêu) và theo người dùng.
     * ĐÃ SỬA: Thêm điều kiện (userId = :userId OR userId IS NULL) để lấy cả các danh mục mặc định của hệ thống.
     * Chọn giải pháp này để người dùng luôn thấy các mục mặc định như "Ăn uống", "Lương" ngay cả khi chưa tạo mục nào.
     */
    @Query("SELECT * FROM categories WHERE (userId = :userId OR userId IS NULL) AND type = :type")
    LiveData<List<Category>> getCategoriesByType(String userId, String type);

    /**
     * getCategoryById: Lấy thông tin chi tiết của một danh mục dựa trên ID.
     * Dùng khi cần hiển thị tên hoặc icon của danh mục từ một giao dịch.
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    LiveData<Category> getCategoryById(int id);

    /**
     * getAllCategoriesSync: Lấy toàn bộ danh mục của một người dùng ở chế độ đồng bộ.
     * Trả về List (không phải LiveData) vì dùng trong luồng nền của SyncManager để đẩy lên Cloud.
     */
    @Query("SELECT * FROM categories WHERE userId = :userId")
    List<Category> getAllCategoriesSync(String userId);

    /**
     * getCategoryBySyncId: Tìm kiếm danh mục dựa trên ID từ Cloud (Firestore).
     * Dùng trong quá trình đồng bộ để kiểm tra xem danh mục đã tồn tại dưới máy chưa.
     */
    @Query("SELECT * FROM categories WHERE syncId = :syncId LIMIT 1")
    Category getCategoryBySyncId(String syncId);

    /**
     * getSystemCategoriesSync: Lấy toàn bộ danh mục hệ thống (userId IS NULL).
     * Dùng để kiểm tra xem đã nạp dữ liệu mẫu chưa.
     */
    @Query("SELECT * FROM categories WHERE userId IS NULL")
    List<Category> getSystemCategoriesSync();

    /**
     * getAllCategoriesSync: Lấy toàn bộ danh mục không phân biệt người dùng.
     */
    @Query("SELECT * FROM categories")
    List<Category> getAllCategoriesSync();
}
