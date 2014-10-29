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
package org.ms123.common.domainobjects;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.jdo.JDOEnhancer;
import javax.jdo.JDOHelper;
import org.apache.sling.commons.compiler.ClassLoaderWriter;
import org.apache.sling.commons.compiler.CompilationResult;
import org.apache.sling.commons.compiler.CompilationUnit;
import org.apache.sling.commons.compiler.CompilerMessage;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.commons.compiler.Options;
import org.ms123.common.entity.api.EntityService;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.setting.api.SettingService;
import org.ms123.common.libhelper.FileSystemClassLoader;
import org.ms123.common.libhelper.Inflector;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import javax.jdo.PersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.system.ThreadContext;
import org.ms123.common.permission.api.PermissionService;
import static org.apache.commons.beanutils.PropertyUtils.getProperty;
import static org.apache.commons.beanutils.PropertyUtils.setProperty;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** DomainObjectsServiceImpl implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=domainobjects" })
public class DomainObjectsServiceImpl implements DomainObjectsService, EventHandler {

	private static final Logger m_logger = LoggerFactory.getLogger(DomainObjectsServiceImpl.class);

	protected BundleContext m_bc;

	private EntityService m_entityService;

	private JavaCompiler m_javaCompiler;

	private SourceGenService m_sourceGenService;

	private ClassGenService m_classGenService;

	private PermissionService m_permissionService;

	private NucleusService m_nucleusService;

	private static String SOURCES = "sources";

	private static String ENTITIES = "entities";

	private static String FIELD = "field";

	private static String RELATION = "relation";

	private static String ENTITY = "entity";

	private static String NAMESPACE = "namespace";

	private int INSERT = 0;

	private int UPDATE = 1;

	private int DELETE = 2;

	protected Inflector m_inflector = Inflector.getInstance();

	static final String[] topics = new String[] { "namespace/installed" };

