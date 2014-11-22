/*
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
package org.ms123.common.camel;

import flexjson.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.Collections;
import java.security.MessageDigest;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.git.GitService;
import org.ms123.common.git.FileHolderApi;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.system.LogService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.datamapper.DatamapperService;
import org.ms123.common.utils.Inflector;
import org.ms123.common.utils.Utils;

import org.osgi.framework.BundleContext;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import javax.jdo.PersistenceManager;
import javax.jdo.Extent;
import javax.jdo.Query;
import javax.transaction.UserTransaction;
import org.apache.camel.util.IntrospectionSupport;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.impl.CompositeRegistry;
import org.apache.camel.impl.PropertyPlaceholderDelegateRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Producer;
import org.apache.camel.Endpoint;
import org.apache.camel.Route;
import org.apache.camel.Exchange;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.CamelContextNameStrategy;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.core.osgi.OsgiServiceRegistry;
import org.apache.camel.processor.interceptor.Tracer;
import org.apache.camel.util.IntrospectionSupport;
import org.apache.camel.MessageHistory;
import org.apache.camel.FailedToCreateRouteException;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ModelCamelContext;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.tools.FileSystemCompiler;
import org.codehaus.groovy.control.CompilerConfiguration;
import java.io.File;
import  org.ms123.common.camel.components.*;
import  org.ms123.common.camel.trace.*;
import  org.ms123.common.camel.view.VisGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ms123.common.camel.jsonconverter.CamelRouteJsonConverter;
import static org.ms123.common.permission.api.PermissionService.PERMISSION_SERVICE;
import static org.ms123.common.system.LogService.LOG_MSG;
import static org.ms123.common.system.LogService.LOG_KEY;
import static org.ms123.common.system.LogService.LOG_TYPE;
import static org.ms123.common.system.LogService.LOG_HINT;
import static org.ms123.common.system.LogService.LOG_TIME;
/**
 *
 */
@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
abstract class BaseCamelServiceImpl implements Constants,org.ms123.common.camel.api.CamelService {
	private static final Logger m_logger = LoggerFactory.getLogger(BaseCamelServiceImpl.class);

	protected Inflector m_inflector = Inflector.getInstance();

	protected DataLayer m_dataLayer;

	protected AuthService m_authService;
	protected LogService m_logService;

	protected GitService m_gitService;

	protected PermissionService m_permissionService;

	protected DatamapperService m_datamapperService;

	protected UtilsService m_utilsService;
	protected EventAdmin m_eventAdmin;


	protected BundleContext m_bundleContext;
	protected JSONDeserializer m_ds = new JSONDeserializer();
	protected JSONSerializer m_js = new JSONSerializer();

	private Map<String, ContextCacheEntry> m_contextCache = new LinkedHashMap();

	public CamelContext getCamelContext(String namespace, String camelName) {
		return m_contextCache.get(getContextKey(namespace,  camelName)).context;
	}

	protected List<Map> _getRouteInstances(String contextKey, String routeId, java.lang.Long _startTime, java.lang.Long endTime){
		List<Map> retList = new ArrayList();
		List<Map> logEntries = m_logService.getLog(contextKey+"/"+routeId,"camel/trace", LOG_KEY+","+LOG_TIME+","+LOG_HINT, _startTime, endTime);
		String currentKey = null;
		Date startTime = null;
		Date prevTime = null;
		boolean hasError=false;
		for( Map entry : logEntries){
			String key = entry.get(LOG_KEY);
			if("error".equals(entry.get(LOG_HINT))){
				hasError= true;
			}
			if( !key.equals(currentKey)){
				if( startTime != null){
					retList.add(createRecord(startTime, prevTime,currentKey,hasError));
					hasError = false;
				}
				startTime = (Date)entry.get(LOG_TIME);
				currentKey = key;
			}
			prevTime = (Date)entry.get(LOG_TIME);
		}
		if( startTime != null){
			retList.add(createRecord(startTime, prevTime,currentKey,hasError));
		}
		sortListByStartTime(retList);
		return retList;
	}

	protected List<Map> _getRouteInstance(String contextKey, String routeId, String exchangeId){
		List<Map> logEntries = m_logService.getLog(contextKey+"/"+routeId+"/"+exchangeId,"camel/trace" );
		return logEntries;
	}

	private Map createRecord(Date startTime, Date endTime, String key,boolean hasError){
		Map retMap = new HashMap();
		retMap.put(STARTTIME, startTime);
		retMap.put(ENDTIME, endTime);
		retMap.put(STATUS, hasError ? "error" : "ok" );
		int lastSlash = key.lastIndexOf("/");
		retMap.put("exchangeId", key.substring(lastSlash+1));
		return retMap;
	}

