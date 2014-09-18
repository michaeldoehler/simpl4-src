package org.ms123.launcher;

import org.apache.felix.framework.*;
import java.util.*;
import org.apache.felix.main.Main;
import org.apache.felix.main.AutoProcessor;
import org.apache.felix.framework.FrameworkFactory;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.FrameworkEvent;

/**
 */
public class FelixLauncher {

	private static FelixLauncher felixLauncherInstance = new FelixLauncher();

	private static Framework m_fwk = null;

	public static void main(String[] args) {
		felixLauncherInstance.start();
	}

	public static void windowsService(String args[]) {
		String cmd = "start";
		if (args.length > 0) {
			cmd = args[0];
		}
		info("windowsService:"+cmd+"/"+args.length);
		if ("start".equals(cmd)) {
			felixLauncherInstance.serviceStart(args);
		} else {
			felixLauncherInstance.serviceStop(args);
		}
	}

	public static void serviceStart(String args[]) {
		info("serviceStart called:"+args.length);
		start();
	}

	public static void serviceStop(String args[]) {
		info("serviceStop called:"+args.length);
		terminate();
	}

	private static void start() {
		if (m_fwk == null) {
			info("Starting the Framework");
			startFramework();
		}
	}

	public static void terminate() {
		if (m_fwk != null) {
			info("Stopping the Framework");
			try {
				m_fwk.stop();
				m_fwk.waitForStop(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
			info("framework stopped");
		}
	}

	private static void info(String msg) {
		System.out.println(msg);
	}

	private static void startFramework() {
		Main.loadSystemProperties();
		Map<String, String> configProps = Main.loadConfigProperties();
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
						System.err.println("Error stopping framework: " + ex);
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
			System.err.println("Could not create framework: " + ex);
			ex.printStackTrace();
			System.exit(0);
		}
	}
}
