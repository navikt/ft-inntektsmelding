package no.nav.familie.inntektsmelding.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import no.nav.foreldrepenger.konfig.Environment;
import org.eclipse.jetty.ee10.cdi.CdiDecoratingListener;
import org.eclipse.jetty.ee10.cdi.CdiServletContainerInitializer;
import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jboss.weld.exceptions.IllegalStateException;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.eclipse.jetty.ee10.webapp.MetaInfConfiguration.CONTAINER_JAR_PATTERN;

public class JettyServer {

    private static final Environment ENV = Environment.current();

    private static final String CONTEXT_PATH = ENV.getProperty("context.path","/ftinntektsmelding");

    private static final String JETTY_SCAN_LOCATIONS = "^.*jersey-.*\\.jar$|^.*felles-.*\\.jar$|^.*app.*\\.jar$";

    private static final String JETTY_LOCAL_CLASSES = "^.*/target/classes/|";
    private final Integer serverPort;
    JettyServer(int serverPort) {
        this.serverPort = serverPort;
    }

    public static void main(String[] args) throws Exception {
        jettyServer().bootStrap();
    }

    private static JettyServer jettyServer() {
        return new JettyServer(ENV.getProperty("server.port", Integer.class, 8080));
    }

    private static ContextHandler createContext() throws MalformedURLException {
        var ctx = new WebAppContext(CONTEXT_PATH, null, simpleConstraints(), null, new ErrorPageErrorHandler(), ServletContextHandler.NO_SESSIONS);
        ctx.setParentLoaderPriority(true);

        String baseResource;
        try (var factory = ResourceFactory.closeable()) {
            baseResource = factory.newResource(".").getRealURI().toURL().toExternalForm();
        }
        ctx.setBaseResourceAsString(baseResource);

        ctx.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        ctx.setInitParameter("pathInfoOnly", "true");

        // Scanns the CLASSPATH for classes and jars.
        ctx.setAttribute(CONTAINER_JAR_PATTERN, String.format("%s%s", ENV.isLocal() ? JETTY_LOCAL_CLASSES : "", JETTY_SCAN_LOCATIONS));

        // Enable Weld + CDI
        ctx.setInitParameter(CdiServletContainerInitializer.CDI_INTEGRATION_ATTRIBUTE, CdiDecoratingListener.MODE);
        ctx.addServletContainerInitializer(new CdiServletContainerInitializer());
        ctx.addServletContainerInitializer(new org.jboss.weld.environment.servlet.EnhancedListener());

        ctx.setThrowUnavailableOnStartupException(true);

        return ctx;
    }

    private static void konfigurerSikkerhet() {
        if (ENV.isLocal()) {
            initTrustStore();
        }
    }

    private static void initTrustStore() {
        final var trustStorePathProp = "javax.net.ssl.trustStore";
        final var trustStorePasswordProp = "javax.net.ssl.trustStorePassword";

        var defaultLocation = ENV.getProperty("user.home", ".") + "/.modig/truststore.jks";
        var storePath = ENV.getProperty(trustStorePathProp, defaultLocation);
        var storeFile = new File(storePath);
        if (!storeFile.exists()) {
            throw new IllegalStateException(
                "Finner ikke truststore i " + storePath + "\n\tKonfigurer enten som System property '" + trustStorePathProp
                    + "' eller environment variabel '" + trustStorePathProp.toUpperCase().replace('.', '_') + "'");
        }
        var password = ENV.getProperty(trustStorePasswordProp, "changeit");
        System.setProperty(trustStorePathProp, storeFile.getAbsolutePath());
        System.setProperty(trustStorePasswordProp, password);
    }

    void bootStrap() throws Exception {
        System.setProperty("task.manager.runner.threads", "4");
        konfigurerSikkerhet();
        var dataSource = setupDataSource();
        migrer(dataSource);
        start();
    }

    private static void migrer(DataSource dataSource) {
        var flyway = flywayConfig(dataSource);
        flyway.load().migrate();
    }

    public static FluentConfiguration flywayConfig(DataSource dataSource) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:/db/postgres/defaultDS").baselineOnMigrate(true);
    }

    public static DataSource setupDataSource() throws NamingException {
        var dataSource = dataSource();
        new EnvEntry("jdbc/defaultDS", dataSource);
        return dataSource;
    }

    public static DataSource dataSource() {
        var config = new HikariConfig();
        config.setJdbcUrl(dbUrl());
        config.setUsername(ENV.getRequiredProperty("DB_USERNAME"));
        config.setPassword(ENV.getRequiredProperty("DB_PASSWORD"));
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(6);
        config.setIdleTimeout(10001);
        config.setMaxLifetime(30001);
        config.setInitializationFailTimeout(30000);
        config.setConnectionTestQuery("select 1");
        config.setDriverClassName("org.postgresql.Driver");
        config.setAutoCommit(false);

        // optimaliserer inserts for postgres
        var dsProperties = new Properties();
        dsProperties.setProperty("reWriteBatchedInserts", "true");
        dsProperties.setProperty("logServerErrorDetail", "false"); // skrur av batch exceptions som lekker statements i åpen logg
        config.setDataSourceProperties(dsProperties);

        return new HikariDataSource(config);
    }

    private static String dbUrl() {
        var host = ENV.getRequiredProperty("DB_HOST");
        var port = ENV.getRequiredProperty("DB_PORT");
        var databaseName = ENV.getRequiredProperty("DB_DATABASE");
        return "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
    }

    private void start() throws Exception {
        var server = new Server(getServerPort());
        server.setConnectors(createConnectors(server).toArray(new Connector[]{}));
        server.setHandler(createContext());
        server.start();
        server.join();
    }

    private List<Connector> createConnectors(Server server) {
        List<Connector> connectors = new ArrayList<>();
        var httpConnector = new ServerConnector(server);
        httpConnector.setPort(getServerPort());
        connectors.add(httpConnector);
        return connectors;
    }

    private static ConstraintSecurityHandler simpleConstraints() {
        var handler = new ConstraintSecurityHandler();
        // Slipp gjennom kall fra plattform til JaxRs. Foreløpig kun behov for GET
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, InternalApiConfig.API_URI + "/*"));
        // Slipp gjennom til autentisering i JaxRs / auth-filter
        handler.addConstraintMapping(pathConstraint(Constraint.ALLOWED, ApiConfig.API_URI + "/*"));
        // Alt annet av paths og metoder forbudt - 403
        handler.addConstraintMapping(pathConstraint(Constraint.FORBIDDEN, "/*"));
        return handler;
    }

    private static ConstraintMapping pathConstraint(Constraint constraint, String path) {
        var mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec(path);
        return mapping;
    }

    private Integer getServerPort() {
        return this.serverPort;
    }

}