	protected List<Map> _getRouteInfoList(String contextKey){
		List<Map> resultList = new ArrayList();
		ContextCacheEntry cce  = m_contextCache.get(contextKey);
		if( cce == null){
			//throw new RuntimeException("_getRouteDefinitions:context:"+contextKey+" not found");
			return resultList;
		}
		CamelContext cc = cce.context;
		info("Def:"+cc.getRouteDefinitions());
		List<RouteDefinition> rdList =  cc.getRouteDefinitions();
		for( RouteDefinition rd : rdList){
			System.out.println("I:"+rd.getInputs());
			System.out.println("O:"+rd.getOutputs());
			Map routeMap = new HashMap();
			routeMap.put("id", rd.getId());
			routeMap.put("route", rd.toString());
			resultList.add(routeMap);
		}
		return resultList;
	}
	protected List<String> _getContextNames(String namespace){
		List<String> retList = new ArrayList();
		for( String key : m_contextCache.keySet()){
			String[] a = key.split("/");
			String ns = a[0];
			String name = a[1];
			if( namespace != null){
				if( !ns.equals(namespace)){
					continue;
				}
			}
			retList.add(key);
		}
		return retList;
	}
	protected Map<String,List> _getRouteVisGraph(String contextKey, String routeId){
		ContextCacheEntry cce  = m_contextCache.get(contextKey);
		if( cce == null){
			throw new RuntimeException("_getVisGraph:context:"+contextKey+" not found");
		}
		CamelContext cc = cce.context;
		RouteDefinition routeDefinition =  cc.getRouteDefinition(routeId);
		VisGenerator vg = new VisGenerator();
		return vg.getGraph(routeDefinition);
	}
	protected synchronized void _createRoutesFromJsonDescription(){
		List<Map> repos = m_gitService.getRepositories(new ArrayList(),false);
		for(Map<String,String> repo : repos){
			String namespace = repo.get("name");
			_createRoutesFromJsonDescription(namespace);
		}
	}
	private RouteDefinition createRouteDefinitionFromJson(String path, ModelCamelContext context, Map rootShape) {
		def c = new CamelRouteJsonConverter(path, context, rootShape);
		return c.getRouteDefinition();
	}
	protected synchronized void _createRoutesFromJsonDescription(String namespace){
		_createRoutesFromJsonDescription(namespace,null);
	}
	protected synchronized void _createRoutesFromJsonDescription(String namespace,String path){
		Map<String, List> routeJsonDescriptionMap = getRouteJsonDescriptionMap(namespace);
		if( routeJsonDescriptionMap.size() == 0){
			stopNotActiveRoutes( getContextKey(namespace,"default"), []);
		}
		for( String  contextKey : routeJsonDescriptionMap.keySet()){
			List<Map> list = routeJsonDescriptionMap.get(contextKey);
			ContextCacheEntry cce = m_contextCache.get(contextKey);
			if( cce == null){
				if(list.size() == 0) {
					//No enabled route
					stopNotActiveRoutes(contextKey,[]);
					continue;
				}
				cce = new ContextCacheEntry();
				cce.groovyRegistry = new GroovyRegistry( BaseCamelServiceImpl.class.getClassLoader(), namespace);
				cce.context = CamelContextBuilder.createCamelContext(namespace,cce.groovyRegistry, m_bundleContext,true);
				cce.context.setNameStrategy( new FixedCamelContextNameStrategy(contextKey));
				cce.context.start();
				m_contextCache.put(contextKey, cce);
			}
			List<String> okList = [];
			for( Map map : list){
				Map routeJsonDescription = (Map)map.routeJsonDescription;
				String md5 = (String)map.md5;
				String _path = (String)map.path;
				String routeId = getId(routeJsonDescription);
				info("path:"+path+"/_path:"+_path);
				if( path != null && _path != path ){
					okList.add( routeId);
					continue;
				}
				info("routeId:"+routeId);
				boolean autoStart = getBoolean(routeJsonDescription, AUTOSTART, false);
				RouteCacheEntry re = cce.routeEntryMap[routeId];
				if( re == null){
					//new Route
					re = new RouteCacheEntry( md5:md5,routeId:routeId);
					info("Add route:"+routeId);
					RouteDefinition rd = createRouteDefinitionFromJson( _path, cce.context, routeJsonDescription);
					rd.routeId(routeId);
					addRouteDefinition(cce.context, rd,re);
					cce.routeEntryMap[routeId] = re;
					cce.context.startRoute(routeId);
					okList.add( routeId);
				}else{
					if( re.lastError == null  && re.md5 == md5 ){
						//Nothing changed.
						okList.add( routeId);
						continue;
					}else{
						//exchange route
						info("Exchange route:"+routeId+"/"+autoStart);
						re.md5 = md5;
						RouteDefinition rd = createRouteDefinitionFromJson( _path, cce.context, routeJsonDescription);
						rd.routeId(routeId);
						okList.add( routeId);
						cce.context.stopRoute(routeId);
						cce.context.removeRoute(routeId);
						addRouteDefinition(cce.context,rd,re);
						if( autoStart){
							cce.context.startRoute(routeId);
						}
					}
				}
			}
			stopNotActiveRoutes(contextKey,okList);
		}
	}

