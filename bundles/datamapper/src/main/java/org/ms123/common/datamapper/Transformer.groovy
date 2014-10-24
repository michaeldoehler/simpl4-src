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
package org.ms123.common.datamapper;

import org.milyn.Smooks;
import org.milyn.SmooksException;
import org.milyn.event.report.HtmlReportGenerator;
import org.milyn.payload.JavaResult;
import org.milyn.payload.JavaSource;
import org.milyn.javabean.*;
import org.milyn.io.StreamUtils;
import org.milyn.container.ExecutionContext;
import org.milyn.javabean.lifecycle.*;
import org.milyn.javabean.decoder.*;
import  org.milyn.javabean.decoders.*;
import org.milyn.javabean.context.BeanContext;
import org.xml.sax.SAXException;
import org.milyn.javabean.context.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.milyn.javabean.factory.*;
import org.milyn.csv.CSVRecordParserConfigurator;
import org.milyn.payload.StringSource;
import org.milyn.json.*;
import org.apache.commons.beanutils.*;
import java.io.*;
import java.util.*;
import flexjson.*;
import groovy.lang.*;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.converters.*;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.store.StoreDesc;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.collections.map.MultiValueMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 */
@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
public class Transformer implements Constants{
	private static final Log m_logger = LogFactory.getLog(Transformer.class);
	private static JSONSerializer m_js = new JSONSerializer();
	private  JSONDeserializer m_ds = new JSONDeserializer();
	protected NucleusService m_nucleusService;
	private String m_configName;
	private StoreDesc m_storeDesc;
	private BeanFactory m_beanFactory;

	public Transformer(String namespace,String configName, NucleusService ns){
		this( namespace, configName, ns, null);
	}
	public Transformer(String namespace,String configName, NucleusService ns, BeanFactory bf){
		m_configName = configName == null ? "-" : configName;
		m_nucleusService = ns;
		m_beanFactory = bf;
		m_storeDesc = StoreDesc.getNamespaceData(namespace);
		m_js.prettyPrint(true);
	}

	public Object transform(Map config, Object content) throws Exception {
		Smooks smooks = new Smooks();
		Map input = (Map)config.input;
		Map output = (Map)config.output;
		ClassLoader saveCl = null;
		if( input.format == FORMAT_POJO || output.format == FORMAT_POJO){
			saveCl = _setContextClassLoader();
		}
		try {
			Source source = null;
			if( input.format == FORMAT_CSV){
				configureCSV(config,smooks);
				setPath((Map)config.input,"");
				source = new StringSource(content as String);
			}else if(((Map)config.input).format == FORMAT_JSON){
				configureJSON(config,smooks);
				setPathJSON((Map)config.input,"");
				source = new StringSource( content as String);
			}else if(((Map)config.input).format == FORMAT_XML){
				source = new StringSource(content as String);
				setPath((Map)config.input,"");
			}else if(((Map)config.input).format == FORMAT_MAP){
				if( content instanceof String ){
					content = m_ds.deserialize(content as String);
				}
				source = new JavaSource(content);
				configureMAP(config,smooks);
				setPathMAP((Map)config.input,"");
			}
			if( config.mapping == null || ((List)config.mapping).size() == 0){
				throw new RuntimeException("No mapping");
			}
			Context context = createContext(config,m_configName);
			Bean bean = createBeans(context);
			if( bean == null){
				throw new RuntimeException("No Bean");
			}
			smooks.addVisitor(bean);
			ExecutionContext executionContext = smooks.createExecutionContext();
			//executionContext.setEventListener(new HtmlReportGenerator("/usr/share/nginx/html/report.html"));
			executionContext.setAttribute(TRANSFORMER_CONTEXT, context);
			BeanContext bc = executionContext.getBeanContext();
			bc.addObserver(new Observer());
			JavaResult result = new JavaResult();
			smooks.filterSource(executionContext, source, result);
			return result.getBean(bean.getBeanId());
		} finally {
			smooks.close();
			if( saveCl != null){
				Thread.currentThread().setContextClassLoader(saveCl);
			}
		}
	}
	private void configureJSON(Map config, Smooks smooks){
		Map input = (Map)config.input;
		JSONReaderConfigurator c = new JSONReaderConfigurator();
		if( input.type == NODETYPE_ELEMENT){
			c.setRootName("object");
			config.input[NODENAME] = "object";
		}else{
			config.input[NODENAME] = "array";
			c.setRootName("array");
		}
		smooks.setReaderConfig(c);
	}
	private void configureMAP(Map config, Smooks smooks){
		Map input = (Map)config.input;
		if( input.type == NODETYPE_ELEMENT){
			config.input[NODENAME] = "map";
		}else{
			config.input[NODENAME] = "list";
		}
	}
	private void configureCSV(Map config, Smooks smooks){
		Map input = (Map)config.input;
		String fields = "";
		String komma = "";
		for( Map child in (List<Map>)input.children){
			fields+=komma+child.name;
			komma = ",";
		}
		CSVRecordParserConfigurator c = new CSVRecordParserConfigurator(fields);
		c.setSeparatorChar(((String)input.columnDelim).charAt(0));
		c.setQuoteChar(((String)input.quote).charAt(0));
		config.input[NODENAME] = CSV_RECORD;
		c.setRecordElementName( CSV_RECORD );
		if( input.header){
			c.setSkipLineCount(1);
		}
		smooks.setReaderConfig(c);

		Map csvSet  = [:]
		csvSet[NODENAME] = CSV_ROOT;
		csvSet[NODETYPE] = NODETYPE_COLLECTION;
		csvSet[ROOT] = true;
		csvSet[CHILDREN] = [input];

		input.root=false;
		config.input = csvSet;
	}
	private Context createContext(Map config,String configName){
		def context =  new Context();
		context.mappings = (List<Map>)config.mapping;
		context.inputTree=  (Map)config.input;
		context.outputTree= setPath((Map)config.output,"");
		context.elementIds= [:];
		context.transformer= this;
		context.configName= configName;
		context.beanValues = new HashMap();
		context.scriptCache = new HashMap();
		context.beanUtilsBean = BeanUtilsBean.newInstance();
		debug(m_js.deepSerialize(context.inputTree));
		return context;
	}

