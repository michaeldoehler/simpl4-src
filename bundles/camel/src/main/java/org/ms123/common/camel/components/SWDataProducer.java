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
package org.ms123.common.camel.components;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.CamelContext;
import org.apache.camel.util.MessageHelper;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.CamelContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.system.ThreadContext;

/**
 * The SWData producer.
 */
@SuppressWarnings("unchecked")
public class SWDataProducer extends DefaultProducer {

	private String m_filterName = null;

	private String m_namespace = null;

	private String m_objectId = null;
	private String m_lookupUpdateObjectExpr = null;
	private String m_lookupRelationObjectExpr = null;
	private String m_relation = null;
	private Boolean m_noUpdate = false;

	private String m_entityType = null;

	private SWDataOperation m_operation;

	private SWDataEndpoint m_endpoint;

	private Map m_options;
	private PermissionService m_permissionService;

	DataLayer m_dataLayer = null;

	private static final transient Logger LOG = LoggerFactory.getLogger(SWDataProducer.class);

	public SWDataProducer(SWDataEndpoint endpoint) {
		super(endpoint);
		CamelContext camelContext = endpoint.getCamelContext();
		m_endpoint = endpoint;
		m_dataLayer = endpoint.getDataLayer();
		m_options = endpoint.getOptions();
		m_namespace = endpoint.getNamespace();
		m_filterName = endpoint.getFilterName();
		m_objectId = endpoint.getObjectId();
		m_entityType = endpoint.getEntityType();
		m_lookupRelationObjectExpr = endpoint.getLookupRelationObjectExpr();
		m_lookupUpdateObjectExpr = endpoint.getLookupUpdateObjectExpr();
		m_relation = endpoint.getRelation();
		m_noUpdate = endpoint.isNoUpdate();
		String endpointKey = endpoint.getEndpointKey();
		if (endpointKey.indexOf("?") != -1) {
			endpointKey = endpointKey.split("\\?")[0];
		}
		if (endpointKey.indexOf(":") == -1) {
			throw new RuntimeException("SWDataProducer.no_operation_in_uri:" + endpointKey);
		}
		String[] path = endpointKey.split(":");
		m_operation = SWDataOperation.valueOf(path[1].replace("//", ""));
		info("m_operation:" + m_operation);
		if (path.length > 2) {
			m_filterName = path[2].split("\\?")[0];
		}
		if( m_namespace == null){
			m_namespace = (String)CamelContextHelper.mandatoryLookup(camelContext, "namespace");
		}
		m_permissionService = CamelContextHelper.mandatoryLookup(camelContext, PermissionService.class.getName(), PermissionService.class);
	}

	public void process(Exchange exchange) throws Exception {
		String ns = m_namespace;
		if( ThreadContext.getThreadContext() == null){
			System.out.println("getThreadContext");
			ThreadContext.loadThreadContext(ns, "admin");
			m_permissionService.loginInternal(ns);
		}
		invokeOperation(m_operation, exchange);
	}

	/**
	 * Entry method that selects the appropriate MongoDB operation and executes it
	 * @param operation
	 * @param exchange
	 * @throws Exception
	 */
	protected void invokeOperation(SWDataOperation operation, Exchange exchange) throws Exception {
		switch(operation) {
			case findOneByFilter:
				doFindOneByFilter(exchange);
				break;
			case findByFilter:
				doFindByFilter(exchange);
				break;
			case findById:
				doFindById(exchange);
				break;
			case insert:
				doInsert(exchange);
				break;
			case update:
				doUpdate(exchange);
				break;
			case delete:
				doDelete(exchange);
				break;
			case multiInsertUpdate:
				doMultiInsertUpdate(exchange);
				break;
			/*case aggregate:
				doAggregate(exchange);
				break;*/
			default:
				throw new RuntimeException("SWDataProducer.Operation not supported. Value: " + operation);
		}
	}

	private String getStringCheck(Exchange e, String key, String def) {
		String value = e.getIn().getHeader(key, String.class);
		info("getStringCheck:"+key+"="+value+"/def:"+def);
		if (value == null && def == null){
			throw new RuntimeException("SWDataProducer." + key + "_is_null");
		}
		return value != null ? value : def;
	}
	private String getString(Exchange e, String key, String def) {
		String value = e.getIn().getHeader(key, String.class);
		info("getString:"+key+"="+value+"/def:"+def);
		return value != null ? value : def;
	}
	private boolean getBoolean(Exchange e, String key, boolean def) {
		Boolean value = e.getIn().getHeader(key, Boolean.class);
		info("getString:"+key+"="+value+"/def:"+def);
		return value != null ? value : def;
	}
	private void doMultiInsertUpdate(Exchange exchange) {
		String lookupUpdateObjectExpr = getString(exchange, SWDataConstants.LOOKUP_UPDATE_OBJECT_EXPR, m_lookupUpdateObjectExpr);
		String lookupRelationObjectExpr = getString(exchange, SWDataConstants.LOOKUP_RELATION_OBJECT_EXPR, m_lookupRelationObjectExpr);
		String relation = getString(exchange, SWDataConstants.RELATION, m_relation);
		if( "-".equals(relation))relation=null;
		Boolean no_update = getBoolean(exchange, SWDataConstants.NO_UPDATE, m_noUpdate);
		Map<String,Object> persistenceSpecification = new HashMap();
		persistenceSpecification.put(SWDataConstants.LOOKUP_RELATION_OBJECT_EXPR,lookupRelationObjectExpr);
		persistenceSpecification.put(SWDataConstants.LOOKUP_UPDATE_OBJECT_EXPR,lookupUpdateObjectExpr);
		persistenceSpecification.put(SWDataConstants.RELATION,relation);
		persistenceSpecification.put(SWDataConstants.NO_UPDATE,no_update);
		System.out.println("persistenceSpecification:"+persistenceSpecification);
		//String entityType = getStringCheck(exchange, SWDataConstants.ENTITY_TYPE, m_entityType);
		SessionContext sc = getSessionContext();
		Exception ex = null;
		List<Map> result = null;
		try {
			result = sc.persistObjects(exchange.getIn().getBody(),persistenceSpecification);
		} catch (Exception e) {
			ex = e;
		}
		Message resultMessage = prepareResponseMessage(exchange, SWDataOperation.multiInsertUpdate);
		processAndTransferResult(null, exchange, ex);
	}

