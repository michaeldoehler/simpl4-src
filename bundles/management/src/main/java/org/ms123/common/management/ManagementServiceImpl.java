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
package org.ms123.common.management;

import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;

/** ManagementService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=management" })
public class ManagementServiceImpl implements ManagementService {

	private static final Logger m_logger = LoggerFactory.getLogger(ManagementServiceImpl.class);

	private DataLayer m_dataLayer;

	public static String CUSTOMER_ENTITY = "customer";
	public static String UNIT_ENTITY = "unit";

	public static String MANAGEMENT_NS = "management";

	public ManagementServiceImpl() {
		m_logger.info("ManagementServiceImpl construct");
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("ManagementServiceImpl.activate.props:" + props);
		try {
			m_logger.info("ManagementServiceImpl.activate -->");
			Bundle b = bundleContext.getBundle();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void update(Map<String, Object> props) {
		System.out.println("ManagementServiceImpl.updated:" + props);
	}

	protected void deactivate() throws Exception {
		m_logger.info("deactivate");
		System.out.println("deactivate");
	}

	protected void sendEmail(String to, String from, String subject, String text) throws Exception {
		System.out.println("sendEmail:" + to);
		SimpleEmail email = new SimpleEmail();
		email.setMsg(text);
		email.addTo(to);
		email.setFrom(from);
		email.setSubject(subject);
		email.setHostName("127.0.0.1");
		email.send();
	}

	private int exec(String line, ByteArrayOutputStream outputStream, ByteArrayOutputStream outputErr, int[] exitValues) throws Exception {
		CommandLine cmdLine = CommandLine.parse(line);
		DefaultExecutor executor = new DefaultExecutor();
		executor.setExitValues(exitValues);
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, outputErr);
		executor.setStreamHandler(streamHandler);
		int exitValue = executor.execute(cmdLine);
		return exitValue;
	}

	/* BEGIN JSON-RPC-API*/
	@RequiresRoles("admin")
	public Map managementCmd(
			@PName("host")             String host, 
			@PName("cmd")              String cmd) throws RpcException {
		try {
			String line = "ssh -o StrictHostKeyChecking=no " + host + " virsh -c qemu:///system " + cmd;
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ByteArrayOutputStream outputError = new ByteArrayOutputStream();
			int exitValue = exec(line, outputStream, outputError, new int[] { 0, 1 });
			String stderr = outputError.toString();
			String stdout = outputStream.toString();
			System.out.println("startVM.management.exetValue:" + exitValue);
			System.out.println("startVM.management.stdout:" + stdout);
			System.out.println("startVM.management.stderr:" + stderr);
			Map ret = new HashMap();
			ret.put("exitValue", exitValue);
			ret.put("stdout", stdout);
			ret.put("stderr", stderr);
			return ret;
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ManagementService.managementCmd:", e);
		} finally {
		}
	}

	public String destroyUnit(
			@PName("customerId")       String customerId, 
			@PName("unitId")       String unitId, 
			@PName("name")             String name) throws RpcException {
		try {
			if (customerId == null) {
				throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ManagementService.destroyUnit:missing_customer");
			}
			System.out.println("customerId:" + customerId);
			StoreDesc sdesc = StoreDesc.getNamespaceData(MANAGEMENT_NS);
			SessionContext sc = m_dataLayer.getSessionContext(sdesc);
			Object c = sc.getObjectById(sc.getClass(CUSTOMER_ENTITY), customerId);
			System.out.println("customer:" + c);
			int network = (Integer) PropertyUtils.getProperty(c, "netnumber");
			String host = (String) PropertyUtils.getProperty(c, "hostsystem");
			System.out.println("host:" + host);
			Map ret = new HashMap();
			String line = "ssh -p2122 -o StrictHostKeyChecking=no " + host + " /opt/projects/vm-admin/destroy-vm.sh -n " + name + " -f";
			System.out.println("line:" + line);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ByteArrayOutputStream outputError = new ByteArrayOutputStream();
			int exitValue = exec(line, outputStream, outputError, new int[] { 0, 1 });
			String demoname = outputError.toString();
			String stdout = outputStream.toString();
			System.out.println("createUnit.create-vm.exetValue:" + exitValue);
			System.out.println("createUnit.create-vm.stdout:" + stdout);
			m_dataLayer.deleteObject(null, sdesc, "unit", unitId);
			return "SWunit destroyed:" + name;
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ManagementService.destroyUnit:", e);
		} finally {
		}
	}

	private boolean checkName(SessionContext sc, String name){
			Object c = sc.getObjectByAttr(sc.getClass(UNIT_ENTITY), "name", name);
			if( c != null){
				return false;
			}
			return true;
	}

	public String createUnit(
			@PName("customerId")       String customerId, 
			@PName("data")             Map data) throws RpcException {
		try {
			if (customerId == null) {
				throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ManagementService.createUnit:missing_customer");
			}
			String name = (String) data.get("name");
			System.out.println("customerId:" + customerId);
			StoreDesc sdesc = StoreDesc.getNamespaceData(MANAGEMENT_NS);
			SessionContext sc = m_dataLayer.getSessionContext(sdesc);
			
			if( !checkName(sc, name) ){
				throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ManagementService.createUnit:name_exists:"+name);
			}

			Object c = sc.getObjectById(sc.getClass(CUSTOMER_ENTITY), customerId);
			System.out.println("customer:" + c);
			int network = (Integer) PropertyUtils.getProperty(c, "netnumber");
			String host = (String) PropertyUtils.getProperty(c, "hostsystem");
			System.out.println("host:" + host);
			Map ret = new HashMap();
			String githost = (String) data.get("githost");
			String swusage = (String) data.get("usage");
			String line = "ssh -p2122 -o StrictHostKeyChecking=no " + host + " /opt/projects/vm-admin/create-vm.sh -u "+ swusage+" -g " + githost + " -n " + name + " -w 2 -b sw-base-debian -t qemu -o linux -y";
			System.out.println("line:" + line);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ByteArrayOutputStream outputError = new ByteArrayOutputStream();
			int exitValue = exec(line, outputStream, outputError, new int[] { 0, 1 });
			String demoname = outputError.toString();
			String stdout = outputStream.toString();
			System.out.println("createUnit.create-vm.exetValue:" + exitValue);
			System.out.println("createUnit.create-vm.stdout:" + stdout);

			m_dataLayer.insertObject(data, sdesc, "unit_list", "customer", customerId);
			return "http://" + name + ".osshosting.org/sw/start.html";
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ManagementService.createUnit:", e);
		} finally {
		}
	}

	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		System.out.println("ManagementServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
	}
}
