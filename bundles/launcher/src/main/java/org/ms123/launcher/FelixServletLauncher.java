package org.ms123.launcher;

import org.apache.felix.framework.*;
import java.util.*;
import java.io.File;
import org.apache.felix.main.Main;
import org.apache.felix.main.AutoProcessor;
import org.apache.felix.framework.FrameworkFactory;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.FrameworkEvent;

/**
 */
public class FelixServletLauncher {

	private static Framework m_fwk = null;
	public static String basePath;

	public static void start() {
		if (m_fwk == null) {
			info("Starting the osgi-framework");
			startFramework();
		}
	}

	public static void terminate() {
		if (m_fwk != null) {
			info("Stopping the osgi-framework");
			try {
				m_fwk.stop();
				m_fwk.waitForStop(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
			info("osgi-framework stopped");
		}
	}

	private static void setProperties() {
		String af = new File(".").getAbsolutePath();
		info("setProperties:" + af);
		String simpl4Dir = basePath;
		String jettyPort = "10000";
		System.setProperty("workspace", simpl4Dir + "/workspace");
		System.setProperty("org.osgi.service.http.port", "7170");
		System.setProperty("disableCheckForReferencesInContentException", "true");
		System.setProperty("git.repos", simpl4Dir + "/gitrepos");
		System.setProperty("server.root", simpl4Dir + "/server");
		System.setProperty("org.osgi.framework.storage", simpl4Dir + "/server/felix/cache/runner");
		//		System.setProperty("felix.cache.rootdir",simpl4Dir+ "/server");
		setCurrentDirectory(simpl4Dir + "/server");
		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("groovy.target.indy", "false");
		System.setProperty("simpl4.dir", simpl4Dir);
		System.setProperty("jetty.port", jettyPort);
		System.setProperty("karaf.startLocalConsole", "false");
		System.setProperty("karaf.systemBundlesStartLevel", "0");
		System.setProperty("karaf.startRemoteShell", "false");
		System.setProperty("felix.cm.dir", simpl4Dir + "/etc/config");
		System.setProperty("felix.fileinstall.dir", simpl4Dir + "/gitrepos/.bundles");
		System.setProperty("org.ops4j.pax.logging.DefaultServiceLog.level", "ERROR");
		System.setProperty("drools.dialect.java.compiler", "JANINO");
		System.setProperty("karaf.shell.init.script", simpl4Dir + "/etc/shell.init.script");
		System.setProperty("felix.config.properties", "file:" + simpl4Dir + "/server/felix/config.ini");

		System.setProperty("karaf.local.roles","admin,manager");
		System.setProperty("openfireHome",simpl4Dir+"/etc/openfire");
		System.setProperty("karaf.etc",simpl4Dir+"/etc/activemq/etc");
		System.setProperty("cassandra.boot_without_jna","true");
		System.setProperty("etc.dir",simpl4Dir+"/etc");
		System.setProperty("webconsole.type","properties");
		System.setProperty("webconsole.jms.url","tcp://localhost:61616");
		System.setProperty("webconsole.jmx.url","service:jmx:rmi:///jndi/rmi://localhost:1098/jmxrmi");
		System.setProperty("webconsole.jmx.user","admin");
		System.setProperty("webconsole.jmx.password","admin");
		System.setProperty("webconsole.jms.user","admin");
		System.setProperty("webconsole.jms.password","admin");
		System.setProperty("activemq.data",simpl4Dir+"/etc/activemq/data");
		System.setProperty("karaf.shell.init.script",simpl4Dir+"/etc/shell.init.script");


	}

	public static boolean setCurrentDirectory(String directory_name) {
		boolean result = false; // Boolean indicating whether directory was set
		File directory; // Desired current working directory

		directory = new File(directory_name).getAbsoluteFile();
		info("setCurrentDirectory:" + directory);
		if (directory.exists()) {
			result = (System.setProperty("user.dir", directory.getAbsolutePath()) != null);
			info("setCurrentDirectory2:" + result);
		}

		return result;
	}

	private static void startFramework() {
		setProperties();
		info("getProperties:" + System.getProperties());
		Main.loadSystemProperties();
		Map<String, String> configProps = Main.loadConfigProperties();
		info("configProps:" + configProps);
		if (configProps == null) {
			System.err.println("No " + Main.CONFIG_PROPERTIES_FILE_VALUE + " found.");
			configProps = new HashMap<String, String>();
		}
		Main.copySystemProperties(configProps);
		String enableHook = configProps.get(Main.SHUTDOWN_HOOK_PROP);
		if ((enableHook == null) || !enableHook.equalsIgnoreCase("false")) {
			Runtime.getRuntime().addShutdownHook(new Thread("Felix Shutdown Hook") {

				public void run() {
					try {
						if (m_fwk != null) {
							m_fwk.stop();
							m_fwk.waitForStop(0);
						}
					} catch (Exception ex) {
						System.err.println("Error stopping osgi-framework: " + ex);
					}
				}
			});
		}
		try {
			FrameworkFactory factory = new FrameworkFactory();
			m_fwk = factory.newFramework(configProps);
			m_fwk.init();
			AutoProcessor.process(configProps, m_fwk.getBundleContext());
			FrameworkEvent event;
			do {
				m_fwk.start();
				event = m_fwk.waitForStop(0);
			} while (event.getType() == FrameworkEvent.STOPPED_UPDATE);
			//	System.exit(0);
			m_fwk = null;
		} catch (Exception ex) {
			System.err.println("Could not create osgi-framework: " + ex);
			ex.printStackTrace();
		}
	}

	private static void info(String msg) {
		System.out.println("FelixServletLauncher:" + msg);
	}
}

