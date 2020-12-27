package cu.sld.ucmgt.directory;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchTest {

    @Test
    void servicesAndRepositoriesShouldNotDependOnWebLayer() {

        JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("cu.sld.ucmgt.directory");

        noClasses()
            .that()
                .resideInAnyPackage("cu.sld.ucmgt.directory.service..")
            .or()
                .resideInAnyPackage("cu.sld.ucmgt.directory.repository..")
            .should().dependOnClassesThat()
                .resideInAnyPackage("..cu.sld.ucmgt.directory.web..")
        .because("Services and repositories should not depend on web layer")
        .check(importedClasses);
    }
}
