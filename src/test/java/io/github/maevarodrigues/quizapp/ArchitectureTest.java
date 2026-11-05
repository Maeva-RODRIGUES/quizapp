package io.github.maevarodrigues.quizapp;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Règles de l'architecture hexagonale, vérifiées à chaque build.
 * <p>
 * Les dépendances pointent vers le centre : adapters → application → domain.
 * Le domaine et l'application ne connaissent aucun framework.
 */
@AnalyzeClasses(packages = "io.github.maevarodrigues.quizapp", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "..quizapp.domain..";
    private static final String APPLICATION = "..quizapp.application..";

    @ArchTest
    static final ArchRule domainDependsOnlyOnItselfAndTheJdk = classes()
            .that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, "java..")
            .because("le domaine est le cœur métier : il ne doit dépendre d'aucun framework ni d'aucune autre couche");

    @ArchTest
    static final ArchRule applicationDependsOnlyOnDomainAndTheJdk = classes()
            .that().resideInAPackage(APPLICATION)
            .should().onlyDependOnClassesThat().resideInAnyPackage(APPLICATION, DOMAIN, "java..")
            .because("les services applicatifs orchestrent le domaine sans connaître Spring, la base ou HTTP");

    @ArchTest
    static final ArchRule domainDoesNotKnowTheApplicationLayer = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAPackage(APPLICATION)
            .because("les dépendances pointent vers le centre, jamais vers l'extérieur");

    @ArchTest
    static final ArchRule incomingAdaptersOnlyUseInboundPorts = noClasses()
            .that().resideInAPackage("..quizapp.adapters.in..")
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, "..quizapp.adapters.out..", "..quizapp.domain.port.out..")
            .because("un controller appelle un cas d'usage (port « in ») ; il ne connaît ni son implémentation ni la base");

    @ArchTest
    static final ArchRule portsAreInterfaces = classes()
            .that().resideInAPackage("..quizapp.domain.port..")
            .should().beInterfaces()
            .because("un port est un contrat ; son implémentation vit dans l'application ou dans un adaptateur");
}
