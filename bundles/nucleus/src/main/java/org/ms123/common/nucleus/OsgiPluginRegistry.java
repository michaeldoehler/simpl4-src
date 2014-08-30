/**
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ms123.common.nucleus;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import javax.xml.parsers.DocumentBuilder;
import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.Localiser;
import org.datanucleus.plugin.*;
import org.osgi.framework.BundleContext;
import org.ops4j.pax.swissbox.extender.*;

/**
 */
@SuppressWarnings("unchecked")
public class OsgiPluginRegistry implements PluginRegistry {

	protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation", ClassConstants.NUCLEUS_CONTEXT_LOADER);

	private static final String DATANUCLEUS_PKG = "org.datanucleus";

	private static final String PLUGIN_DIR = "/";

	private static final FilenameFilter MANIFEST_FILE_FILTER = new FilenameFilter() {

		public boolean accept(File dir, String name) {
			// accept a directory named "meta-inf"
			if (name.equalsIgnoreCase("meta-inf")) {
				return true;
			}
			// or accept /meta-inf/manifest.mf
			if (!dir.getName().equalsIgnoreCase("meta-inf")) {
				return false;
			}
			return name.equalsIgnoreCase("manifest.mf");
		}
	};

	/**

	 * Character that is used in URLs of jars to separate the file name from the path of a resource inside

	 * the jar.<br/> example: jar:file:foo.jar!/META-INF/manifest.mf

	 */
	private static final char JAR_SEPARATOR = '!';

	private final ClassLoaderResolver clr;

	Map<String, ExtensionPoint> extensionPointsByUniqueId = new HashMap();

	Map<String, Bundle> registeredPluginByPluginId = new HashMap();

	ExtensionPoint[] extensionPoints;

	private String bundleCheckType = "EXCEPTION";

	private boolean allowUserBundles = false;

	private static List<org.osgi.framework.Bundle> m_registeredBundles = new ArrayList<org.osgi.framework.Bundle>();

	private static BundleContext m_bundleContext;

	public static void setBundleContext(BundleContext bl) {
		m_bundleContext = bl;
		new BundleWatcher<URL>(m_bundleContext, new BundleURLScanner("/", "plugin.xml", false), new BundleObserver<URL>() {

			public void addingEntries(org.osgi.framework.Bundle bundle, List<URL> entries) {
				m_registeredBundles.add(bundle);
			}

			public void removingEntries(org.osgi.framework.Bundle bundle, List<URL> entries) {
			}
		}).start();
	}

	public OsgiPluginRegistry(ClassLoaderResolver clr) {
		this(clr, null, true);
	}

	public OsgiPluginRegistry(ClassLoaderResolver clr, String bundleCheckType, boolean allowUserBundles) {
		this.clr = clr;
		extensionPoints = new ExtensionPoint[0];
		this.bundleCheckType = (bundleCheckType != null ? bundleCheckType : "EXCEPTION");
		this.allowUserBundles = allowUserBundles;
	}

	public ExtensionPoint getExtensionPoint(String id) {
		return extensionPointsByUniqueId.get(id);
	}

	public ExtensionPoint[] getExtensionPoints() {
		return extensionPoints;
	}

	public void registerExtensionPoints() {
		registerExtensions();
	}

	public void registerExtensionsForPlugin(URL pluginURL, Bundle bundle) {
		DocumentBuilder docBuilder = PluginParser.getDocumentBuilder();
		List[] elements = PluginParser.parsePluginElements(docBuilder, this, pluginURL, bundle, clr);
		registerExtensionPointsForPluginInternal(elements[0], true);
		// Register extensions
		Iterator<Extension> pluginExtensionIter = elements[1].iterator();
		while (pluginExtensionIter.hasNext()) {
			Extension extension = pluginExtensionIter.next();
			ExtensionPoint exPoint = extensionPointsByUniqueId.get(extension.getExtensionPointId());
			if (exPoint == null) {
				NucleusLogger.GENERAL.warn(LOCALISER.msg("024002", extension.getExtensionPointId(), extension.getPlugin().getSymbolicName(), extension.getPlugin().getManifestLocation().toString()));
			} else {
				extension.setExtensionPoint(exPoint);
				exPoint.addExtension(extension);
			}
		}
	}

