package com.gfi.backend.services.interfaces;

/**
 * Service để sinh mã học sinh tự động.
 * 
 * Format: HS-{MA_DON_VI}-{NAM}-{STT}
 * Ví dụ: HS-TH01-2025-0001
 * 
 * Xử lý race condition:
 * - Sử dụng PESSIMISTIC_WRITE lock để khóa dòng counter khi update
 * - Bắt exception khi tạo counter lần đầu để xử lý 2 request cùng lúc
 */
public interface StudentCodeGeneratorService {
    
    /**
     * Sinh mã học sinh cho đơn vị trong năm chỉ định.
     * 
     * Luồng:
     * 1. Kiểm tra unit tồn tại và có code
     * 2. Tìm/tạo counter cho (unitId, year) với lock
     * 3. Tăng last_number lên 1
     * 4. Ghép mã: HS-{UNIT_CODE}-{YEAR}-{LAST_NUMBER:04d}
     * 
     * @param unitId ID của đơn vị
     * @param year Năm sinh mã
     * @return Mã học sinh được sinh
     * @throws RuntimeException nếu không tìm thấy đơn vị hoặc đơn vị chưa có code
     */
    String generateStudentCode(Long unitId, Integer year);
}