	private Bean createBeans(Context context){
		List<Map> mappings = (List)context.mappings;
		updateMappingPathes(mappings, context.outputTree);
		sortMappingByPathLen(mappings);
		debug("Mapping:"+m_js.deepSerialize(mappings));
		MultiValueMap pmap = new MultiValueMap();
		for( Map m in mappings){
			createBeans( m, pmap, context );
		};
		if( context.rootBean != null){
			debug("Result-->:"+context.rootBean.getBeanId());
		}
		return context.rootBean;
	}

	private Map setPathJSON(Map node,String path){
		node.path = path+"/"+node.name;
		if(node.type == NODETYPE_COLLECTION){
			node.path = ((String)node.path) + "/element";
		}
		for( Map child in (List<Map>)node.children){
			setPathJSON(child,((String)node.path));
		}
		return node;
	}
	private Map setPathMAP(Map node,String path){
		node.path = path+"/"+node.name;
		if(node.type == NODETYPE_COLLECTION){
			node.path = ((String)node.path) + "/map";
		}
		for( Map child in (List<Map>)node.children){
			setPathMAP(child,((String)node.path));
		}
		return node;
	}

	private Map setPath(Map node,String path){
		node.path = path+"/"+node.name;
		for( Map child in (List<Map>)node.children){
			setPath(child,((String)node.path));
		}
		return node;
	}

