package com.Banking_app.notification.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * The same hexagonal rules the notification module had in the main app, now for the service:
 * adapter ──► application ──► domain. config, security, exception and shared are the service's own plumbing.
 */
@AnalyzeClasses(packages = "com.Banking_app.notification", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    private static final String ROOT = "com.Banking_app.notification..";
    private static final String DOMAIN = "com.Banking_app.notification.domain..";
    private static final String APPLICATION = "com.Banking_app.notification.application..";

    @ArchTest
    static final ArchRule domainUsesNoFrameworks = noClasses()
            .that().resideInAnyPackage(DOMAIN, "com.Banking_app.notification.shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "javax..", "org.hibernate..", "com.fasterxml..", "lombok..")
            .because("the domain (and the shared kernel) is plain Java: no Spring, JPA, Jackson or Lombok");

    @ArchTest
    static final ArchRule domainDependsOnlyOnItself = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat(resideInAPackage(ROOT).and(not(resideInAPackage(DOMAIN))))
            .because("the domain (Notification and its texts) knows nothing about the application or adapters");

    @ArchTest
    static final ArchRule applicationDoesNotUseAdapters = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAPackage("..adapter..")
            .because("the application talks to the outside only through its ports; adapters implement them");

    @ArchTest
    static final ArchRule applicationUsesNoPersistenceWebOrKafka = noClasses()
            .that().resideInAnyPackage(DOMAIN, APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "org.hibernate..", "org.springframework.data..",
                    "org.springframework.web..", "org.springframework.http..",
                    "org.apache.kafka..", "org.springframework.kafka..")
            .because("JPA, HTTP and Kafka belong in adapters (Spring's @Service/@Transactional are allowed)");

    @ArchTest
    static final ArchRule useCasesAreInterfaces = classes()
            .that().resideInAPackage("com.Banking_app.notification.application.port.in.usecase..")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule outboundPortsAreInterfaces = classes()
            .that().resideInAPackage("com.Banking_app.notification.application.port.out..")
            .and().haveSimpleNameEndingWith("Port")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule servicesImplementUseCases = classes()
            .that().resideInAPackage("com.Banking_app.notification.application.service..")
            .and().areAnnotatedWith(Service.class)
            .should().implement(resideInAPackage("..application.port.in.usecase.."));

    @ArchTest
    static final ArchRule controllersLiveInWebAdapters = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..adapter.in.web..");

    @ArchTest
    static final ArchRule entitiesLiveInPersistenceAdapters = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..adapter.out.persistence..");

    @ArchTest
    static final ArchRule repositoriesOnlyUsedByPersistenceAdapters = noClasses()
            .that().resideOutsideOfPackage("..adapter.out.persistence..")
            .should().dependOnClassesThat().areAssignableTo(JpaRepository.class);

    @ArchTest
    static final ArchRule packagesAreFreeOfCycles = slices()
            .matching("com.Banking_app.notification.(*)..")
            .should().beFreeOfCycles();
}