	private void doDelete(Exchange exchange) {
		String objectId = getStringCheck(exchange, SWDataConstants.OBJECT_ID, m_objectId);
		String entityType = getStringCheck(exchange, SWDataConstants.ENTITY_TYPE, m_entityType);
		SessionContext sc = getSessionContext();
		Exception ex = null;
		Map result = null;
		try {
			result = sc.deleteObjectById(entityType, objectId);
		} catch (Exception e) {
			ex = e;
		}
		Message resultMessage = prepareResponseMessage(exchange, SWDataOperation.delete);
		processAndTransferResult(result, exchange, ex);
	}

	private void doInsert(Exchange exchange) {
		String entityType = getStringCheck(exchange, SWDataConstants.ENTITY_TYPE, m_entityType);
		Map insert = exchange.getIn().getBody(Map.class);
		SessionContext sc = getSessionContext();
		Exception ex = null;
		Map result = null;
		try {
			result = sc.insertObjectMap(insert, entityType);
		} catch (Exception e) {
			ex = e;
		}
		Message resultMessage = prepareResponseMessage(exchange, SWDataOperation.insert);
		processAndTransferResult(result, exchange, ex);
		resultMessage.setBody(result);
	}

	private void doUpdate(Exchange exchange) {
		String objectId = getStringCheck(exchange, SWDataConstants.OBJECT_ID, m_objectId);
		String entityType = getStringCheck(exchange, SWDataConstants.ENTITY_TYPE, m_entityType);
		Map update = exchange.getIn().getBody(Map.class);
		SessionContext sc = getSessionContext();
		Exception ex = null;
		Map result = null;
		try {
			result = sc.updateObjectMap(update, entityType, objectId);
		} catch (Exception e) {
			ex = e;
		}
		Message resultMessage = prepareResponseMessage(exchange, SWDataOperation.update);
		processAndTransferResult(result, exchange, ex);
	}

	private void doFindById(Exchange exchange) {
		String objectId = getStringCheck(exchange, SWDataConstants.OBJECT_ID, m_objectId);
		String entityType = getStringCheck(exchange, SWDataConstants.ENTITY_TYPE, m_entityType);
		SessionContext sc = getSessionContext();
		Object ret = sc.getObjectMapById(m_entityType, m_objectId);
		Message resultMessage = prepareResponseMessage(exchange, SWDataOperation.findById);
		resultMessage.setBody(ret);
	}

	private void doFindByFilter(Exchange exchange) {
		String filterName = getStringCheck(exchange, SWDataConstants.FILTER_NAME, m_filterName);
		SessionContext sc = getSessionContext();
		List result = null;
		Map exVars = ExchangeUtils.prepareVariables(exchange, true,false,false);
		Map retMap = sc.executeNamedFilter(filterName, exVars,m_options);
		if (retMap == null) {
			result = new ArrayList();
		} else {
			result = (List) retMap.get("rows");
		}
		Message resultMessage = prepareResponseMessage(exchange, SWDataOperation.findByFilter);
		resultMessage.setHeader(SWDataConstants.ROW_COUNT, result.size());
		resultMessage.setBody(result);
	}

	private void doFindOneByFilter(Exchange exchange) {
		String filterName = getStringCheck(exchange, SWDataConstants.FILTER_NAME, m_filterName);
		SessionContext sc = getSessionContext();
		Map result = null;
		Map exVars = ExchangeUtils.prepareVariables(exchange, true,false,false);
		Map retMap = sc.executeNamedFilter(filterName, exVars,m_options);
		if (retMap == null) {
		} else {
			List rows = (List) retMap.get("rows");
			if (rows.size() > 0) {
				result = (Map) rows.get(0);
			}
		}
		Message resultMessage = prepareResponseMessage(exchange, SWDataOperation.findOneByFilter);
		resultMessage.setHeader(SWDataConstants.ROW_COUNT, 1);
		resultMessage.setBody(result);
	}

	private SessionContext getSessionContext() {
		StoreDesc sdesc = StoreDesc.getNamespaceData(m_namespace);
		SessionContext sc = m_dataLayer.getSessionContext(sdesc);
		return sc;
	}

	private void processAndTransferResult(Map result, Exchange exchange, Exception ex) {
		if (ex != null) {
			exchange.getOut().setHeader(SWDataConstants.LAST_ERROR, ex.getMessage());
			exchange.setException(ex);
		}
		if (m_endpoint.isWriteResultAsHeader()) {
			exchange.getOut().setHeader(SWDataConstants.WRITE_RESULT, result);
		} else {
			exchange.getOut().setBody(result);
		}
	}

	private Message prepareResponseMessage(Exchange exchange, SWDataOperation operation) {
		Message answer = exchange.getOut();
		MessageHelper.copyHeaders(exchange.getIn(), answer, false);
		if (isWriteOperation(operation) && m_endpoint.isWriteResultAsHeader()) {
			answer.setBody(exchange.getIn().getBody());
		}
		return answer;
	}

	private boolean isWriteOperation(SWDataOperation operation) {
		return SWDataComponent.WRITE_OPERATIONS.contains(operation);
	}
	private void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(SWDataProducer.class);
}