	private void createBeans(Map<String,Map<String,String>> mapping, MultiValueMap pmap, Context context){
		debug("\n\n------------------------pmap:"+pmap);

		Map inputNode = getTreeNodeById( context.inputTree, mapping.input.id);
		String selector = inputNode.path;
		Map outputNode = getTreeNodeById( context.outputTree, mapping.output.id);
		String beanName = outputNode.name;
		String basePath = outputNode.path;
		String lid = beanName+"List_" + mapping.output.id;
		String id = mapping.output.id;


		String path = mapping.get("output").get("path");
		Collection<Bean> parentBeanList = null;
		boolean isRoot = false;
		if( pmap.keySet().size() == 0){
			isRoot = true;
		}
		if( !isRoot){
			parentBeanList = getParentBeans(pmap, path, context, selector);
			debug("getParentBeanList:"+getParentIdList(parentBeanList));
		}

		Bean listBean = null;
		Map _m = mapping;
		String _selector = (_m.map2parent as boolean) ? getParentSelector(selector) : selector;
		if( isRoot && outputNode.type == NODETYPE_ELEMENT){
			Class clazz = getClass(context,outputNode);
			listBean = new Bean(clazz, lid, _selector, new LocalBeanFactory(clazz,m_beanFactory));
			debug("+++newMapBean:"+beanName+"\t\t\tselector:"+_selector+",id:"+lid+",typeout:"+outputNode.type);
		}else{
			listBean = new Bean(getListClass(context), lid, _selector);
			debug("+++newListBean:"+beanName+"\t\t\tselector:"+_selector+",id:"+lid+",typeout:"+outputNode.type);
		}


		String bid = beanName+getInternalId(context)+"_"+id;
		debug("+++newHashMapBean:"+bid+",\t\t\tselector:"+selector+"|name:"+outputNode.name);
		Class clazz = getClass(context,outputNode);
		Bean mapBean = new Bean(clazz, bid, selector, new LocalBeanFactory(clazz,m_beanFactory));
		if( isRoot && outputNode.type == NODETYPE_ELEMENT){
			debug("\t---bindList:"+beanName+"List,"+beanName);
			mapBean = listBean;
		}else{
			debug("\t---bindList:"+listBean.getBeanId()+" -> "+mapBean.getBeanId()+",propertyname:"+beanName);
			listBean.bindTo(beanName,mapBean);
		}
		context.listBeanMap.put(mapBean.getBeanId(), listBean.getBeanId());

		Bean currentBean = mapBean;
		for( Map<String,Map<String,String>> m in (_m.children as List<Map>)){
			//AttributeMapping
			inputNode = getTreeNodeById( context.inputTree, m.input.id);
			outputNode = getTreeNodeById( context.outputTree, m.output.id);
			String currentPath = getParentSelector((String)outputNode.path);
			debug("--->currentPath:"+currentPath+",basePath:"+basePath);
			if( currentPath == basePath){
				currentBean = mapBean;
			}else{
				String diffPath = currentPath.substring(basePath.length()+1);
				String[] pathElem = diffPath.split("/");
				Bean lastBean = mapBean;
				debug("\tdiffPath:"+diffPath);
				def ppath ="";
				for( String pe in pathElem){
					debug("\tpe:"+basePath+"|"+ppath+"|"+pe);
					Map elementNode = getTreeNodeByPath(context.outputTree, basePath+ppath+"/"+pe);
					debug("\telementNode:"+(elementNode ? elementNode.path:"not found"));
					if( elementNode ){
						currentBean = context.elementIds[(String)elementNode.id];
						if( currentBean == null){
							String name = elementNode.name as String;
							String _id = getInternalId(context);
							clazz = getClass(context,elementNode);
							currentBean = new Bean(clazz, name+_id+"_"+elementNode.id as String, getParentSelector((String)inputNode.path), new LocalBeanFactory(clazz,m_beanFactory));
							context.elementIds[(String)elementNode.id] = currentBean;
						}
						lastBean.bindTo( pe, currentBean);
						lastBean = currentBean;
					}
					ppath += "/"+pe;
				}
			}

			debug("\t---bindAttribute:"+currentBean.getBeanId()+","+outputNode.name+","+inputNode.path+",cp:"+currentPath);
			DataDecoder d = new DecoderWrapper(context, inputNode, outputNode,mapping);
			currentBean.bindTo((String)outputNode.name,(String)inputNode.path,d);
			currentBean.bindTo((String)outputNode.name,replaceLast((String)inputNode.path,"/","/@"),d);
		}
		if( isRoot ){
			context.rootBean = listBean;
			pmap.put(path, mapBean);
		}else{
			pmap.put(path, mapBean);
			for( Bean parentBean in parentBeanList){
				debug("\t---bindParent:"+parentBean.getBeanId()+" -> "+listBean.getBeanId()+",propertyname:"+beanName);
				parentBean.bindTo( beanName, listBean);
			}
		}
	}

