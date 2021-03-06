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

import java.util.Map;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepoComponent extends DefaultComponent {

	private static final transient Logger LOG = LoggerFactory.getLogger(RepoComponent.class);

	protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
		RepoConfiguration configuration = new RepoConfiguration();
		// set options from component
		configuration.setPath((String) parameters.get("path"));
		configuration.setTarget((String) parameters.get("target"));
		configuration.setHeader((String) parameters.get("header"));
		configuration.setRepo((String) parameters.get("repo"));
		configuration.setNewPath((String) parameters.get("newpath"));
		configuration.setOperation(RepoOperation.valueOf(remaining));
		// and then override from parameters
		setProperties(configuration, parameters);
		LOG.info("repo configuration set!");
		Endpoint endpoint = new RepoEndpoint(uri, this, configuration);
		return endpoint;
	}
}