	public void registerExtensions() {
		if (extensionPoints.length > 0) {
			return;
		}
		List registeringExtensions = new ArrayList();
		// parse the plugin files
		DocumentBuilder docBuilder = PluginParser.getDocumentBuilder();
		Iterator<org.osgi.framework.Bundle> it = m_registeredBundles.iterator();
		try {
			while (it.hasNext()) {
				org.osgi.framework.Bundle b = it.next();
				URL manifestUrl = b.getEntry("/META-INF/MANIFEST.MF");
				URL pluginUrl = b.getEntry("/plugin.xml");
				Manifest manifest = new Manifest(manifestUrl.openStream());
				if (manifest == null) {
					// No MANIFEST.MF for this plugin.xml so ignore it
					continue;
				}
				Bundle bundle = registerBundle(manifest, pluginUrl);
				if (bundle == null) {
					// No MANIFEST.MF for this plugin.xml so ignore it
					continue;
				}
				List[] elements = PluginParser.parsePluginElements(docBuilder, this, pluginUrl, bundle, clr);
				registerExtensionPointsForPluginInternal(elements[0], false);
				registeringExtensions.addAll(elements[1]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		extensionPoints = extensionPointsByUniqueId.values().toArray(new ExtensionPoint[extensionPointsByUniqueId.values().size()]);
		// Register the extensions now that we have the extension-points all loaded
		for (int i = 0; i < registeringExtensions.size(); i++) {
			Extension extension = (Extension) registeringExtensions.get(i);
			ExtensionPoint exPoint = getExtensionPoint(extension.getExtensionPointId());
			if (exPoint == null) {
				if (extension.getPlugin() != null && extension.getPlugin().getSymbolicName() != null && extension.getPlugin().getSymbolicName().startsWith(DATANUCLEUS_PKG)) {
					NucleusLogger.GENERAL.warn(LOCALISER.msg("024002", extension.getExtensionPointId(), extension.getPlugin().getSymbolicName(), extension.getPlugin().getManifestLocation().toString()));
				}
			} else {
				extension.setExtensionPoint(exPoint);
				exPoint.addExtension(extension);
			}
		}
		if (allowUserBundles) {
			ExtensionSorter sorter = new ExtensionSorter();
			for (int i = 0; i < extensionPoints.length; i++) {
				ExtensionPoint pt = extensionPoints[i];
				pt.sortExtensions(sorter);
			}
		}
	}

	protected static class ExtensionSorter implements Comparator<Extension>, Serializable {

		public int compare(Extension o1, Extension o2) {
			String name1 = o1.getPlugin().getSymbolicName();
			String name2 = o2.getPlugin().getSymbolicName();
			if (name1.startsWith("org.datanucleus") && !name2.startsWith("org.datanucleus")) {
				return -1;
			} else if (!name1.startsWith("org.datanucleus") && name2.startsWith("org.datanucleus")) {
				return 1;
			} else {
				return name1.compareTo(name2);
			}
		}
	}

	protected void registerExtensionPointsForPluginInternal(List extPoints, boolean updateExtensionPointsArray) {
		// Register extension-points
		Iterator<ExtensionPoint> pluginExtPointIter = extPoints.iterator();
		while (pluginExtPointIter.hasNext()) {
			ExtensionPoint exPoint = pluginExtPointIter.next();
			extensionPointsByUniqueId.put(exPoint.getUniqueId(), exPoint);
		}
		if (updateExtensionPointsArray) {
			extensionPoints = extensionPointsByUniqueId.values().toArray(new ExtensionPoint[extensionPointsByUniqueId.values().size()]);
		}
	}

	protected Bundle registerBundle(Manifest mf, URL manifest) {
		Bundle bundle = PluginParser.parseManifest(mf, manifest);
		if (bundle == null || bundle.getSymbolicName() == null) {
			// Didn't parse correctly or the bundle is basically junk, so ignore it
			return null;
		}
		if (!allowUserBundles && !bundle.getSymbolicName().startsWith(DATANUCLEUS_PKG)) {
			NucleusLogger.GENERAL.debug("Ignoring bundle " + bundle.getSymbolicName() + " since not DataNucleus, and only loading DataNucleus bundles");
			return null;
		}
		if (registeredPluginByPluginId.get(bundle.getSymbolicName()) == null) {
			if (NucleusLogger.GENERAL.isDebugEnabled()) {
				NucleusLogger.GENERAL.debug("Registering bundle " + bundle.getSymbolicName() + " version " + bundle.getVersion() + " at URL " + bundle.getManifestLocation() + ".");
			}
			registeredPluginByPluginId.put(bundle.getSymbolicName(), bundle);
		} else {
			Bundle previousBundle = registeredPluginByPluginId.get(bundle.getSymbolicName());
			if (bundle.getSymbolicName().startsWith(DATANUCLEUS_PKG) && !bundle.getManifestLocation().toExternalForm().equals(previousBundle.getManifestLocation().toExternalForm())) {
				String msg = LOCALISER.msg("024009", bundle.getSymbolicName(), bundle.getManifestLocation(), previousBundle.getManifestLocation());
				if (bundleCheckType.equalsIgnoreCase("EXCEPTION")) {
					throw new NucleusException(msg);
				} else if (bundleCheckType.equalsIgnoreCase("LOG")) {
					NucleusLogger.GENERAL.warn(msg);
				} else {
				}
			}
		}
		return bundle;
	}

	public Object createExecutableExtension(ConfigurationElement confElm, String name, Class[] argsClass, Object[] args) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Class cls = clr.classForName(confElm.getAttribute(name), this.getClass().getClassLoader());
		Constructor constructor = cls.getConstructor(argsClass);
		return constructor.newInstance(args);
	}

	public Class loadClass(String pluginId, String className) throws ClassNotFoundException {
		return clr.classForName(className, this.getClass().getClassLoader());
	}

	public URL resolveURLAsFileURL(URL url) throws IOException {
		return url;
	}

	public void resolveConstraints() {
		Iterator<Bundle> it = registeredPluginByPluginId.values().iterator();
		while (it.hasNext()) {
			Bundle bundle = it.next();
			List set = bundle.getRequireBundle();
			Iterator requiredBundles = set.iterator();
			while (requiredBundles.hasNext()) {
				Bundle.BundleDescription bd = (Bundle.BundleDescription) requiredBundles.next();
				String symbolicName = bd.getBundleSymbolicName();
				Bundle requiredBundle = registeredPluginByPluginId.get(symbolicName);
				if (requiredBundle == null) {
					if (bd.getParameter("resolution") != null && bd.getParameter("resolution").equalsIgnoreCase("optional")) {
						NucleusLogger.GENERAL.debug(LOCALISER.msg("024013", bundle.getSymbolicName(), symbolicName));
					} else {
						NucleusLogger.GENERAL.error(LOCALISER.msg("024014", bundle.getSymbolicName(), symbolicName));
					}
				}
				if (bd.getParameter("bundle-version") != null) {
					if (requiredBundle != null && !isVersionInInterval(requiredBundle.getVersion(), bd.getParameter("bundle-version"))) {
						NucleusLogger.GENERAL.error(LOCALISER.msg("024015", bundle.getSymbolicName(), symbolicName, bd.getParameter("bundle-version"), bundle.getVersion()));
					}
				}
			}
		}
	}

	private boolean isVersionInInterval(String version, String interval) {
		Bundle.BundleVersionRange versionRange = PluginParser.parseVersionRange(version);
		Bundle.BundleVersionRange intervalRange = PluginParser.parseVersionRange(interval);
		int compare_floor = versionRange.floor.compareTo(intervalRange.floor);
		boolean result = true;
		if (intervalRange.floor_inclusive) {
			result = compare_floor >= 0;
		} else {
			result = compare_floor > 0;
		}
		if (intervalRange.ceiling != null) {
			int compare_ceiling = versionRange.floor.compareTo(intervalRange.ceiling);
			if (intervalRange.ceiling_inclusive) {
				result = compare_ceiling <= 0;
			} else {
				result = compare_ceiling < 0;
			}
		}
		return result;
	}

	public Bundle[] getBundles() {
		return registeredPluginByPluginId.values().toArray(new Bundle[registeredPluginByPluginId.values().size()]);
	}
}
