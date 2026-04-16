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
                .orElseThrow(() -> new RuntimeException("Khong tim thay don vi voi ID: " + unitId));

        String unitCode = unit.getCode();
        if (unitCode == null || unitCode.trim().isBlank()) {
            throw new RuntimeException("Don vi chua co ma code, khong the sinh ma can bo");
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
                        .orElseThrow(() -> new RuntimeException("Khong the tao hoac tim duoc counter", ex));
            }
        }

        long nextNumber = counter.getLastNumber() + 1;
        counter.setLastNumber(nextNumber);
        counterRepository.save(counter);

        return String.format("CB-%s-%d-%04d", unitCode, year, nextNumber);
    }
}
