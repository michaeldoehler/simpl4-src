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

import org.apache.camel.Exchange;
import org.ms123.common.camel.components.repo.RepoConfiguration;
import org.ms123.common.camel.components.repo.RepoEndpoint;

public class RepoMoveProducer extends RepoProducer {

	public RepoMoveProducer(RepoEndpoint endpoint, RepoConfiguration configuration) {
		super(endpoint, configuration);
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		//RepoResult result = RepoAPIFacade.getInstance(configuration.getClient()) .move(configuration.getRemotePath(), configuration.getNewRemotePath());
		//result.populateExchange(exchange);
		log.info("Moved from " + configuration.getPath() + " to " + configuration.getNewPath());
	}
}
