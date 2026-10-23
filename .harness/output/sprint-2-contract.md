# Sprint 2: Staff lookup by store and role
Depends on: none
Files expected: `staff/repository/StaffRepository.java`, `staff/repository/InMemoryStaffRepository.java`, `staff/service/StaffService.java`, `ServiceCoverageTest.java`

## Acceptance criteria
### S2-AC1 Find staff by store and role
GIVEN the seeded staff
WHEN `StaffService.findByStoreAndRole("store-1", STORE_MANAGER)`
THEN the list contains `manager-1` only
Proof: `ServiceCoverageTest#staffFindByStoreAndRole`

### S2-AC2 Seeded Department Lead
GIVEN the default `InMemoryStaffRepository`
WHEN `findById("lead-1")`
THEN a User with storeId "store-1" and role DEPARTMENT_LEAD is returned; existing seeds are unchanged
Proof: `ServiceCoverageTest#staffSeedContainsDepartmentLead`

### S2-AC3 No match yields empty list
GIVEN no user with the role in the store
WHEN `findByStoreAndRole("store-2", STORE_MANAGER)`
THEN an empty list, no exception
Proof: `ServiceCoverageTest#staffFindByStoreAndRoleEmpty`

### S2-AC4 Boundaries intact
GIVEN the code base
WHEN `ArchitectureTest` runs
THEN no class outside `staff` depends on `staff.repository`
Proof: `ArchitectureTest`

## Out of scope
New endpoints, alerts, user creation.
