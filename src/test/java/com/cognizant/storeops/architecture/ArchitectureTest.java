package com.cognizant.storeops.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.cognizant.storeops", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest static final ArchRule ACTIVITY_REPOSITORY_BOUNDARY = noClasses()
            .that().resideOutsideOfPackage("..activities..")
            .should().dependOnClassesThat().resideInAPackage("..activities.repository..");
    @ArchTest static final ArchRule PROGRAMME_REPOSITORY_BOUNDARY = noClasses()
            .that().resideOutsideOfPackage("..programmes..")
            .should().dependOnClassesThat().resideInAPackage("..programmes.repository..");
    @ArchTest static final ArchRule STAFF_REPOSITORY_BOUNDARY = noClasses()
            .that().resideOutsideOfPackage("..staff..")
            .should().dependOnClassesThat().resideInAPackage("..staff.repository..");
    @ArchTest static final ArchRule ROUTES_DO_NOT_ACCESS_REPOSITORIES = noClasses()
            .that().resideInAPackage("..routes..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..");
}
