package no.nav.familie.inntektsmelding.database;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareExtension;

public class JpaTestcontainerExtension extends EntityManagerAwareExtension {

    private static final String TEST_DB_CONTAINER = Environment.current().getProperty("testcontainer.test.db", String.class, "postgres:15-alpine");

    private static final PostgreSQLContainer TEST_DATABASE = new PostgreSQLContainer<>(DockerImageName.parse(TEST_DB_CONTAINER))
        .withDatabaseName(Databaseskjemainitialisering.USER)
        .withUsername(Databaseskjemainitialisering.USER)
        .withPassword(Databaseskjemainitialisering.USER);

    static {
        TEST_DATABASE.start();
        Databaseskjemainitialisering.migrerUnittestSkjemaer(TEST_DATABASE.getJdbcUrl());
    }

}
