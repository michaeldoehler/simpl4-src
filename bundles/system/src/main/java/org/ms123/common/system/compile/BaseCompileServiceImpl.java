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
package org.ms123.common.system.compile;

import flexjson.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import org.ms123.common.git.GitService;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.store.StoreDesc;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.tools.FileSystemCompiler;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.ms123.common.utils.Utils;

/**
 *
 */
@SuppressWarnings("unchecked")
abstract class BaseCompileServiceImpl {
	public final String GROOVY_TYPE = "sw.groovy";
	public final String DIRECTORY_TYPE = "sw.directory";
	public final String PATH = "path";
	public final String MSG = "msg";

	protected Inflector m_inflector = Inflector.getInstance();

	protected BundleContext m_bundleContext;
	protected GitService m_gitService;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	public void _compileGroovyAll(){
		List<Map> repos = m_gitService.getRepositories(new ArrayList(),false);
		for(Map<String,String> repo : repos){
			String namespace = repo.get("name");
			info("Compile in "+namespace+":");
			List<Map> resultList = compileGroovyNamespace(namespace);
			for( Map<String,String> result : resultList){
				info("CompileGroovy:"+result.get(PATH) + " -> "+ result.get(MSG));
			}
		}
	}

	public List<Map> compileGroovyNamespace(String namespace){
		List<String> types = new ArrayList();
		types.add(GROOVY_TYPE);
		types.add(DIRECTORY_TYPE);
		List<String> typesGroovy = new ArrayList();
		typesGroovy.add(GROOVY_TYPE);

		Map map= m_gitService.getWorkingTree(namespace, null, 100, types, null, null,null);
		List<Map> pathList = new ArrayList();
		toFlatList(map,typesGroovy,pathList);

		List<Map> resultList = new ArrayList();
		for( Map pathMap : pathList){
			String path = (String)pathMap.get(PATH);
			String  code = m_gitService.getContent(namespace, path);
			String msg = _compileGroovy(namespace,path,code);
			Map<String,String> result = new HashMap();
			result.put(PATH, path);
			result.put(MSG,msg);
			resultList.add(result);
		}
		return resultList;
	}

	public  void	compileGroovy(String namespace,String path,String code){
		String msg = _compileGroovy(namespace,path,code);
		if( msg!=null){
			throw new RuntimeException(msg);
		}
	}

	private String	_compileGroovy(String namespace,String path,String code){
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
			return Utils.formatGroovyException(e,code);
		}
		return null;
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
	protected static void info(String msg) {
		System.err.println(msg);
		m_logger.info(msg);
	}
	protected static void debug(String msg) {
		m_logger.debug(msg);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(CompileServiceImpl.class);
}
