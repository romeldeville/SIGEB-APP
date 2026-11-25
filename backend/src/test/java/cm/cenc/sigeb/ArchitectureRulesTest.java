package cm.cenc.sigeb;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Garde-fous d'architecture du monolithe modulaire SIGEB :
 * empêche toute dérive vers un monolithe « spaghetti » au fil des années.
 */
class ArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void loadClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("cm.cenc.sigeb");
    }

    @Test
    void sharedNeDependDaucunModuleMetier() {
        noClasses().that().resideInAPackage("..shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..auth..", "..referentiel..", "..bapteme..",
                        "..mention..", "..carte..", "..verification..",
                        "..audit..", "..statistique..")
                .check(classes);
    }

    @Test
    void baptemeNeDependPasDeCarteVerificationOuStatistique() {
        noClasses().that().resideInAPackage("..bapteme..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..carte..", "..verification..", "..statistique..")
                .check(classes);
    }

    @Test
    void controllersUniquementDansPackageControllerEtJamaisDependantsDeRepository() {
        classes().that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("..controller..")
                .check(classes);

        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .check(classes);
    }

    @Test
    void entitesJpaUniquementDansPackageEntity() {
        classes().that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage("..entity..")
                .check(classes);
    }
}