	private String replaceLast(String string, String substring, String replacement) {
		int index = string.lastIndexOf(substring);
		if (index == -1){
			return string;
		}
		return string.substring(0, index) + replacement + string.substring(index+substring.length());
	}
	private List getParentIdList(Collection<Bean> col) {
		List ret = new ArrayList();
		for(Bean b in col){
			ret.add(b.getBeanId());
		}
		return ret;
	}
	private String getParentSelector(String string) {
		if( string=="/") return string;
		int index = string.lastIndexOf("/");
		if (index == -1){
			return string;
		}
		String ret = string.substring(0, index);
		return (ret == "") ?  "/" : ret;
	}

	private Map getTreeNodeById(Map node, id ){
		if( node.id == id){
			return node;
		}
		for( Map child in (List<Map>)node.children){
			Map n = getTreeNodeById(child,id);
			if( n != null){
				return n;
			}
		}
		return null;
	}
	private Map getTreeNodeByPath(Map node, path ){
		if( node.path == path){
			return node;
		}
		for( Map child in (List<Map>)node.children){
			Map n = getTreeNodeByPath(child,path);
			if( n != null){
				return n;
			}
		}
		return null;
	}

	private Map getAttributeNode(Map collectionNode, String attr){
		List<Map> children = (List)collectionNode.children;
		for( Map node in children){
			if( node.type == NODETYPE_ATTRIBUTE && node[NODENAME] == attr ){
				return node;
			}
		}
		return null;
	}

	private String getTreeNodeId(String string) {
		int index = string.indexOf("_ID_");
		return string.substring(index+1);
	}
	private String beanName(String string) {
		int index = string.indexOf("_ID_");
		if( index == -1){
			index = string.indexOf("_");
		}
		return string.substring(0,index);
	}
	private String getNameFromId(String string) {
		int index = string.indexOf("_");
		if( index == -1) return string;
		return string.substring(0, index);
		return string;
	}

	private String getInternalId(Context context){
		context.internalId += 1;
		return (context.internalId);
	}
	private void debug(String msg){
		//println(msg);
		m_logger.debug(msg);
	}


	protected void sortMappingByPathLen(List<Map> mappingList) {
		Collections.sort(mappingList, new MappingSortByPathLen());
	}

	protected class MappingSortByPathLen implements Comparator<Map> {
		public int compare(Map<String,Map<String,String>> m1, Map<String,Map<String,String>> m2) {
			int len1 = m1.get("output").get("path").split("/").length;
			int len2 = m2.get("output").get("path").split("/").length;
			return len1-len2;
		}
	}

	protected void updateMappingPathes( List<Map> mappingList,Map outputTree){
		for( Map<String,Map<String,String>> mapping : mappingList){
			String outputId = mapping.get("output").get("id");
			Map node = getTreeNodeById(outputTree, outputId);
			mapping.get("output").put("path",node.path as String);
		}
	}

	protected Collection getParentBeans(MultiValueMap pmap, String path,Context context,String selector){
		debug("----------------------->getParentBeans:"+path+"|sel:"+selector);
		String[] segs = path.split("/");
		int segNum = segs.length-2;
		int _segNum = segNum;
		String pathPrefix = getFirstNSegs(path,segNum);
		debug("getParentPath:"+pathPrefix+"|segNum:"+segNum);
		Collection<Bean> parentList = pmap.getCollection(pathPrefix);
		Bean prevBean = null;
		while( parentList == null){
			pathPrefix = getFirstNSegs(path,segNum);
			debug("pathPrefix:"+pathPrefix+"|segNum:"+segNum);
			Bean dummyBean = createDummyBean(context,selector);

			parentList = pmap.getCollection(pathPrefix);

			if( parentList != null){
				if( prevBean==null) prevBean = dummyBean;
				for( Bean parent in parentList){
					debug("\t---bindParentDummy:"+parent.getBeanId()+" -> "+prevBean.getBeanId()+",propertyname:"+segs[segNum+1]);
					parent.bindTo(segs[segNum+1],prevBean);
				}
			}else{
				if( prevBean != null){
					debug("\t---bindDummy:"+dummyBean.getBeanId()+" -> "+prevBean.getBeanId()+",propertyname:"+segs[segNum+1]);
					dummyBean.bindTo(segs[segNum+1], prevBean);
				}
				pmap.put(pathPrefix,dummyBean);
			}
			prevBean = dummyBean;
			segNum--;

		}
		pathPrefix = getFirstNSegs(path,_segNum);
		parentList = pmap.getCollection(pathPrefix);
		return parentList;
	}