	private void stopNotActiveRoutes(String contextKey, List okList){
		info("stopNotActiveRoutes:"+contextKey+"/"+okList);
		ContextCacheEntry cce = m_contextCache.get(contextKey);
		if( cce == null) return;
		List<String> ridList = [];
		cce.context.getRouteDefinitions().each(){RouteDefinition rdef->
			ridList.add(rdef.getId());
		}
		for( String rid in ridList){
			if( !okList.contains(rid)){
				info("Remove route:"+rid);
				cce.context.stopRoute(rid);
				cce.context.removeRoute(rid);
				cce.routeEntryMap.remove(rid);
			}
		}
		info("-->Context("+contextKey+"):status:"+cce.context.getStatus());
		cce.context.getRouteDefinitions().each(){RouteDefinition rdef->
			String rid = rdef.getId();
			info("\tRoute("+rid+"):status:"+cce.context.getRouteStatus(rid));
		}
	}

	private void addRouteDefinition(CamelContext context, RouteDefinition rd, RouteCacheEntry re) throws Exception{
		try{
			context.addRouteDefinition(rd );
			re.lastError = null;
		}catch(Exception e){
			e.printStackTrace();
			context.removeRouteDefinition(rd);
			re.lastError = e.getMessage();
			if( e instanceof FailedToCreateRouteException){
				String msg = e.getMessage();
				println("msg:"+msg);
				int ind;
				if( (ind=msg.indexOf("<<<")) != -1){
					msg = msg.substring(ind+4);
				}
				throw new RuntimeException("<br/>"+msg);
			}
			throw e;
		}
	}
	private Map<String,List> getRouteJsonDescriptionMap(String namespace){
		List<String> types = new ArrayList();
		types.add(CAMEL_TYPE);
		types.add(DIRECTORY_TYPE);
		List<String> typesCamel = new ArrayList();
		typesCamel.add(CAMEL_TYPE);

		Map map= m_gitService.getWorkingTree(namespace, null, 100, types, null, null,null);
		List<Map> pathList = new ArrayList();
		toFlatList(map,typesCamel,pathList);

		Map<String,List> routeJsonDescriptionMap = [:];
		for( Map pathMap : pathList){
			String path = (String)pathMap.get(PATH);
			String  routeString = m_gitService.getContent(namespace, path);
			String md5 = getMD5OfUTF8(routeString);
			Map routeJsonDescription=null;
			try{
				routeJsonDescription = (Map)m_ds.deserialize(routeString);
			}catch(Exception e){
				info("Cannot deserialize:"+path);
				continue;
			}
			String contextName = getString(routeJsonDescription, CAMELCONTEXT, "default");
			String contextKey = getContextKey(namespace, contextName);
			boolean enabled = getBoolean(routeJsonDescription, ENABLED, true);
			if( !enabled) continue;
			if( routeJsonDescriptionMap[contextKey] == null){
				routeJsonDescriptionMap[contextKey] = [];
			}
			routeJsonDescriptionMap[contextKey].add([md5:md5, path:path, routeJsonDescription: routeJsonDescription]);
		}
		return routeJsonDescriptionMap;
	}

	protected void	_compileGroovyScripts(String namespace,String path,String code){
		String destDir = System.getProperty("workspace")+"/"+ "groovy"+"/"+namespace;
		String srcDir = System.getProperty("git.repos")+"/"+namespace;
		CompilerConfiguration.DEFAULT.getOptimizationOptions().put("indy", false);
		CompilerConfiguration config = new CompilerConfiguration();
		config.getOptimizationOptions().put("indy", false);

		config.setTargetDirectory( destDir);
		FileSystemCompiler fsc = new FileSystemCompiler(config);

		File[] files = new File[1];
		files[0] = new File(srcDir, path);
		try {
			fsc.compile(files);
		} catch (Throwable e) {
			String msg = Utils.formatGroovyException(e,code);
			throw new RuntimeException(msg);
		}
		newGroovyClassLoader();
	}

