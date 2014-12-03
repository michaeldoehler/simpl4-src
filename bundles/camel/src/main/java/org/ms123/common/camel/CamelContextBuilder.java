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

import java.util.EventObject;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.CamelContext;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.core.osgi.OsgiServiceRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.impl.CompositeRegistry;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.management.event.ExchangeCompletedEvent;
import org.apache.camel.management.event.ExchangeCreatedEvent;
import org.apache.camel.management.event.ExchangeSentEvent;
import org.apache.camel.management.event.ExchangeSendingEvent;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.processor.interceptor.Tracer;
import org.apache.camel.spi.Registry;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.apache.camel.support.EventNotifierSupport;
import org.ms123.common.camel.components.*;
import org.ms123.common.camel.trace.*;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.datamapper.DatamapperService;
import org.ms123.common.libhelper.ClassLoaderWrapper;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.system.ThreadContext;
import org.ms123.common.system.TransactionService;
import org.osgi.framework.BundleContext;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

/**
 *
 */
public class CamelContextBuilder {

	private static final Logger m_logger = LoggerFactory.getLogger(CamelContextBuilder.class);

	public static synchronized ModelCamelContext createCamelContext(String namespace, Registry groovyRegistry, BundleContext bc, boolean trace) throws Exception {
		SimpleRegistry sr = new SimpleRegistry();
		OsgiServiceRegistry or = new OsgiServiceRegistry(bc);
		PermissionService permissionService = (PermissionService) or.lookupByName(PermissionService.class.getName());
		sr.put(PermissionService.PERMISSION_SERVICE, permissionService);
		sr.put(DataLayer.DATA_LAYER, or.lookupByNameAndType("dataLayer", DataLayer.class));
		sr.put("datamapper", or.lookupByName(DatamapperService.class.getName()));
		sr.put("namespace", namespace);
		sr.put("activiti", new ActivitiComponent());
		sr.put("swdata", new SWDataComponent());
		TransactionService ts = (TransactionService) or.lookupByName(TransactionService.class.getName());
		sr.put(org.springframework.transaction.PlatformTransactionManager.class.getName(), ts.getPlatformTransactionManager());
		createTransactionPolicies(ts.getPlatformTransactionManager(), sr);
		ModelCamelContext camelContext;
		camelContext = new OsgiDefaultCamelContext(bc);
		Registry r = camelContext.getRegistry();
		CompositeRegistry cr = new CompositeRegistry();
		cr.addRegistry(or);
		cr.addRegistry(sr);
		cr.addRegistry(r);
		if (groovyRegistry != null) {
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
		if (trace) {
			camelContext.addInterceptStrategy(tracer);
			camelContext.setTracing(true);
		}
		camelContext.getProperties().put(Exchange.LOG_DEBUG_BODY_STREAMS, "true");
		camelContext.setMessageHistory(true);
		camelContext.getManagementStrategy().addEventNotifier(new ExchangeEventNotifer(namespace, permissionService));
		return camelContext;
	}

	private static void createTransactionPolicies(PlatformTransactionManager ptm, SimpleRegistry sr) {
		String[] names = { "PROPAGATION_MANDATORY", "PROPAGATION_NESTED", "PROPAGATION_NEVER", "PROPAGATION_NOT_SUPPORTED", "PROPAGATION_REQUIRED", "PROPAGATION_REQUIRES_NEW", "PROPAGATION_SUPPORTS" };
		for (String name : names) {
			SpringTransactionPolicy stp = new SpringTransactionPolicy(ptm);
			stp.setPropagationBehaviorName(name);
			sr.put(name, stp);
		}
	}

	public static class ExchangeEventNotifer extends EventNotifierSupport {
		PermissionService m_permissionService;
		String m_namespace;

		public ExchangeEventNotifer(String ns, PermissionService ps) {
			m_namespace = ns;
			m_permissionService = ps;
		}

		public void notify(EventObject event) throws Exception {
			if (event instanceof ExchangeCreatedEvent) {
				ExchangeCreatedEvent ev = (ExchangeCreatedEvent) event;
				if( ev.getExchange().getProperty(Exchange.CORRELATION_ID )==null){
					ThreadContext tc = ThreadContext.getThreadContext();
					info("------>EventNotifierSupportStart:" + ev +"/"+tc);
					if( tc == null){
						m_permissionService.loginInternal(m_namespace);
						ThreadContext.getThreadContext().put(ev.getExchange().getExchangeId(),true);
					}
				}
			}
			if (event instanceof ExchangeCompletedEvent) {
				ExchangeCompletedEvent ev = (ExchangeCompletedEvent) event;
				if( ev.getExchange().getProperty(Exchange.CORRELATION_ID )==null){
					info("<-----EventNotifierSupportComplete:" + ev );
					if(ThreadContext.getThreadContext().get(ev.getExchange().getExchangeId()) != null){
						ThreadContext.getThreadContext().finalize(null);
					}
				}
			}
		}

		public boolean isEnabled(EventObject event) {
			if (event instanceof ExchangeCreatedEvent) return true;
			if (event instanceof ExchangeCompletedEvent) return true;
			return false;
		}

		protected void doStart() throws Exception {
		}

		protected void doStop() throws Exception {
		}
	}

	private static void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
}
