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
package org.ms123.common.system.compile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;
import org.ms123.common.git.GitService;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.store.StoreDesc;
import org.apache.shiro.authz.annotation.RequiresRoles;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** CompileService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=compile" })
public class CompileServiceImpl extends BaseCompileServiceImpl implements CompileService {

	public CompileServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("CompileEventHandlerService.activate.props:" + props);
		try {
			Bundle b = bundleContext.getBundle();
			m_bundleContext = bundleContext;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void update(Map<String, Object> props) {
		info("CompileServiceImpl.updated:" + props);
	}

	protected void deactivate() throws Exception {
		info("CompileServiceImpl.deactivate");
	}


	@RequiresRoles("admin")
	public void  compileGroovyAll(
		 ) throws RpcException {
		try {
			_compileGroovyAll();
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CompileServiceImpl.groovycompile:",e);
		}
	}

	@RequiresRoles("admin")
	public void  compileJavaAll(
		 ) throws RpcException {
		try {
			_compileJavaAll();
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CompileServiceImpl.javacompile:",e);
		}
	}

	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		System.out.println("CompileServiceImpl.setGitService:" + gitService);
		this.m_gitService = gitService;
	}
}