	private void newGroovyClassLoader(){
		for( String key : m_contextCache.keySet()){
			GroovyRegistry gr = m_contextCache.get(key).groovyRegistry;
			gr.newClassLoader();
			CamelContext co = m_contextCache.get(key).context;
			co.stop();
			co.start();
		}
	}
	private static void info(String msg) {
		System.err.println(msg);
		m_logger.info(msg);
	}


	private void toFlatList(Map<String,Object> fileMap,List<String> types,List<Map> result){
		String type = (String)fileMap.get("type");
		if( types.indexOf(type) != -1){
			result.add(fileMap);
		}
		List<Map> childList = (List)fileMap.get("children");
		for( Map child : childList){
			toFlatList(child,types,result);
		}
	}
	public void saveHistory(Exchange exchange) {
		String activitikey = (String)exchange.getProperty("activitikey");

		List<MessageHistory> list = exchange.getProperty(Exchange.MESSAGE_HISTORY, List.class);
		ExchangeFormatter formatter = new ExchangeFormatter();
		formatter.setShowExchangeId(true);
		formatter.setMultiline(true);
		formatter.setShowHeaders(true);
		formatter.setStyle(ExchangeFormatter.OutputStyle.Fixed);
		String routeStackTrace = MessageHelper.dumpMessageHistoryStacktrace(exchange, formatter, true);
		boolean hasException = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class) != null;

		Map props = new HashMap();
		props.put(LOG_TYPE, "camel/history");
		String key = activitikey;
		props.put(LOG_KEY, key);
		props.put(LOG_HINT, hasException ? "error": "ok");
		props.put(LOG_MSG, routeStackTrace);
		m_eventAdmin.sendEvent(new Event("log", props));
	}

	public static class SleepBean {
		public void sleep(String body, Exchange exchange) throws Exception {
			info("SleepBean.start");
			Thread.sleep(500);
			info("SleepBean.end");
		}
	}

	protected void printRoutes(CamelContext camelContextObj) {
		for (Endpoint e : camelContextObj.getEndpoints()) {
			info("Endpoint:" + e + "/" + e.getEndpointKey());
		}
	}



	private void sortListByStartTime(List<Map> list) {
		Collections.sort(list, new TComparable());
	}

	private static class TComparable implements Comparator<Map> {
		@Override
		public int compare(Map m1, Map m2) {
			Date l1 = (Date) m1.get(Constants.STARTTIME);
			Date l2 = (Date) m2.get(Constants.STARTTIME);
			return l2.compareTo(l1);
		}
	}

	private static String getMD5OfUTF8(String text) {
		try {
			MessageDigest msgDigest = MessageDigest.getInstance("MD5");
			byte[] mdbytes = msgDigest.digest(text.getBytes("UTF-8"));
			StringBuffer hexString = new StringBuffer();
			for (int i=0;i<mdbytes.length;i++) {
				String hex=Integer.toHexString(0xff & mdbytes[i]);
				if(hex.length()==1) hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception ex) {
			throw new RuntimeException("BaseCamelServiceImpl.getMD5OfUTF8");
		}
	}

	private String getContextKey(String namespace,String name){
		return namespace+"/"+name;
	}

	protected boolean getBoolean(Map shape, String name,boolean _default) {
		Map properties = (Map) shape.get(PROPERTIES);

		Object value  = properties.get(name);
		if( value == null) return _default;
		return (boolean)value;
	}

	protected String getString(Map shape, String name,String _default) {
		Map properties = (Map) shape.get(PROPERTIES);

		Object value  = properties.get(name);
		if( value == null) return _default;
		return (String)value;
	}

	protected String getId(Map shape) {
		Map properties = (Map) shape.get(PROPERTIES);
		String id = ((String) properties.get(OVERRIDEID));
		if( id == null || id.trim().length()==0){
			id = (String)shape.get(RESOURCEID);
		}
		return id;
	}

	private static class RouteCacheEntry {
		String lastError;
		String routeId;
		String md5;
	}

	private static class ContextCacheEntry {
		GroovyRegistry groovyRegistry;
		String key;
		Map<String,RouteCacheEntry> routeEntryMap = [:];
		ModelCamelContext context;
	}

	private static  class FixedCamelContextNameStrategy implements CamelContextNameStrategy {
		private String name;

		public FixedCamelContextNameStrategy(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getNextName() {
			throw new RuntimeException("FixedCamelContextNameStrategy: not allowed");
		}

		@Override
		public boolean isFixedName() {
			return true;
		}
	}



}

