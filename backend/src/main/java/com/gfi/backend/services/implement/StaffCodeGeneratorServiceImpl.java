package com.gfi.backend.services.implement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gfi.backend.models.entities.StaffCodeCounter;
import com.gfi.backend.models.entities.Unit;
import com.gfi.backend.repositories.StaffCodeCounterRepository;
import com.gfi.backend.repositories.UnitRepository;
import com.gfi.backend.services.interfaces.StaffCodeGeneratorService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffCodeGeneratorServiceImpl implements StaffCodeGeneratorService {

    private final StaffCodeCounterRepository counterRepository;
    private final UnitRepository unitRepository;

    @Override
    @Transactional
    public String generateStaffCode(Long unitId, Integer year) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn vị với ID: " + unitId));

        String unitCode = unit.getCode();
        if (unitCode == null || unitCode.trim().isBlank()) {
            throw new RuntimeException("Đơn vị chưa có mã code, không thể sinh mã cán bộ");
        }
        unitCode = unitCode.trim().toUpperCase();

        StaffCodeCounter counter = counterRepository.findByUnitIdAndYearForUpdate(unitId, year).orElse(null);

        if (counter == null) {
            try {
                StaffCodeCounter newCounter = new StaffCodeCounter();
                newCounter.setUnit(unit);
                newCounter.setYear(year);
                newCounter.setLastNumber(0L);
                counter = counterRepository.saveAndFlush(newCounter);
            } catch (Exception ex) {
                counter = counterRepository.findByUnitIdAndYearForUpdate(unitId, year)
                        .orElseThrow(() -> new RuntimeException("Không thể tạo hoặc tìm được số thứ tự", ex));
            }
        }

        long nextNumber = counter.getLastNumber() + 1;
        counter.setLastNumber(nextNumber);
        counterRepository.save(counter);

        return String.format("CB-%s-%d-%04d", unitCode, year, nextNumber);
    }
}
