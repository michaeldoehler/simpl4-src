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
package org.ms123.common.system.registry;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import com.datastax.driver.core.Session;
import com.noorq.casser.core.Casser;
import com.noorq.casser.core.CasserSession;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.cassandra.CassandraService;
import org.ms123.common.git.GitService;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.store.StoreDesc;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import static com.noorq.casser.core.Query.eq;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** RegistryService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=registry" })
public class RegistryServiceImpl extends BaseRegistryServiceImpl implements RegistryService {

	private CasserSession m_session;
	private Registry registry;

	private String GLOBAL_KEYSPACE = "global";

	public RegistryServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("RegistryEventHandlerService.activate.props:" + props);
		try {
			Bundle b = bundleContext.getBundle();
			m_bundleContext = bundleContext;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void update(Map<String, Object> props) {
		info("RegistryServiceImpl.updated:" + props);
	}

	protected void deactivate() throws Exception {
		info("RegistryServiceImpl.deactivate");
	}

	@RequiresRoles("admin")
	public void  upsert(
		@PName("key") String key, 
		@PName("value") String value) throws RpcException {
		if( m_session == null) initRegistry();
		m_session.upsert() .value(registry::key, key) .value(registry::value, value) .sync();
	}

	@RequiresRoles("admin")
	public String  get(
		@PName("key") String key ) throws RpcException {
		if( m_session == null) initRegistry();
		String value = m_session.select(registry::value).where(registry::key, eq(key)).sync().findFirst().get()._1;
		info("get.value("+key+"):"+value);
		return value;
	}

	@RequiresRoles("admin")
		public void  delete(
		@PName("key") String key ) throws RpcException {
		if( m_session == null) initRegistry();
		m_session.delete().where(registry::key, eq(key)).sync();
	}

	private void initRegistry() {
		Session session = m_cassandraService.getSession(GLOBAL_KEYSPACE);
		registry = Casser.dsl(Registry.class);
		m_session = Casser.init(session).showCql().add(registry).autoCreateDrop().get();
		info("registry:" + registry);
	}

	@Reference(dynamic = true, optional = true)
	public void setCassandraService(CassandraService cassandraService) {
		System.out.println("RegistryServiceImpl.setCassandraService:" + cassandraService);
		this.m_cassandraService = cassandraService;
	}
}