	public DomainObjectsServiceImpl() {
		m_logger.info("DomainObjectsServiceImpl construct");
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		m_bc = bundleContext;
		System.out.println("DomainObjectsServiceImpl.activate.props:" + props);
		try {
			m_logger.info("DomainObjectsServiceImpl.activate -->");
			Bundle b = bundleContext.getBundle();
			Dictionary d = new Hashtable();
			d.put(EventConstants.EVENT_TOPIC, topics);
			b.getBundleContext().registerService(EventHandler.class.getName(), this, d);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleEvent(Event event) {
		System.out.println("DomainObjectsServiceImpl.Event: " + event);
		try {
			String ns = (String) event.getProperty("namespace");
			StoreDesc sdesc = StoreDesc.getNamespaceData(ns);
			m_permissionService.loginInternal(ns);
			ThreadContext.loadThreadContext(ns, "admin");
			createClasses(sdesc);
			System.out.println(">>>> End handleEvent:" + ThreadContext.getThreadContext().get(ThreadContext.SESSION_MANAGER));
			ThreadContext.getThreadContext().finalize(null);
			System.out.println(">>>> End handleEvent");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

	public void update(Map<String, Object> props) {
	}

	protected void deactivate() throws Exception {
		System.out.println("DomainObjectsServiceImpl.deactivate");
	}

	//Old Stuff(SourceGen and Compile)
	public void generateClasses(StoreDesc sdesc) throws Exception {
		generateClasses(sdesc, null);
	}

	public void generateClasses(StoreDesc sdesc, List<Map> entities) throws Exception {
		Map ret = generateSource(sdesc, entities);
		List<String> srcFiles = (List) ret.get(SOURCES);
		ret = compile(sdesc, (List<Map>) ret.get(ENTITIES), (List<String>) ret.get(SOURCES));
		List errors = (List) ret.get("errors");
		if (errors != null && errors.size() > 0) {
			throw new RuntimeException("DomainObjectsServiceImpl.generateClasses:" + errors);
		}
	}

	public Map generateSource(StoreDesc sdesc, List<Map> entities) throws Exception {
		String namespace = sdesc.getNamespace();
		String pack = sdesc.getPack();
		if (entities == null) {
			entities = getEntities(sdesc);
		}
		printList("\n----> generateSource.entities(" + namespace + "," + pack + "):", entities);
		File outDir = new File(sdesc.getBaseDir(), "/src");
		List<String> srcFiles = m_sourceGenService.generate(sdesc, entities, outDir.toString());
		Map retMap = new HashMap();
		retMap.put(SOURCES, srcFiles);
		retMap.put(ENTITIES, entities);
		return retMap;
	}


	public Map compile(StoreDesc sdesc, List<Map> entities, List<String> srcFiles) throws Exception {
		File outDir1 = new File(sdesc.getBaseDir(), "classes");
		if (!outDir1.exists()) {
			outDir1.mkdirs();
		}
		File[] locations = new File[1];
		locations[0] = outDir1;
		FileSystemClassLoader fscl = new FileSystemClassLoader(this.getClass().getClassLoader(), locations);
		JDOEnhancer enhancer = m_nucleusService.getEnhancer(sdesc);
		enhancer.setClassLoader(fscl);
		final Options options = new Options();
		options.put(Options.KEY_SOURCE_VERSION, Options.VERSION_1_6);
		options.put(Options.KEY_TARGET_VERSION, Options.VERSION_1_6);
		options.put(Options.KEY_CLASS_LOADER_WRITER, createClassWriter(outDir1));
		options.put(Options.KEY_CLASS_LOADER, fscl);
		CompilationUnit[] cus = new CompilationUnit[srcFiles.size()];
		int i = 0;
		for (String src : srcFiles) {
			cus[i++] = createCompileUnit(src);
		}
		CompilationResult result = m_javaCompiler.compile(cus, options);
		System.out.println("result:" + result);
		if (result != null && result.getErrors() != null) {
			List<String> errorList = new ArrayList();
			for (CompilerMessage msg : result.getErrors()) {
				String ms = msg.getFile() + ":" + msg.getLine() + "=>" + msg.getMessage();
				errorList.add(ms);
			}
			Map retMap = new HashMap();
			System.out.println("result:" + errorList);
			retMap.put("errors", errorList);
			return retMap;
		} else {
			if (enhancer != null) {
				for (int c = 0; c < cus.length; c++) {
					String className = cus[c].getMainClassName();
					File file = new File(outDir1, className.replace(".", "/"));
					if (jdoStore(entities, className)) {
						System.out.println("DomainObjectsServiceImpl.enhancer.add:" + file);
						enhancer.addClasses(file.toString() + ".class");
					}
				}
				enhancer.enhance();
			}
			try {
				m_nucleusService.close(sdesc);
			} catch (Exception e) {
				System.out.println("CLOSE:" + e);
			}
			return new HashMap();
		}
	}
	//End old stuff

	public void createClasses(StoreDesc sdesc) throws Exception {
		String namespace = sdesc.getNamespace();
		String pack = sdesc.getPack();
		List<Map> entities = getEntities(sdesc);
		File outDir = new File(sdesc.getBaseDir(), "classes");
		ClassLoader clParent = Thread.currentThread().getContextClassLoader();
		List<String> classFiles = null;
		try {
			//BundleDelegatingClassLoader bdc = new BundleDelegatingClassLoader(m_bc.getBundle(), clParent);
			//Thread.currentThread().setContextClassLoader(bdc);
			classFiles = m_classGenService.generate(sdesc, entities, outDir.toString());
		} finally {
			Thread.currentThread().setContextClassLoader(clParent);
		}
		enhance(sdesc, entities, classFiles);
	}

	private Map enhance(StoreDesc sdesc, List<Map> entities, List<String> classNames) throws Exception {
		File outDir1 = new File(sdesc.getBaseDir(), "classes");
		if (!outDir1.exists()) {
			outDir1.mkdirs();
		}
		File[] locations = new File[1];
		locations[0] = outDir1;
System.out.println("Enhancer.enhance:"+sdesc);
		FileSystemClassLoader fscl = new FileSystemClassLoader(this.getClass().getClassLoader(), locations);
		JDOEnhancer enhancer = m_nucleusService.getEnhancer(sdesc);
		enhancer.setClassLoader(fscl);
		if (enhancer != null) {
			for (String className : classNames) {
				File file = new File(outDir1, className.replace(".", "/"));
				if (jdoStore(entities, className)) {
					System.out.println("DomainObjectsServiceImpl.enhancer.add:" + file);
					enhancer.addClasses(file.toString() + ".class");
				}
			}
			enhancer.enhance();
		}
		try {
System.out.println("Enhancer.close:"+sdesc);
			m_nucleusService.close(sdesc);
		} catch (Exception e) {
			System.out.println("Nucleus.close.ex:" + e);
		}
		return new HashMap();
	}

	private List<Map> getEntities(StoreDesc sdesc) throws Exception {
		String pack = sdesc.getPack();
		boolean withInternal = false;
		if (!StoreDesc.isAidPack(pack)) {
			withInternal = true;
		}
		List entities = m_entityService.getEntities(sdesc, false, null);
		//List entTypes = m_entityService.getEntitytypes(sdesc.getStoreId());
		//System.out.println("entities:"+entities);
		//System.out.println("entTypes:"+entTypes);
		return entities;
	}

	private List<Map> prepareEntities(List<Map> entities) {
		List<Map> ents = new ArrayList();
		for (Map ent : entities) {
			String name = (String) ent.get("name");
			String clazz = m_inflector.getClassName(name);
			Map<String, Object> m = new HashMap();
			m.put("name", name);
			m.put("classname", clazz);
			m.put("genDefFields", ent.get("default_fields"));
			m.put("fields", ent.get("fields"));
			m.put(StoreDesc.PACK, ent.get(StoreDesc.PACK));
			ents.add(m);
		}
		return ents;
	}

	private void printList(String header, List<Map> list) {
		System.out.println("----->" + header);
		if (list != null) {
			for (Map m : list) {
				System.out.println("\t" + m);
			}
		}
		System.out.println("--------------------------------------------------------");
	}

	private boolean jdoStore(List<Map> entities, String className) {
		int dot = className.lastIndexOf(".");
		String entName = m_inflector.getEntityName(className.substring(dot + 1));
		for (Map entMap : entities) {
			String mname = (String) entMap.get("name");
			if (entName.equals(mname)) {
				String store = (String) entMap.get(StoreDesc.STORE);
				System.out.println("\tJdoStore:" + entName + "/" + store);
				if (!"jcr".equals(store)) {
					return true;
				}
			}
		}
		return false;
	}

	private CompilationUnit createCompileUnit(final String sourceFile) throws Exception {
		return new CompilationUnit() {

			public String getMainClassName() {
				int slash = sourceFile.lastIndexOf("src/");
				int dot = sourceFile.lastIndexOf(".");
				String mp = sourceFile.substring(slash + 4, dot).replaceAll("/", ".");
				return mp;
			}

			public Reader getSource() throws IOException {
				InputStream in = new FileInputStream(sourceFile);
				return new InputStreamReader(in, "UTF-8");
			}

			public long getLastModified() {
				return 0;
			}
		};
	}

	private ClassLoaderWriter createClassWriter(final File outDir) throws Exception {
		return new ClassLoaderWriter() {

			public boolean delete(String path) {
				return false;
			}

			public InputStream getInputStream(String path) throws IOException {
				System.out.println("getInputStream:" + path);
				return null;
			}

			public long getLastModified(String path) {
				return -1;
			}

			public OutputStream getOutputStream(String path) {
				File file = new File(outDir, path);
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				try {
					return new FileOutputStream(file);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}

			public boolean rename(String oldPath, String newPath) {
				return false;
			}
		};
	}

	private String checkNull(Map m, String key, String msg) {
		if (m.get(key) != null) {
			return (String) m.get(key);
		}
		throw new RuntimeException(msg);
	}

	/*BEGIN JSON-RPC-API*/
	public void generateClasses(
			@PName(StoreDesc.STORE_ID) String storeId) throws RpcException {
		try {
			StoreDesc sdesc = StoreDesc.get(storeId);
			generateClasses(sdesc,null);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DomainObjectsServiceImpl.generateClasses:", e);
		}
	}
	public void generateSource(
			@PName(StoreDesc.STORE_ID) String storeId) throws RpcException {
		try {
			StoreDesc sdesc = StoreDesc.get(storeId);
			generateSource(sdesc,null);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DomainObjectsServiceImpl.generateClasses:", e);
		}
	}
	public void createClasses(
			@PName(StoreDesc.STORE_ID) String storeId) throws RpcException {
		try {
			StoreDesc sdesc = StoreDesc.get(storeId);
			createClasses(sdesc);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DomainObjectsServiceImpl.createClasses:", e);
		}
	}

	/*END JSON-RPC-API*/
	@Reference(dynamic = true)
	public void setEntityService(EntityService paramEntityService) {
		m_entityService = paramEntityService;
		System.out.println("DomainObjectsServiceImpl.setEntityService:" + paramEntityService);
	}

	@Reference(dynamic = true)
	public void setSourceGenService(SourceGenService sgs) {
		m_sourceGenService = sgs;
		System.out.println("DomainObjectsServiceImpl.setSourceGenService:" + sgs);
	}

	@Reference(dynamic = true)
	public void setClassGenService(ClassGenService sgs) {
		m_classGenService = sgs;
		System.out.println("DomainObjectsServiceImpl.setClassGenService:" + sgs);
	}

	@Reference(dynamic = true)
	public void setNucleusService(NucleusService paramNucleusService) {
		this.m_nucleusService = paramNucleusService;
		System.out.println("DomainObjectsServiceImpl.setNucleusService:" + paramNucleusService);
	}

	@Reference(dynamic = true)
	public void setPermissionService(PermissionService shiroService) {
		System.out.println("DomainObjectsServiceImpl:" + shiroService);
		this.m_permissionService = shiroService;
	}

	@Reference(dynamic = true)
	public void setJavaCompiler(JavaCompiler compiler) {
		m_javaCompiler = compiler;
		System.out.println("DomainObjectsServiceImpl.setJavaCompiler:" + compiler);
	}
}
