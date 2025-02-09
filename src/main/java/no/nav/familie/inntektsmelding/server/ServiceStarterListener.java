package no.nav.familie.inntektsmelding.server;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import no.nav.k9.prosesstask.impl.TaskManager;

public class ServiceStarterListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        CDI.current().select(ApplicationServiceStarter.class).get().startServices();
        var taskManager = CDI.current().select(TaskManager.class).get();
        taskManager.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        CDI.current().select(ApplicationServiceStarter.class).get().stopServices();
        var taskManager = CDI.current().select(TaskManager.class).get();
        taskManager.stop();
    }
}
