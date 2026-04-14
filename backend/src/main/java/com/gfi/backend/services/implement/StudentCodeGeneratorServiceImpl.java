package com.gfi.backend.services.implement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.models.entities.StudentCodeCounter;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.repositories.StudentCodeCounterRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.services.interfaces.StudentCodeGeneratorService;

import lombok.RequiredArgsConstructor;

/**
 * Service sinh mã học sinh tự động.
 * 
 * Luồng:
 * 1. Lấy unit_id và year
 * 2. Kiểm tra unit tồn tại và có code
 * 3. Tìm dòng counter theo (unit_id, year) với lock pessimistic
 * 4. Nếu không có thì tạo mới (với xử lý race condition)
 * 5. Tăng last_number lên 1
 * 6. Ghép thành student_code: HS-{UNIT_CODE}-{YEAR}-{LAST_NUMBER:04d}
 * 7. Trả về mã sinh
 * 
 * Sử dụng PESSIMISTIC_WRITE lock và bắt exception để tránh race condition khi nhiều request cùng lúc.
 */
@Service
@RequiredArgsConstructor
public class StudentCodeGeneratorServiceImpl implements StudentCodeGeneratorService {

    private final StudentCodeCounterRepository counterRepository;
    private final UnitRepository unitRepository;

    @Override
    @Transactional
    public String generateStudentCode(Long unitId, Integer year) {
        // Lấy Unit để xác nhận tồn tại và lấy mã đơn vị
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn vị với ID: " + unitId));

        // Kiểm tra unit.code không được null hoặc rỗng
        String unitCode = unit.getCode();
        if (unitCode == null || unitCode.trim().isBlank()) {
            throw new RuntimeException("Đơn vị chưa có mã code, không thể sinh mã học sinh");
        }
        unitCode = unitCode.trim().toUpperCase();

        // Tìm counter với lock pessimistic, nếu không có thì tạo mới
        StudentCodeCounter counter = counterRepository
                .findByUnitIdAndYearForUpdate(unitId, year)
                .orElse(null);

        if (counter == null) {
            try {
                // Thử tạo mới - nếu 2 request cùng lúc, cái thứ 2 sẽ bị lỗi unique constraint
                StudentCodeCounter newCounter = new StudentCodeCounter();
                newCounter.setUnit(unit);
                newCounter.setYear(year);
                newCounter.setLastNumber(0L);
                counter = counterRepository.saveAndFlush(newCounter);
            } catch (Exception ex) {
                // Bị duplicate unique constraint -> tìm lại với lock, cái counter đã được tạo bởi request khác
                counter = counterRepository
                        .findByUnitIdAndYearForUpdate(unitId, year)
                        .orElseThrow(() -> new RuntimeException("Không thể tạo hoặc tìm được counter", ex));
            }
        }

        // Tăng số đếm lên 1
        long nextNumber = counter.getLastNumber() + 1;
        counter.setLastNumber(nextNumber);
        counterRepository.save(counter);

        // Ghép mã học sinh: HS-{UNIT_CODE}-{YEAR}-{NEXT_NUMBER:04d}
        return String.format("HS-%s-%d-%04d", unitCode, year, nextNumber);
    }
}