	protected Bean createDummyBean(Context context, String selector){
		Bean b = new Bean( HashMap.class, "autocreated"+getInternalId(context), selector);
		return b
	}

	protected String getFirstNSegs(String path, int segNum){
		debug("getFirstNSegs:"+path+"|"+segNum);
		int pos = path.indexOf("/", 0);
		while (segNum-- > 0 && pos != -1){
			pos = path.indexOf("/", pos+1);
		}
		debug("getFirstNSegs.pos:"+path.substring(0,pos));
		return path.substring(0,pos);
	}

	protected ClassLoader _setContextClassLoader() {
		ClassLoader saveCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(m_nucleusService.getClassLoader(m_storeDesc));
		return saveCl;
	}
	private Class getListClass(Context context){
		if( context.outputTree.format == FORMAT_POJO ){
			return HashSet.class;
		}
		return ArrayList.class;
	}
	
	private Class getClass(Context context, Map node){
		if( context.outputTree.format != FORMAT_POJO ){
			return HashMap.class;
		}
		Class clazz = m_nucleusService.getClass(m_storeDesc,node.name as String);
		debug("getClass.name:"+node.name+"/"+clazz);
		return clazz;
	}
	private String getTabs(Context context,BeanLifecycle event){
		long count=0;
		switch(event){
			case BeanLifecycle.ADD:
			case BeanLifecycle.REMOVE:
				count = context.level;
				break;

			case BeanLifecycle.START_FRAGMENT:
				count = context.level;
				context.level++;
				break;
			case BeanLifecycle.END_FRAGMENT:
				context.level--;
				count = context.level;
				break;
			case BeanLifecycle.POPULATE:
				count = context.level+1;
				break;
		}
		String tabs="";
		for(int i=0; i< count;i++){
			tabs+="\t";
		}
		return tabs;
	}
	private GroovyClassLoader m_loader = null;
	private Script parse(String scriptStr,String scriptname) {
		if( scriptStr == null) return null;
		//debug("parse:" + scriptStr);

		if( m_loader == null){
			ClassLoader parentLoader = this.getClass().getClassLoader();
			CompilerConfiguration config = new CompilerConfiguration();
			config.setScriptBaseClass(org.ms123.common.datamapper.GroovyBase.class.getName());
			m_loader =   new GroovyClassLoader(parentLoader,config);
		}

		try{
			GroovyCodeSource gcs = new GroovyCodeSource( scriptStr, scriptname, "/groovy/shell");
			Script s = InvokerHelper.createScript(m_loader.parseClass(gcs,false), new Binding());
			s.setProperty(SCRIPT_SOURCE,scriptStr);
			s.setProperty(DATAMAPPER_CONFIG,m_configName);
			return s;
		}catch(Exception e){
			if( e instanceof RuntimeException) throw e;
			throw new RuntimeException("<b>Datamapper("+m_configName+"):</b>"+e.getMessage()+" -> "+ scriptname);
		}

	}
	private Object run(Script script, Map vars, String scriptname) {
		debug("run:" + script.getProperty(SCRIPT_SOURCE)+",vars:"+vars);
		vars.put(SCRIPT_NAME, scriptname);
		script.setBinding(new Binding(vars));

		try{
			return script.run();
		}catch(groovy.lang.MissingMethodException e){
			e.printStackTrace();
			Object[] args = e.getArguments();
			String a = "";
			String k = "";
			for(int i=0; i< args.length;i++){
				a += k+args[i];
				k = ",";
			}
			throw new RuntimeException("<b>Datamapper("+m_configName+"):</b>"+e.getMethod() + "("+ a + ") not found -> "+ scriptname);
		}catch(Exception ex){
			ex.printStackTrace();
			Map newVars = new HashMap(vars);
			newVars.remove(SCRIPT_NAME);
			newVars.remove(SCRIPT_SOURCE);
			newVars.remove(DATAMAPPER_CONFIG);
			throw new RuntimeException("<b>Datamapper("+m_configName+"):"+ex.getMessage() +"</b> -> \npath:"+scriptname+"\nexpr:" + script.getProperty(SCRIPT_SOURCE)+"\nvalues:"+newVars);
		}

	}
	private class Context {
		List<Map> mappings;
		Map inputTree;
		Map outputTree;
		Map<String,Bean> elementIds;
		Bean rootBean;
		Transformer transformer;
		long internalId=0;
		long level=0;
		String configName;
		Map<String,Map<String,Map<String,Object>>>  beanValues;
		Map<String,Map<String,Script>>  scriptCache;
		BeanUtilsBean beanUtilsBean;
		Map<String,String> listBeanMap = new HashMap();
	}
	private static class Observer implements BeanContextLifecycleObserver,Constants {
		private static final Log m_logger = LogFactory.getLog(Observer.class);
		public void onBeanLifecycleEvent(BeanContextLifecycleEvent event){
			try{
				long start = new Date().getTime();
				ExecutionContext ec = event.getExecutionContext();
				Context context = (Context)ec.getAttribute(TRANSFORMER_CONTEXT);
				Transformer transformer = context.transformer;
				boolean isList = event.getBean() instanceof List;
				boolean isMap = !isList;

				BeanLifecycle ev = event.getLifecycle();
				if( ev == BeanLifecycle.ADD || ev == BeanLifecycle.REMOVE) return;
				//if( isList && ev != BeanLifecycle.START_FRAGMENT && ev != BeanLifecycle.END_FRAGMENT) return;

				String tabs = transformer.getTabs(context,event.getLifecycle());
				debug(tabs+"Event:"+event.getBeanId()+"/"+event.getLifecycle()+"/"+isMap);
				String beanId = event.getBeanId().getName();
				Map beanNode = transformer.getTreeNodeById(context.outputTree, transformer.getTreeNodeId(beanId));
				if( beanNode == null){
					//throw new RuntimeException("Datamapper.Transformer:treenode_not_found:"+transformer.getTreeNodeId(beanId));
					debug("Datamapper.Transformer:treenode_not_found:"+transformer.getTreeNodeId(beanId));
					return;
				}

				if( event.getLifecycle() == BeanLifecycle.START_FRAGMENT){
					if( isMap){
						context.beanValues.put(beanNode.path as String,new HashMap<String,Map<String,Object>>());
						if(context.scriptCache.get(beanNode.path as String) == null){
							context.scriptCache.put(beanNode.path as String,new HashMap<String,Script>());
						}
					}
					if( isList){
						debug(tabs+"\tStartPathList:"+event.getBean()+"|"+beanNode.path);
					}
				}else if(event.getLifecycle() == BeanLifecycle.END_FRAGMENT){
					if( isList){
						//debug(tabs+"\tEndPathList:"+beanNode.path+"|"+list+"/"+list.getClass());
					}else{
						Map<String,Map<String,Object>> values = context.beanValues.get(beanNode.path);
						if( values == null) return;
						Map<String,Script> scriptMap = context.scriptCache.get(beanNode.path as String);
						BeanContext beanContext = event.getExecutionContext().getBeanContext();
						String wireId = context.listBeanMap.get(beanId);
						debug(tabs+"EndPathMap:"+beanNode.path+"\t|values:"+values+"/"+beanId+"/"+wireId);
						Object beanList = beanContext.getBean(wireId);
						//debug("ListBean---------------------------------------");
						//debug(Transformer.m_js.deepSerialize(beanList));

						for( String key in values.keySet()){
							String propertyName = key;
							Map<String,Object> valMap = values[key];
							String together = "";
							String delim = "";
							for( Object value in valMap.values()){
								together += delim + value as String;
								delim = "/";
							}
							delim = "";
							Script script = scriptMap.get(propertyName+"_"+beanId);
							debug("END_FRAGMENT:"+script+"/"+propertyName+"/"+beanId);
							if( script == null){
								String evalStr = "";
								for( String xkey in valMap.keySet()){
									if( xkey.startsWith("__")) continue;
									evalStr += delim + xkey as String;
									delim = "+'_'+";
								}
								debug(tabs+"evalStr:"+evalStr+"\t\t"+valMap+"|"+propertyName);
								scriptMap.put(propertyName+"_"+beanId, transformer.parse(evalStr,((beanNode.path as String)+"_"+propertyName).replace((char)'/',(char)'_')));
								script = scriptMap.get(propertyName+"_"+beanId);
							}

							Object ores = transformer.run( script, valMap, (beanNode.path as String) +"/"+propertyName);
							if( event.getBean() instanceof Map){
								Map bean = (Map)event.getBean();
								bean.put(propertyName, ores);
							}else{
def clazz=null;
if( ores!=null) clazz=ores.getClass();
debug("setProperty:"+event.getBean()+"/"+propertyName+"="+ores+"/"+clazz);
								context.beanUtilsBean.setProperty(event.getBean(),propertyName,ores);
							}
						}
						if( event.getBean() instanceof Map){
							Map bean = (Map)event.getBean();
							bean.put("class", beanNode.name);
							if( checkBean((Map)event.getBean()) ){
								((List)beanList).remove(event.getBean());
								debug("-----------------------");
								debug("beanContext:"+beanContext);
							}
						}
					}
				}else if ( event.getLifecycle() == BeanLifecycle.POPULATE){
					String propertyName = ec.getAttribute(PROPERTY_NAME) as String;
					Object dataObject = ec.getAttribute(DATAOBJECT);
					DecoderWrapper  decoder = (DecoderWrapper)ec.getAttribute(DECODER);
					Map inputNode = null;
					Map<String,Map<String,String>> mapping;
					String mappingId=null;
					if( decoder != null){
						inputNode = decoder.getInputNode();
						mappingId = decoder.getMapping().id;
					}
					String beanName = transformer.beanName(beanId);
					Map attrNode = transformer.getAttributeNode(beanNode,propertyName);
					if( attrNode){
						String fieldName = attrNode[NODENAME];
						String script = getScriptByMappingId(attrNode.scripts as List<Map>,mappingId);
						debug("script:"+script+"/"+beanId);

						Map<String,Script> scriptMap = context.scriptCache.get(beanNode.path);
						if( scriptMap.get(propertyName) == null){
							scriptMap.put(propertyName+"_"+beanId, transformer.parse(script,((beanNode.path as String)+"/"+propertyName).replace((char)'/',(char)'_')));
						}

						String path = attrNode.path;
						debug(tabs+"Event:"+path+",p:"+propertyName+" ->"+dataObject+","+event.getSource()+"|"+event.getSource().getElement()+"|"+event.getSource().getSAXElement().getName());
						Object b = event.getBean();
						String sourceName = event.getSource().getElement().toString();
						Map<String,Map<String,Object>> values = context.beanValues.get(beanNode.path);
						Map<String,Object> valMap = values.get(propertyName);
						if( valMap == null){
							valMap = new HashMap();
							values.put(propertyName,valMap);
						}
						if( inputNode){
							valMap.put(inputNode.cleanName as String,dataObject);
						}else{
							throw new RuntimeException("<b>Datamapper("+m_configName+"):</b>inputnode is null");
						}
					}
				}
			}catch(Exception e){
				if( e instanceof RuntimeException) throw e;
				throw new RuntimeException(e);
			}
		}
		private String getScriptByMappingId(List<Map> scripts,String mappingId){
			for(Map s in scripts){
				if( s.id == mappingId){
					String script = s.script as String;
					if( script == null || script.trim() == "") return null;
					return script;
				}
			}
			return null;
		}
		boolean checkBean(Map bean){
			debug("checkBean:"+bean);
			if( bean.get("zipInt") as Integer == 666 ) return true;
			return false;
		}
		private void debug(Object msg){
			//println(msg);
			m_logger.debug(msg);
		}
	}
	private static class SWGroovyShell extends GroovyShell{
		private String scriptname;
		public SWGroovyShell(String sn,ClassLoader cl, Binding b, CompilerConfiguration c){
			super(cl,b,c);
			this.scriptname = sn;
		}
		protected synchronized String generateScriptName() {
			return this.scriptname;
		}
	}
	private static class DecoderWrapper implements DataDecoder,Constants {
		private static final Log m_logger = LogFactory.getLog(DecoderWrapper.class);
		private Context context;
		private Map inputNode;
		private Map outputNode;
		private Map mapping;
		private class ConvertDesc{
			Class destClass;
			Converter converter;
		};
		private ConvertDesc desc;
		public DecoderWrapper(Context c, Map inputNode, Map outputNode,Map mapping){
			this.context = c;
			this.inputNode = inputNode;
			this.mapping = mapping;
			this.outputNode = outputNode;
			this.desc  = _getConverter(inputNode.fieldType as String, inputNode.fieldFormat as String);
		}

