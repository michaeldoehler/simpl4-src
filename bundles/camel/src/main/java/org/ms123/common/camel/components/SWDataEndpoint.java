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
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultEndpoint;
import org.ms123.common.data.api.DataLayer;

/**
 * Represents a SWData endpoint.
 */
public class SWDataEndpoint extends DefaultEndpoint {

	private SWDataComponent m_component = null;

	private SWDataConsumer m_swDataConsumer;

	private String m_namespace;

	private String m_filterName;

	private String m_objectId;

	private String m_entityType;

	private String m_relation;
	private Boolean m_noUpdate=false;
	private String m_lookupRelationObjectExpr;
	private String m_lookupUpdateObjectExpr;

	private boolean m_writeResultAsHeader;

	private Map m_options;

	public SWDataEndpoint() {
	}

	public SWDataEndpoint(String uri, SWDataComponent component) {
		super(uri, component);
		m_component = component;
	}

	void addConsumer(SWDataConsumer consumer) {
		if (m_swDataConsumer != null) {
			throw new RuntimeException("SWData consumer already defined for " + getEndpointUri() + "!");
		}
		m_swDataConsumer = consumer;
	}

	public void process(Exchange ex) throws Exception {
		if (m_swDataConsumer == null) {
			throw new RuntimeException("SWData consumer not defined for " + getEndpointUri());
		}
		m_swDataConsumer.getProcessor().process(ex);
	}

	public void configureProperties(Map<String, Object> options) {
		info("SWDataEndpoint:" + options);
		m_options = options;
	}

	public Map getOptions() {
		return m_options;
	}

	public void setFilterName(String data) {
		this.m_filterName = data;
	}

	public String getFilterName() {
		return m_filterName;
	}

	public void setEntityType(String data) {
		this.m_entityType = data;
	}

	public String getEntityType() {
		return m_entityType;
	}
	public Boolean isNoUpdate() {
		return m_noUpdate;
	}
	public void setNoUpdate(boolean nou) {
		m_noUpdate=nou;
	}
	public String getRelation() {
		return m_relation;
	}
	public void setRelation(String relation) {
		m_relation=relation;
	}
	public String getLookupRelationObjectExpr() {
		return m_lookupRelationObjectExpr;
	}
	public void setRelationObjectExpr(String lookup) {
		m_lookupRelationObjectExpr=lookup;
	}
	public String getLookupUpdateObjectExpr() {
		return m_lookupUpdateObjectExpr;
	}
	public void setLookupUpdateObjectExpr(String update) {
System.out.println("setLookupUpdateObjectExpr:"+update);
		m_lookupUpdateObjectExpr=update;
	}

	public void setObjectId(String data) {
		this.m_objectId = data;
	}

	public String getObjectId() {
		return m_objectId;
	}

	public void setNamespace(String namespace) {
		this.m_namespace = namespace;
	}

	public String getNamespace() {
		return m_namespace;
	}

	public boolean isWriteResultAsHeader() {
		return m_writeResultAsHeader;
	}

	public void setWriteResultAsHeader(boolean writeResultAsHeader) {
		m_writeResultAsHeader = writeResultAsHeader;
	}

	public SWDataEndpoint(String endpointUri) {
		super(endpointUri);
	}

	public Producer createProducer() throws Exception {
		return new SWDataProducer(this);
	}

	public Consumer createConsumer(Processor processor) throws Exception {
		return new SWDataConsumer(this, processor);
	}

	protected DataLayer getDataLayer() {
		return m_component.getDataLayer();
	}

	public boolean isSingleton() {
		return true;
	}
	private void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(SWDataEndpoint.class);
}
