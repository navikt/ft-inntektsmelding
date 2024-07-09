package no.nav.familie.inntektsmelding.server;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class ServiceStarterListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        CDI.current().select(ApplicationServiceStarter.class).get().startServices();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        CDI.current().select(ApplicationServiceStarter.class).get().stopServices();
    }
}
