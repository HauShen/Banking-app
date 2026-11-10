package com.Banking_app.architecture;

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
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * The hexagonal architecture of the account, transaction and fx modules, checked on every build.
 * ArchUnit reads the compiled classes (not the tests) and fails with the exact class and line that breaks a rule.
 *
 * <pre>
 *   adapter ──► application ──► domain          (inside one module, arrows = "may depend on")
 *   account.adapter ──► transaction.application.port.in    (and the other way round)
 *   transaction.adapter ──► fx.application.port.in          (fx calls no other module)
 * </pre>
 *
 * userProfile is not covered yet: its rules need their own decisions (e.g. account's owner lookup adapter reads the
 * userProfile domain type returned by GetUserProfileUseCase).
 */
@AnalyzeClasses(packages = "com.Banking_app", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    private static final String ACCOUNT = "com.Banking_app.account..";
    private static final String TRANSACTION = "com.Banking_app.transaction..";
    private static final String FX = "com.Banking_app.fx..";
    private static final String ACCOUNT_DOMAIN = "com.Banking_app.account.domain..";
    private static final String TRANSACTION_DOMAIN = "com.Banking_app.transaction.domain..";
    private static final String FX_DOMAIN = "com.Banking_app.fx.domain..";
    private static final String ACCOUNT_APPLICATION = "com.Banking_app.account.application..";
    private static final String TRANSACTION_APPLICATION = "com.Banking_app.transaction.application..";
    private static final String FX_APPLICATION = "com.Banking_app.fx.application..";

    // ------------------------------------------------------------------ domain: plain Java

    @ArchTest
    static final ArchRule domainUsesNoFrameworks = noClasses()
            .that().resideInAnyPackage(ACCOUNT_DOMAIN, TRANSACTION_DOMAIN, FX_DOMAIN, "com.Banking_app.shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "javax..", "org.hibernate..", "com.fasterxml..", "lombok..")
            .because("the domain (and the shared kernel) is plain Java: no Spring, JPA, Jackson or Lombok");

    @ArchTest
    static final ArchRule accountDomainDependsOnlyOnItself = noClasses()
            .that().resideInAPackage(ACCOUNT_DOMAIN)
            .should().dependOnClassesThat(resideInAPackage("com.Banking_app..").and(not(resideInAPackage(ACCOUNT_DOMAIN))))
            .because("the account domain knows nothing about the application, adapters or other modules");

    @ArchTest
    static final ArchRule transactionDomainDependsOnlyOnItself = noClasses()
            .that().resideInAPackage(TRANSACTION_DOMAIN)
            .should().dependOnClassesThat(resideInAPackage("com.Banking_app..").and(not(resideInAPackage(TRANSACTION_DOMAIN))))
            .because("the transaction domain knows nothing about the application, adapters or other modules");

    @ArchTest
    static final ArchRule fxDomainDependsOnlyOnItself = noClasses()
            .that().resideInAPackage(FX_DOMAIN)
            .should().dependOnClassesThat(resideInAPackage("com.Banking_app..").and(not(resideInAPackage(FX_DOMAIN))))
            .because("the fx domain (ExchangeRate and its rules) knows nothing about the application, adapters or "
                    + "other modules");

    // ------------------------------------------------------------------ application: ports and use cases

    @ArchTest
    static final ArchRule applicationDoesNotUseAdapters = noClasses()
            .that().resideInAnyPackage(ACCOUNT_APPLICATION, TRANSACTION_APPLICATION, FX_APPLICATION)
            .should().dependOnClassesThat().resideInAPackage("..adapter..")
            .because("the application talks to the outside only through its ports; adapters implement them");

    @ArchTest
    static final ArchRule applicationUsesNoPersistenceOrWeb = noClasses()
            .that().resideInAnyPackage(ACCOUNT_APPLICATION, TRANSACTION_APPLICATION, FX_APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "org.hibernate..", "org.springframework.data..",
                    "org.springframework.web..", "org.springframework.http..")
            .because("JPA, Spring Data and HTTP belong in adapters (Spring's @Service/@Transactional are allowed)");

    @ArchTest
    static final ArchRule useCasesAreInterfaces = classes()
            .that().resideInAnyPackage("com.Banking_app.account.application.port.in.usecase..",
                    "com.Banking_app.transaction.application.port.in.usecase..",
                    "com.Banking_app.fx.application.port.in.usecase..")
            .should().beInterfaces()
            .because("inbound ports are interfaces; services implement them");

    @ArchTest
    static final ArchRule outboundPortsAreInterfaces = classes()
            .that().resideInAnyPackage("com.Banking_app.account.application.port.out..",
                    "com.Banking_app.transaction.application.port.out..",
                    "com.Banking_app.fx.application.port.out..")
            .and().haveSimpleNameEndingWith("Port")
            .should().beInterfaces()
            .because("outbound ports are interfaces; adapters implement them");

    @ArchTest
    static final ArchRule servicesImplementUseCases = classes()
            .that().resideInAnyPackage("com.Banking_app.account.application.service..",
                    "com.Banking_app.transaction.application.service..",
                    "com.Banking_app.fx.application.service..")
            .and().areAnnotatedWith(Service.class)
            .should().implement(resideInAPackage("..application.port.in.usecase.."))
            .because("callers depend on the use-case interface, never on the service class");

    // ------------------------------------------------------------------ adapters

    @ArchTest
    static final ArchRule controllersLiveInWebAdapters = classes()
            .that().resideInAnyPackage(ACCOUNT, TRANSACTION, FX)
            .and().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..adapter.in.web..");

    @ArchTest
    static final ArchRule entitiesLiveInPersistenceAdapters = classes()
            .that().resideInAnyPackage(ACCOUNT, TRANSACTION, FX)
            .and().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..adapter.out.persistence..");

    @ArchTest
    static final ArchRule repositoriesOnlyUsedByPersistenceAdapters = noClasses()
            .that().resideInAnyPackage(ACCOUNT, TRANSACTION, FX)
            .and().resideOutsideOfPackage("..adapter.out.persistence..")
            .should().dependOnClassesThat().areAssignableTo(JpaRepository.class)
            .because("only a module's own persistence adapter touches its tables");

    // ------------------------------------------------------------------ module boundaries

    @ArchTest
    static final ArchRule transactionCoreDoesNotKnowAccount = noClasses()
            .that().resideInAnyPackage(TRANSACTION_DOMAIN, TRANSACTION_APPLICATION)
            .should().dependOnClassesThat().resideInAPackage(ACCOUNT)
            .because("the transaction module reaches accounts only through its own ports (TransferAccountsPort, "
                    + "ReceivingAccountPort, AccountDirectoryPort); only its adapters know the account module");

    @ArchTest
    static final ArchRule transactionCoreDoesNotKnowFx = noClasses()
            .that().resideInAnyPackage(TRANSACTION_DOMAIN, TRANSACTION_APPLICATION)
            .should().dependOnClassesThat().resideInAPackage(FX)
            .because("the transaction module converts currencies only through its own CurrencyConversionPort; "
                    + "only its adapter (FxQuoteConversionAdapter) knows the fx module");

    @ArchTest
    static final ArchRule fxDependsOnNoOtherModule = noClasses()
            .that().resideInAPackage(FX)
            .should().dependOnClassesThat().resideInAnyPackage(ACCOUNT, TRANSACTION)
            .because("fx only provides exchange rates and conversions; it calls no other module");

    @ArchTest
    static final ArchRule otherModulesUseOnlyFxInboundPorts = noClasses()
            .that().resideInAnyPackage(ACCOUNT, TRANSACTION)
            .should().dependOnClassesThat(resideInAPackage(FX).and(not(resideInAnyPackage(
                    "com.Banking_app.fx.application.port.in..",
                    "com.Banking_app.fx.domain.enums.."))))   // ConversionDirection appears in QuoteQuery
            .because("other modules use the fx module's inbound ports, never its services, rates source or domain");

    @ArchTest
    static final ArchRule accountCoreDoesNotKnowTransaction = noClasses()
            .that().resideInAnyPackage(ACCOUNT_DOMAIN, ACCOUNT_APPLICATION)
            .should().dependOnClassesThat().resideInAPackage(TRANSACTION)
            .because("the account module records top-ups and checks history only through its own ports");

    @ArchTest
    static final ArchRule transactionUsesOnlyAccountInboundPorts = noClasses()
            .that().resideInAPackage(TRANSACTION)
            .should().dependOnClassesThat(resideInAPackage(ACCOUNT).and(not(resideInAnyPackage(
                    "com.Banking_app.account.application.port.in..",
                    "com.Banking_app.account.domain.enums.."))))   // enums appear in AccountSnapshot
            .because("other modules use the account module's inbound ports, never its services or repositories");

    @ArchTest
    static final ArchRule accountUsesOnlyTransactionInboundPorts = noClasses()
            .that().resideInAPackage(ACCOUNT)
            .should().dependOnClassesThat(resideInAPackage(TRANSACTION)
                    .and(not(resideInAPackage("com.Banking_app.transaction.application.port.in.."))))
            .because("other modules use the transaction module's inbound ports, never its services or repositories");

    @ArchTest
    static final ArchRule accountLayersAreFreeOfCycles = slices()
            .matching("com.Banking_app.account.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule transactionLayersAreFreeOfCycles = slices()
            .matching("com.Banking_app.transaction.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule fxLayersAreFreeOfCycles = slices()
            .matching("com.Banking_app.fx.(*)..")
            .should().beFreeOfCycles();
}
