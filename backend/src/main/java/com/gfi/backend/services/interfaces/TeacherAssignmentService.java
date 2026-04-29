package com.gfi.backend.services.interfaces;

import com.gfi.backend.models.dtos.staff.TeacherAssignmentItemDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentCreateRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentDetailRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentDetailResponse;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentImportResultDto;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentSearchRequest;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentSearchResponse;
import com.gfi.backend.models.dtos.staff.TeacherAssignmentStaffClassResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TeacherAssignmentService {
    TeacherAssignmentSearchResponse search(TeacherAssignmentSearchRequest request);
    TeacherAssignmentDetailResponse getDetail(TeacherAssignmentDetailRequest request);
    List<TeacherAssignmentStaffClassResponse> getClassesByStaff(Long staffId);
    List<TeacherAssignmentItemDto> create(TeacherAssignmentCreateRequest request);
    byte[] exportExcelTemplate(Long schoolYearId, Long unitId);
    TeacherAssignmentImportResultDto importExcel(Long schoolYearId, Long unitId, MultipartFile file);
    void delete(Long id);
}
