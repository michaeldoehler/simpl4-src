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
package org.ms123.common.system.jdbc;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import org.osgi.framework.BundleContext;
import java.util.Dictionary;
import java.util.Map;
import java.util.Hashtable;
import org.osgi.service.jdbc.DataSourceFactory;

@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=jdbc" })
public class JDBCServiceImpl  implements JDBCService {


	private BundleContext m_bundleContext;

	public JDBCServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		try {
			m_bundleContext = bundleContext;
			registerAS400();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void registerAS400() throws Exception{
		AS400DataSourceFactory dsf = new AS400DataSourceFactory();
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, "com.ibm.as400.access.AS400JDBCDriver");
		props.put(DataSourceFactory.OSGI_JDBC_DRIVER_NAME, "as400");
		System.out.println("as400.register.dsf:"+dsf);
		System.out.println("as400.register.props:"+props);
		m_bundleContext.registerService(DataSourceFactory.class.getName(), dsf, props);
	}

	public void update(Map<String, Object> props) {
	}

	protected void deactivate() throws Exception {
	}
}

