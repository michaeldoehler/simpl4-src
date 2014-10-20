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
package org.ms123.common.workflow.api;

import java.io.*;
import java.util.*;
import groovy.lang.*;
import java.text.*;
import java.util.concurrent.TimeUnit;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.camel.api.CamelService;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.Event;
import org.osgi.framework.BundleContext;
import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.util.ExchangeHelper;
import org.apache.commons.beanutils.BeanMap;
import flexjson.*;

@SuppressWarnings("unchecked")
public abstract class GroovyTaskDslBase extends Script {

	protected CamelContext m_camelContext;
	protected ProducerTemplate m_camelTemplate;

	protected Inflector m_inflector = Inflector.getInstance();

	protected JSONSerializer m_js = new JSONSerializer();

	protected JSONDeserializer m_ds = new JSONDeserializer();

	public List executeFilter(String moduleName, String filter) {
		List<Object> res = getSessionContext().query(moduleName, filter);
		for (Object o : res) {
			addQueriedObject(o);
		}
		return res;
	}

	public List executeNamedFilter(String filterName) {
		Map ret = getSessionContext().executeNamedFilter(filterName);
		if (ret != null) {
			return (List) ret.get("rows");
		}
		return new ArrayList();
	}

	public List executeNamedFilter(String filterName, Map params) {
		Map ret = getSessionContext().executeNamedFilter(filterName, params);
		if (ret != null) {
			return (List) ret.get("rows");
		}
		return new ArrayList();
	}

	public Object getObjectById(String moduleName, Object id) {
		Class clazz = getSessionContext().getClass(m_inflector.getClassName(moduleName));
		try {
			return getSessionContext().getObjectById(clazz, id);
		} catch (javax.jdo.JDOObjectNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Object createObject(String moduleName) {
		Object o = getSessionContext().createObject(moduleName);
		getSessionContext().makePersistent(o);
		addCreatedObject(o);
		return o;
	}

	public Object createInstance(String moduleName) {
		Class c = getSessionContext().getClass(moduleName);
		try {
			return c.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String dec2(Object o) {
		if (o instanceof Integer) {
			o = ((Integer) o).floatValue();
		}
		return String.format("%1$,.2f", o);
	}

	public String today(Object o) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat formater = new SimpleDateFormat();
		return formater.format(cal.getTime());
	}

	public long toDays(long duration) {
		return TimeUnit.MILLISECONDS.toDays(duration);
	}

	public long toHours(long duration) {
		return TimeUnit.MILLISECONDS.toHours(duration);
	}

	public long toMicros(long duration) {
		return TimeUnit.MILLISECONDS.toMicros(duration);
	}

	public long toMillis(long duration) {
		return TimeUnit.MILLISECONDS.toMillis(duration);
	}

	public long toMinutes(long duration) {
		return TimeUnit.MILLISECONDS.toMinutes(duration);
	}

	public long toNanos(long duration) {
		return TimeUnit.MILLISECONDS.toNanos(duration);
	}

	public long toSeconds(long duration) {
		return TimeUnit.MILLISECONDS.toSeconds(duration);
	}

	//public void log(String msg) {
	//	Map props = new HashMap();
	//	props.put("param1", getPid());
	//	props.put("msg", msg);
	//	System.out.println("TaskApi.postEvent:" + getEventAdmin());
	//	getEventAdmin().postEvent(new Event("process/scripttask", props));
	//}
	public void addCreatedObject(Object o) {
		List cl = (List) this.getBinding().getVariable("__createdObjects");
		cl.add(o);
	}

	public void addQueriedObject(Object o) {
		List cl = (List) this.getBinding().getVariable("__queriedObjects");
		cl.add(o);
	}

	private EventAdmin getEventAdmin() {
		return (EventAdmin) this.getBinding().getVariable("__eventAdmin");
	}

	private SessionContext getSessionContext() {
		return (SessionContext) this.getBinding().getVariable("__sessionContext");
	}

	private String getNamespace() {
		return (String) this.getBinding().getVariable("__namespace");
	}

	private String getProcessDefinitionKey() {
		return (String) this.getBinding().getVariable("__processDefinitionKey");
	}

	private String getPid() {
		return (String) this.getBinding().getVariable("__pid");
	}

	public CamelContext getCamelContext() {
		if (m_camelContext == null){
			CamelService ws = (CamelService) this.getBinding().getVariable("__camelService");
			m_camelContext = ws.getCamelContext(getNamespace(), CamelService.DEFAULT_CONTEXT );
		}
		return m_camelContext;
	}

	public ProducerTemplate getCamelTemplate(){
		if( m_camelTemplate == null){
			m_camelTemplate = getCamelContext().createProducerTemplate();
		}
		return m_camelTemplate;
	}
	
	public Object camelSend(String epUri, final Map<String, Object> properties) {
		return camelSend(epUri, null, null,properties);
	}
	public Object camelSend(String epUri, final Object body, final Map<String, Object> properties) {
		return camelSend(epUri, body, null,properties);
	}
	public Object camelSend(String epUri, final Object body, final Map<String, Object> headers, final Map<String, Object> properties) {
		Processor p = new Processor() {
			public void process(Exchange exchange) {
				if (properties != null) {
					for (String key : properties.keySet()) {
						exchange.setProperty(key, properties.get(key));
					}
				}
				Message in = exchange.getIn();
				if (headers != null) {
					for (String key : headers.keySet()) {
						in.setHeader(key, headers.get(key));
					}
				}
				in.setBody(body);
			}
		};
		Exchange result = getCamelTemplate().send(epUri, p);
    return ExchangeHelper.extractResultBody(result, null);
	}

	public Map beanToMap(Object bean) {
		return (Map) m_ds.deserialize(m_js.deepSerialize(bean));
	}

	public boolean isListEmpty(List l1) {
		if (l1 != null && l1.size() == 0) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isListInList(Object o1, Object o2) {
		return isListInList(o1, o2, "id");
	}

	public boolean isListInList(Object o1, Object o2, String key) {
		System.out.println("contains:" + o1 + "/" + o2 + "/" + key);
		if (o1 instanceof List && o2 instanceof List) {
			List l1 = (List) o1;
			List l2 = (List) o2;
			if (l1.size() == 0) {
				return true;
			}
			if (l2.size() == 0) {
				return false;
			}
			if (l1.get(0) instanceof Map) {
				for (Map m1 : (List<Map>) l1) {
					Object val1 = (Object) m1.get(key);
					boolean ok = false;
					for (Map m2 : (List<Map>) l2) {
						Object val2 = (Object) m2.get(key);
						if (val1.equals(val2)) {
							ok = true;
						}
					}
					if (!ok) {
						System.out.println("l2:" + l2 + "contains not " + l1);
						return false;
					}
				}
			}
			if (l1.get(0) instanceof String) {
				for (String s1 : (List<String>) l1) {
					boolean ok = false;
					for (String s2 : (List<String>) l2) {
						if (s1.equals(s2)) {
							ok = true;
						}
					}
					if (!ok) {
						System.out.println("l2:" + l2 + "contains not " + l1);
						return false;
					}
				}
			}
			System.out.println("l2:" + l2 + "contains " + l1);
			return true;
		}
		return false;
	}

	public void log(String message) {
		m_logger.info(message);
		System.out.println(message);
	}

	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(GroovyTaskDslBase.class);
}
