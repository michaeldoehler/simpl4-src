package org.ms123.launcher;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class OsgiStarter implements ServletContextListener {
	public void contextInitialized(ServletContextEvent sce) {
		System.out.println("contextInitialized:" + sce);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			String cp = sce.getServletContext().getRealPath("/WEB-INF");
			FelixServletLauncher.basePath = cp.substring(0, cp.length() - 8);
			FelixServletLauncher.start();
		});
	}

	public void contextDestroyed(ServletContextEvent sce) {
	}
}

