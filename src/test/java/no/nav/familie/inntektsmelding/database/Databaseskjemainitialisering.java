package no.nav.familie.inntektsmelding.database;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Initielt skjemaoppsett + migrering av unittest-skjemaer
 */
public final class Databaseskjemainitialisering {
    private static final AtomicBoolean GUARD_UNIT_TEST_SKJEMAER = new AtomicBoolean();

    public static final String USER = "inntektsmelding";

    private static DataSource DS;

    @SuppressWarnings("resource")
    public static void migrerUnittestSkjemaer(String jdbcUrl) {
        settJdniOppslag(jdbcUrl);

        if (GUARD_UNIT_TEST_SKJEMAER.compareAndSet(false, true)) {
            var location = "/db/postgres/";

            var flyway = Flyway.configure()
                .dataSource(DS)
                .locations(location)
                .baselineOnMigrate(true)
                .load();

            try {
                flyway.migrate();
            } catch (FlywayException fwe) {
                try {
                    // prøver igjen
                    flyway.clean();
                    flyway.migrate();
                } catch (FlywayException fwe2) {
                    throw new IllegalStateException("Migrering feiler", fwe2);
                }
            }
        }
    }

    private static synchronized DataSource settJdniOppslag(String user, String jdbcUrl) {
        var ds = createDs(user, jdbcUrl);
        try {
            new EnvEntry("jdbc/defaultDS", ds); // NOSONAR
            return ds;
        } catch (NamingException e) {
            throw new IllegalStateException("Feil under registrering av JDNI-entry for default datasource", e); // NOSONAR
        }
    }

    private static HikariDataSource createDs(String user, String jdbcUrl) {
        Objects.requireNonNull(user, "user");
        var cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(USER);
        cfg.setPassword(USER);
        cfg.setConnectionTimeout(1500);
        cfg.setValidationTimeout(120L * 1000L);
        cfg.setMaximumPoolSize(4);
        cfg.setAutoCommit(false);

        var ds = new HikariDataSource(cfg);
        Runtime.getRuntime().addShutdownHook(new Thread(ds::close));
        return ds;
    }

    private static void settJdniOppslag(String jdbcUrl) {
        if (DS != null) {
            return;
        }
        DS = settJdniOppslag(USER, jdbcUrl);
    }

}
