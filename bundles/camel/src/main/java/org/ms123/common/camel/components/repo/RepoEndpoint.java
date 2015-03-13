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
package org.ms123.common.camel.components.repo;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.ms123.common.camel.components.repo.producer.RepoDelProducer;
import org.ms123.common.camel.components.repo.producer.RepoGetProducer;
import org.ms123.common.camel.components.repo.producer.RepoMoveProducer;
import org.ms123.common.camel.components.repo.producer.RepoPutProducer;
import org.apache.camel.impl.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepoEndpoint extends DefaultEndpoint {

	private static final transient Logger LOG = LoggerFactory.getLogger(RepoEndpoint.class);

	private RepoConfiguration m_configuration;

	public RepoEndpoint() {
	}

	public RepoEndpoint(String uri, RepoComponent component, RepoConfiguration configuration) {
		super(uri, component);
		m_configuration = configuration;
	}

	public RepoEndpoint(String endpointUri) {
		super(endpointUri);
	}

	public Producer createProducer() throws Exception {
		LOG.info("resolve producer repo endpoint {" + m_configuration.getOperation().toString() + "}");
		if (m_configuration.getOperation() == RepoOperation.put) {
			return new RepoPutProducer(this, m_configuration);
		} else if (m_configuration.getOperation() == RepoOperation.del) {
			return new RepoDelProducer(this, m_configuration);
		} else if (m_configuration.getOperation() == RepoOperation.get) {
			return new RepoGetProducer(this, m_configuration);
		} else if (m_configuration.getOperation() == RepoOperation.move) {
			return new RepoMoveProducer(this, m_configuration);
		} else {
			throw new RuntimeException("operation specified is not valid for producer!");
		}
	}

	public Consumer createConsumer(Processor processor) throws Exception {
		throw new RuntimeException("operation specified is not valid for consumer!");
	}

	public boolean isSingleton() {
		return true;
	}
}
