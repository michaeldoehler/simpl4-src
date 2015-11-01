package org.ms123.launcher;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.net.ServerSocket;
import java.net.InetAddress;

import org.apache.felix.framework.*;
import java.util.*;
import java.io.File;
import org.apache.felix.main.Main;
import org.apache.felix.main.AutoProcessor;
import org.apache.felix.framework.FrameworkFactory;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.FrameworkEvent;

public class OsgiStarter implements ServletContextListener {
	public static int jettyPort = -1;
	public static String jettyHost = null;
	public static String simpl4BaseUrl = null;
	public static String simpl4Dir;
	private static Framework osgiFramework = null;

	public void contextInitialized(ServletContextEvent sce) {
		if (jettyPort != -1) {
			return;
		}
		InetAddress loopBack = null;
		try {
			loopBack = InetAddress.getLoopbackAddress();
			ServerSocket socket = new ServerSocket(0, 1, loopBack);
			jettyPort = socket.getLocalPort();
			jettyHost = loopBack.getHostAddress();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
			jettyPort = 10000;
			jettyHost = "127.0.0.1";
		}

		simpl4BaseUrl = "http://" + loopBack.getHostAddress() + ":" + jettyPort;
		System.out.println("contextInitialized:" + jettyPort + "|" + loopBack + "|" + simpl4BaseUrl);

		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

			String cp = sce.getServletContext().getRealPath("/WEB-INF");
			simpl4Dir = cp.substring(0, cp.length() - 8);
			start();
		});
	}

	public void contextDestroyed(ServletContextEvent sce) {
		terminate();
		jettyPort = -1;
	}

	private static void start() {
		if (osgiFramework == null) {
			info("Starting the osgi-framework");
			startFramework();
		}
	}

	private static void terminate() {
		if (osgiFramework != null) {
			info("Stopping the osgi-framework");
			try {
				osgiFramework.stop();
				osgiFramework.waitForStop(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
			info("osgi-framework stopped");
		}
	}

	private static void setProperties() {
		String af = new File(".").getAbsolutePath();
		info("currentDir:" + af);
		info("simpl4Dir:" + simpl4Dir);
		setUserDir(simpl4Dir + "/server");
		System.setProperty("activemq.data", simpl4Dir + "/etc/activemq/data");
		System.setProperty("cassandra.boot_without_jna", "true");
		System.setProperty("cassandra.storagedir", simpl4Dir + "/gitrepos/global_data/store/cassandra");
		System.setProperty("disableCheckForReferencesInContentException", "true");
		System.setProperty("drools.dialect.java.compiler", "JANINO");
		System.setProperty("etc.dir", simpl4Dir + "/etc");
		System.setProperty("felix.cm.dir", simpl4Dir + "/etc/config");
		System.setProperty("felix.config.properties", "file:" + simpl4Dir + "/server/felix/config.ini");
		System.setProperty("felix.fileinstall.dir", simpl4Dir + "/gitrepos/.bundles");
		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("git.repos", simpl4Dir + "/gitrepos");
		System.setProperty("groovy.target.indy", "false");
		System.setProperty("jetty.host", jettyHost);
		System.setProperty("jetty.port", String.valueOf(jettyPort));
		System.setProperty("karaf.etc", simpl4Dir + "/etc/activemq/etc");
		System.setProperty("karaf.local.roles", "admin,manager");
		System.setProperty("karaf.shell.init.script", simpl4Dir + "/etc/shell.init.script");
		System.setProperty("karaf.shell.init.script", simpl4Dir + "/etc/shell.init.script");
		System.setProperty("karaf.startLocalConsole", "false");
		System.setProperty("karaf.startRemoteShell", "false");
		System.setProperty("karaf.systemBundlesStartLevel", "0");
		System.setProperty("openfireHome", simpl4Dir + "/etc/openfire");
		System.setProperty("org.ops4j.pax.logging.DefaultServiceLog.level", "ERROR");
		System.setProperty("org.osgi.framework.storage", simpl4Dir + "/server/felix/cache/runner");
		System.setProperty("org.osgi.service.http.port", "7170");
		System.setProperty("server.root", simpl4Dir + "/server");
		System.setProperty("simpl4.dir", simpl4Dir);
		System.setProperty("webconsole.jms.password", "admin");
		System.setProperty("webconsole.jms.url", "tcp://localhost:61616");
		System.setProperty("webconsole.jms.user", "admin");
		System.setProperty("webconsole.jmx.password", "admin");
		System.setProperty("webconsole.jmx.url", "service:jmx:rmi:///jndi/rmi://localhost:1098/jmxrmi");
		System.setProperty("webconsole.jmx.user", "admin");
		System.setProperty("webconsole.type", "properties");
		System.setProperty("workspace", simpl4Dir + "/workspace");

	}

	private static void setUserDir(String directory_name) {
		File directory = new File(directory_name).getAbsoluteFile();
		if (directory.exists()) {
			System.setProperty("user.dir", directory.getAbsolutePath());
		}
	}

	private static void startFramework() {
		setProperties();
		info("getProperties:" + System.getProperties());
		Main.loadSystemProperties();
		Map<String, String> configProps = Main.loadConfigProperties();
		if (configProps == null) {
			info("No " + Main.CONFIG_PROPERTIES_FILE_VALUE + " found.");
			configProps = new HashMap<String, String>();
		}
		Main.copySystemProperties(configProps);
		String enableHook = configProps.get(Main.SHUTDOWN_HOOK_PROP);
		if ((enableHook == null) || !enableHook.equalsIgnoreCase("false")) {
			Runtime.getRuntime().addShutdownHook(new Thread("Felix Shutdown Hook") {

				public void run() {
					try {
						if (osgiFramework != null) {
							osgiFramework.stop();
							osgiFramework.waitForStop(0);
						}
					} catch (Exception ex) {
						System.err.println("Error stopping osgi-framework: " + ex);
					}
				}
			});
		}
		try {
			FrameworkFactory factory = new FrameworkFactory();
			osgiFramework = factory.newFramework(configProps);
			osgiFramework.init();
			AutoProcessor.process(configProps, osgiFramework.getBundleContext());
			FrameworkEvent event;
			do {
				osgiFramework.start();
				event = osgiFramework.waitForStop(0);
			} while (event.getType() == FrameworkEvent.STOPPED_UPDATE);
			osgiFramework = null;
		} catch (Exception ex) {
			info("Could not create osgi-framework: " + ex);
			ex.printStackTrace();
		}
	}

	private static void info(String msg) {
		System.out.println("FelixServletLauncher:" + msg);
	}

}

