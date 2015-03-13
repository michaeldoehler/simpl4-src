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
package org.ms123.common.camel.components.repo.producer;

import org.ms123.common.camel.components.repo.RepoConfiguration;
import org.ms123.common.camel.components.repo.RepoEndpoint;
import org.apache.camel.impl.DefaultProducer;
import org.ms123.common.git.GitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RepoProducer extends DefaultProducer {

	private static final transient Logger LOG = LoggerFactory.getLogger(RepoProducer.class);

	protected RepoEndpoint endpoint;

	protected RepoConfiguration configuration;

	public RepoProducer(RepoEndpoint endpoint, RepoConfiguration configuration) {
		super(endpoint);
		this.endpoint = endpoint;
		this.configuration = configuration;
	}

	public GitService getGitService() {
		return getByType(GitService.class);
	}

	private <T> T getByType(Class<T> kls) {
		return kls.cast(endpoint.getCamelContext().getRegistry().lookupByName(kls.getName()));
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
	}
}