		public Map getInputNode(){
			return inputNode;
		}

		public Map<String,Map<String,String>> getMapping(){
			return this.mapping;
		}

		public Object decode(String data) throws DataDecodeException {
			if( this.desc.converter==null){
				return data;
			}
			try{
				debug("");
				Class clazz = null;
				Object o = this.desc.converter.convert(desc.destClass, data);
				if(o!=null) clazz = o.getClass();
				debug(getTabs(context)+"Decode:"+data+",in("+inputNode[NODENAME]+","+inputNode.fieldType+","+inputNode.fieldFormat+") -> out("+outputNode[NODENAME]+","+outputNode.fieldType+"):"+o+"/"+clazz);
				return o;
			}catch( ConversionException e){
				throw new RuntimeException("<b>Datamapper("+context.configName+"):</b>"+e.getMessage()+":(Path:"+inputNode.path+",Type:"+inputNode.fieldType+",Format:"+inputNode.fieldFormat+"):input:"+data);
			}
		}
		private ConvertDesc _getConverter(String type,String format){
			ConvertDesc  desc = new ConvertDesc();
			if( type == FIELDTYPE_STRING){
				desc.converter = new StringConverter();
				desc.destClass = String.class;
			}
			if( type == FIELDTYPE_INTEGER){
				IntegerConverter c =  new IntegerConverter();
				if(format!=null) c.setPattern(format);
				desc.converter=c;
				desc.destClass = Integer.class;
			}
			if( type == FIELDTYPE_DECIMAL){
				BigDecimalConverter c =  new BigDecimalConverter();
				if(format!=null) c.setPattern(format);
				desc.converter=c;
				desc.destClass = BigDecimal.class;
			}
			if( type == FIELDTYPE_DOUBLE){
				DoubleConverter c =  new DoubleConverter();
				if(format!=null) c.setPattern(format);
				desc.converter=c;
				desc.destClass = Double.class;
			}
			if( type == FIELDTYPE_LONG){
				LongConverter c =  new LongConverter();
				if(format!=null) c.setPattern(format);
				desc.converter=c;
				desc.destClass = Long.class;
			}
			if( type == FIELDTYPE_BYTE){
				desc.converter =  new ByteConverter();
			}
			if( type == FIELDTYPE_BOOLEAN){
				desc.converter =  new BooleanConverter();
				desc.destClass = Boolean.class;
			}
			if( type == FIELDTYPE_DATE){
				DateConverter c =  new DateConverter();
				if(format!=null) c.setPattern(format);
				desc.converter=c;
				desc.destClass = Date.class;
			}
			if( type == FIELDTYPE_CALENDAR){
				CalendarConverter c =  new CalendarConverter();
				if(format!=null) c.setPattern(format);
				desc.converter=c;
				desc.destClass = Calendar.class;
			}
			return desc;
		}
		private String getTabs(Context context){
			long count=context.level+1;
			String tabs="";
			for(int i=0; i< count;i++){
				tabs+="\t";
			}
			return tabs;
		}
		private void debug(Object msg){
			//println(msg);
			m_logger.debug(msg);
		}
	}

	private static  class LocalBeanFactory<T> implements Factory<T> {
		Class clazz;
		BeanFactory beanFactory;
		public LocalBeanFactory(Class clazz, BeanFactory bf) {
			this.clazz = clazz;
			this.beanFactory=bf;
		}

		public T create(ExecutionContext executionContext) {
			if( beanFactory != null){
				return beanFactory.create(clazz);
			}else{
				return clazz.newInstance();
			}
		}
	}
}
