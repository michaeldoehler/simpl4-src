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
package org.ms123.common.cassandra;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.*;
import flexjson.*;
import java.io.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.cassandra.service.CassandraDaemon.Server;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.permission.api.PermissionException;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultLong;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** CassandraService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=cassandra" })
public class CassandraServiceImpl extends BaseCassandraServiceImpl implements CassandraService {

	private JSONDeserializer m_ds = new JSONDeserializer();
	private JSONSerializer m_js = new JSONSerializer();

	private static final String INTERNAL_CASSANDRA_KEYSPACE = "system";
	private static final String INTERNAL_CASSANDRA_AUTH_KEYSPACE = "system_auth";
	private static final String INTERNAL_CASSANDRA_TRACES_KEYSPACE = "system_traces";

	private CassandraDaemon cassandraDaemon;

	private String cassandraConfig;

	public CassandraServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		cassandraConfig = new File(System.getProperty("etc.dir"), "cassandra.yaml").toString();
		start();
	}

	protected void deactivate() throws Exception {
	}

	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
	}

	public boolean isRunning() {
		if (cassandraDaemon == null)
			return false;
		return (cassandraDaemon.nativeServer != null && cassandraDaemon.nativeServer.isRunning()) || (cassandraDaemon.thriftServer != null && cassandraDaemon.thriftServer.isRunning());
	}

	public void start() {
		info("starting Cassandra in Embedded mode");
		if (cassandraConfig != null) {
			System.setProperty("cassandra.config", "file://" + cassandraConfig);
		}
		System.setProperty("cassandra-foreground", "false");
		cassandraDaemon = new CassandraDaemon();
		try {
			info("initializing cassandra deamon");
			cassandraDaemon.init(null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		info("starting cassandra deamon");
		cassandraDaemon.start();
		info("cassandra up and runnign");
	}

	public void stop() {
		info("Stopping cassandra deamon");
		info("cleaning up the Schema keys");
		Schema.instance.clear();
		info("stopping cassandra");
		cassandraDaemon.stop();
		info("destroying the cassandra deamon");
		cassandraDaemon.destroy();
		info("cassandra is removed");
		cassandraDaemon = null;
		info("removing MBean");
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			mbs.unregisterMBean(new ObjectName("org.apache.cassandra.db:type=DynamicEndpointSnitch"));
		} catch (Exception e) {
			//} catch (MBeanRegistrationException | InstanceNotFoundException | MalformedObjectNameException e) {
			info("Couldn't remove MBean");
		}
	}

	public void cleanUp() {
		if (isRunning()) {
			dropKeyspaces();
		}
	}

	private void dropKeyspaces() {
		String host = DatabaseDescriptor.getRpcAddress().getHostName();
		int port = DatabaseDescriptor.getRpcPort();
		debug("Cleaning cassandra keyspaces on " + host + ":" + port);
		Cluster cluster = HFactory.getOrCreateCluster("TestCluster", new CassandraHostConfigurator(host + ":" + port));
		/* get all keyspace */
		List<KeyspaceDefinition> keyspaces = cluster.describeKeyspaces();
		/* drop all keyspace except internal cassandra keyspace */
		for (KeyspaceDefinition keyspaceDefinition : keyspaces) {
			String keyspaceName = keyspaceDefinition.getName();
			if (!INTERNAL_CASSANDRA_KEYSPACE.equals(keyspaceName) && !INTERNAL_CASSANDRA_AUTH_KEYSPACE.equals(keyspaceName) && !INTERNAL_CASSANDRA_TRACES_KEYSPACE.equals(keyspaceName)) {
				cluster.dropKeyspace(keyspaceName);
			}
		}
	}
}
