package com.cognizant.storeops.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.cognizant.storeops", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    // Rule 1: module boundary - no module imports another module's repository
    @ArchTest static final ArchRule ACTIVITY_REPOSITORY_BOUNDARY = noClasses()
            .that().resideOutsideOfPackage("..activities..")
            .should().dependOnClassesThat().resideInAPackage("..activities.repository..");
    @ArchTest static final ArchRule PROGRAMME_REPOSITORY_BOUNDARY = noClasses()
            .that().resideOutsideOfPackage("..programmes..")
            .should().dependOnClassesThat().resideInAPackage("..programmes.repository..");
    @ArchTest static final ArchRule STAFF_REPOSITORY_BOUNDARY = noClasses()
            .that().resideOutsideOfPackage("..staff..")
            .should().dependOnClassesThat().resideInAPackage("..staff.repository..");
    @ArchTest static final ArchRule ALERT_REPOSITORY_BOUNDARY = noClasses()
            .that().resideOutsideOfPackage("..alerts..")
            .should().dependOnClassesThat().resideInAPackage("..alerts.repository..");
    @ArchTest static final ArchRule REPORT_REPOSITORY_BOUNDARY = noClasses()
            .that().resideOutsideOfPackage("..reports..")
            .should().dependOnClassesThat().resideInAPackage("..reports.repository..");

    // Rule 2: event bus only - alerts and reports are reached through events, never by service import
    @ArchTest static final ArchRule ALERT_SERVICE_NOT_CALLED_DIRECTLY = noClasses()
            .that().resideOutsideOfPackage("..alerts..")
            .should().dependOnClassesThat().resideInAPackage("..alerts.service..");
    @ArchTest static final ArchRule REPORT_SERVICE_NOT_CALLED_DIRECTLY = noClasses()
            .that().resideOutsideOfPackage("..reports..")
            .should().dependOnClassesThat().resideInAPackage("..reports.service..");

    // Rule 4: layer separation - routes skip nothing, repositories never reach upward
    @ArchTest static final ArchRule ROUTES_DO_NOT_ACCESS_REPOSITORIES = noClasses()
            .that().resideInAPackage("..routes..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..");
    @ArchTest static final ArchRule REPOSITORIES_DO_NOT_CALL_UPWARD = noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..", "..routes..");

    // Rule 5: reports is read-only - it never touches the data stores of the modules it aggregates
    @ArchTest static final ArchRule REPORTS_NEVER_WRITE_OTHER_MODULES = noClasses()
            .that().resideInAPackage("..reports..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..activities.repository..", "..programmes.repository..", "..staff.repository..");
}
