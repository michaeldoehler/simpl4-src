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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.cassandra.CassandraService;
import org.ms123.common.git.GitService;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.store.StoreDesc;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** RegistryService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=registry" })
public class RegistryServiceImpl extends BaseRegistryServiceImpl implements RegistryService {

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
	public void unbind(Map<String, Object> props) {
		info("RegistryServiceImpl.updated:" + props);
	}

	protected void deactivate() throws Exception {
		info("RegistryServiceImpl.deactivate");
	}

	public void unbind() {
	}

	@RequiresRoles("admin")
	public void  createRegistry() throws RpcException {
		try {
			Registry registry = Casser.dsl(Registry.class);
			Session session = m_cassandraService.getSession("cassandra");
			CasserSession casserSession = Casser.init(session).showCql().add(registry).autoCreateDrop().get();
			info("registry:"+registry);
			casserSession.insert()
				.value(registry::key, "testkey1")
				.value(registry::value, "testvalue1")
				.sync();
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "RegistryServiceImpl.registry:",e);
		}
	}

	@Reference(dynamic = true, unbind="unbind", optional = true)
	public void setCassandraService(CassandraService cassandraService) {
		System.out.println("RegistryServiceImpl.setCassandraService:" + cassandraService);
		this.m_cassandraService = cassandraService;
	}
	@Reference(dynamic = true, unbind="unbind", optional = true)
	public void setGitService(GitService gitService) {
		System.out.println("RegistryServiceImpl.setGitService:" + gitService);
	}
}
