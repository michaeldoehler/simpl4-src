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
import java.io.File;
import java.security.MessageDigest;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.git.GitService;
import org.ms123.common.git.FileHolderApi;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.system.TransactionService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.datamapper.DatamapperService;
import org.ms123.common.utils.Inflector;
import org.ms123.common.utils.Utils;
import org.osgi.framework.BundleContext;
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
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.core.osgi.OsgiServiceRegistry;
import org.apache.camel.processor.interceptor.Tracer;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.ms123.common.libhelper.FileSystemClassLoader;
import org.ms123.common.libhelper.ClassLoaderWrapper;
import groovy.lang.GroovyShell;
import org.ms123.common.camel.components.*;
import org.ms123.common.camel.trace.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.apache.camel.spring.spi.SpringTransactionPolicy;

/**
 *
 */
@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
public class CamelContextBuilder {

	private static final Logger m_logger = LoggerFactory.getLogger(CamelContextBuilder.class);

	public static synchronized ModelCamelContext createCamelContext(String namespace, Registry groovyRegistry, BundleContext bc,boolean trace) throws Exception {
		SimpleRegistry sr = new SimpleRegistry();
		OsgiServiceRegistry or = new OsgiServiceRegistry(bc);
		sr.put(PermissionService.PERMISSION_SERVICE, or.lookupByName(PermissionService.class.getName()));
		sr.put(DataLayer.DATA_LAYER, or.lookupByNameAndType("dataLayer", DataLayer.class));
		sr.put("datamapper", or.lookupByName(DatamapperService.class.getName()));
		sr.put("namespace", namespace);
	//	sr.put("sleepBean", new BaseCamelServiceImpl.SleepBean());
		sr.put("activiti", new ActivitiComponent());
		sr.put("swdata", new SWDataComponent());

		TransactionService ts = (TransactionService)or.lookupByName(TransactionService.class.getName());
		sr.put(org.springframework.transaction.PlatformTransactionManager.class.getName(), ts.getPlatformTransactionManager());
		createTransactionPolicies(ts.getPlatformTransactionManager(), sr);

		ModelCamelContext camelContext;
		camelContext = new OsgiDefaultCamelContext(bc);

		Registry r = camelContext.getRegistry();
		CompositeRegistry cr = new CompositeRegistry();
		cr.addRegistry(or);
		cr.addRegistry(sr);
		cr.addRegistry(r);
		if( groovyRegistry != null){
			cr.addRegistry(groovyRegistry);
		}
		
		camelContext = new OsgiDefaultCamelContext(bc, cr);
		camelContext.setApplicationContextClassLoader(new ClassLoaderWrapper(CamelContextBuilder.class.getClassLoader(), groovy.lang.Script.class.getClassLoader()));
		DefaultErrorHandlerBuilder dehb = new DefaultErrorHandlerBuilder();
    dehb.logExhaustedMessageHistory(false);
    dehb.disableRedelivery();
		camelContext.setErrorHandlerBuilder(dehb);
		Tracer tracer = new Tracer();
		tracer.setTraceOutExchanges(true);
		TraceFormatter formatter = new TraceFormatter();
		formatter.setShowBreadCrumb(false);
		formatter.setShowProperties(false);
		formatter.setShowOutBody(false);
		formatter.setShowBody(false);
		formatter.setShowOutHeaders(false);
		formatter.setShowHeaders(false);
		formatter.setShowBodyType(false);
		formatter.setShowExchangePattern(false);
		formatter.setShowNode(true);
		tracer.setFormatter(formatter);
		tracer.setTraceExceptions(true);
		tracer.setTraceHandler(new TraceEventHandler(true));
		if( trace){
			camelContext.addInterceptStrategy(tracer);
			camelContext.setTracing(true);
		}
		camelContext.getProperties().put(Exchange.LOG_DEBUG_BODY_STREAMS, "true");
		camelContext.setMessageHistory(true);
		return camelContext;
	}

	private static void createTransactionPolicies(PlatformTransactionManager ptm, SimpleRegistry sr){
		String[] names = { "PROPAGATION_MANDATORY",
											 "PROPAGATION_NESTED",
											 "PROPAGATION_NEVER",
											 "PROPAGATION_NOT_SUPPORTED",
											 "PROPAGATION_REQUIRED",
											 "PROPAGATION_REQUIRES_NEW",
											 "PROPAGATION_SUPPORTS" };

		for( String name : names){
			SpringTransactionPolicy stp = new SpringTransactionPolicy(ptm);
			stp.setPropagationBehaviorName(name);
			sr.put(name, stp);	
		}
	}

	private static void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
}
