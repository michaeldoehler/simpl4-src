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
package org.ms123.common.smtp;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.metatype.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.ServletOutputStream;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.git.GitService;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultLong;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.permission.api.PermissionException;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.activiti.ActivitiService;
import org.ms123.common.utils.UtilsService;
import org.osgi.framework.BundleContext;
import org.subethamail.smtp.server.SMTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** SmtpService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=smtp" })
public class SmtpServiceImpl extends BaseSmtpServiceImpl implements SmtpService {

	private static final Logger m_logger = LoggerFactory.getLogger(SmtpServiceImpl.class);
	private SMTPServer m_smtpServer=null;
	private SWMessageHandlerFactory m_messageFactory;
	public SmtpServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		
		MDC.setContextMap(new HashMap());
		System.out.println("SmtpServiceImpl.activate.smtpServer1:" + m_smtpServer);

		if( m_smtpServer==null){
			m_messageFactory = new SWMessageHandlerFactory(m_activitiService,m_permissionService);
			m_smtpServer = new SMTPServer(m_messageFactory);
			m_smtpServer.setPort(26000);
			m_smtpServer.start();
		}
		System.out.println("SmtpServiceImpl.activate.smtpServer2:" + m_smtpServer);
	}

	protected void deactivate() throws Exception {
		System.out.println("SmtpServiceImpl.deactivate.SMTPServer:"+m_smtpServer);
		if( m_smtpServer!=null){
			m_smtpServer.stop();
			m_smtpServer=null;
		}
	}

	/* BEGIN JSON-RPC-API*/
	public void markdownToSmtpXml(
			@PName("markdown")         String markdown) throws RpcException {
		try {
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "SmtpServiceImpl.markdownToSmtpXml:", e);
		}
	}

	/* END JSON-RPC-API*/
	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		System.out.println("SmtpServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
	}

	@Reference(dynamic = true, optional = true)
	public void setActivitiService(ActivitiService activitiService) {
		System.out.println("SmtpServiceImpl.setActivitiService:" + activitiService);
		this.m_activitiService = activitiService;
	}

	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		System.out.println("SmtpServiceImpl.setGitService:" + gitService);
	}
	@Reference(dynamic = true, optional=true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("SmtpServiceImpl.setPermissionService:" + paramPermissionService);
	}

	@Reference(dynamic = true, optional=true)
	public void setUtilsService(UtilsService paramUtilsService) {
		this.m_utilsService = paramUtilsService;
		System.out.println("SmtpServiceImpl.setUtilsService:" + paramUtilsService);
	}
}
