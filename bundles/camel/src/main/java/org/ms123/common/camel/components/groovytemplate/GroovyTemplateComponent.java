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
package org.ms123.common.camel.components.groovytemplate;

import java.util.Map;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.util.ResourceHelper;

/**
 * @version 
 */
public class GroovyTemplateComponent extends DefaultComponent {

	protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
		GroovyTemplateEndpoint answer = new GroovyTemplateEndpoint(uri, this, remaining);
		setProperties(answer, parameters);
		// if its a http resource then append any remaining parameters and update the resource uri
		if (ResourceHelper.isHttpUri(remaining)) {
			remaining = ResourceHelper.appendParameters(remaining, parameters);
			answer.setResourceUri(remaining);
		}
		return answer;
	}
}
