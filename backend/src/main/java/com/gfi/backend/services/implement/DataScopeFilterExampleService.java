package com.gfi.backend.services.implement;

/**
 * EXAMPLE: How to filter data by user's data scopes
 * 
 * This example shows how to integrate DataScopeFilterService
 * into existing service methods to automatically filter results
 * based on user's assigned scopes.
 */
public class DataScopeFilterExampleService {
    
    /**
     * EXAMPLE 1: Filter list results by unit scope
     * 
     * When user calls classroom service, automatically filter
     * to only show classrooms from their allowed units
     * 
     * OLD:
     * public List<Classroom> getAllClassrooms() {
     *     return classroomRepository.findAll();
     * }
     * 
     * NEW:
     * public List<Classroom> getAllClassrooms() {
     *     List<Long> allowedUnitIds = dataScopeFilterService.getAllowedScopes("CLASS_MANAGEMENT");
     *     
     *     if (allowedUnitIds.isEmpty()) {
     *         // Empty scope means ALL (no restriction)
     *         return classroomRepository.findAll();
     *     } else {
     *         // Filter by allowed units
     *         return classroomRepository.findByUnitIdIn(allowedUnitIds);
     *     }
     * }
     */
    
    /**
     * EXAMPLE 2: Check single access before return
     * 
     * When user requests specific resource, check if they have access
     * 
     * OLD:
     * public Classroom getClassroomById(Long id) {
     *     return classroomRepository.findById(id)
     *         .orElseThrow(() -> new NotFoundException("Classroom not found"));
     * }
     * 
     * NEW:
     * public Classroom getClassroomById(Long id) {
     *     Classroom classroom = classroomRepository.findById(id)
     *         .orElseThrow(() -> new NotFoundException("Classroom not found"));
     *     
     *     // Check if user has access to this classroom's unit
     *     dataScopeFilterService.checkAccess("CLASS_MANAGEMENT", classroom.getUnit().getId());
     *     
     *     return classroom;
     * }
     */
    
    /**
     * EXAMPLE 3: Pre-filter in repository query
     * 
     * Add method to repository to support scope filtering:
     * 
     * interface ClassroomRepository extends JpaRepository<Classroom, Long> {
     *     List<Classroom> findByUnitIdIn(List<Long> unitIds);
     *     Page<Classroom> findByUnitIdIn(List<Long> unitIds, Pageable pageable);
     * }
     */
    
    /**
     * EXAMPLE 4: Filter in controller using @DataScoped
     * 
     * Add annotation to controller method:
     * 
     * @GetMapping("/{unitId}/classrooms")
     * @DataScoped(menuCode = "CLASS_MANAGEMENT", scopeParamName = "unitId")
     * public ResponseEntity<?> getClassroomsByUnit(
     *         @PathVariable Long unitId,
     *         Pageable pageable) {
     *     // Framework automatically checks if user can access unitId
     *     // If not, throws AccessDeniedException
     *     
     *     Page<Classroom> classrooms = classroomService.getByUnit(unitId, pageable);
     *     return ResponseEntity.ok(classrooms);
     * }
     */
}
