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
package org.ms123.common.system.hook;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.store.StoreDesc;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.camel.api.CamelService;
import org.ms123.common.git.GitService;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** HookService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=hook" })
public class HookServiceImpl extends BaseHookServiceImpl implements org.ms123.common.rpc.HookService {

	public HookServiceImpl() {
	}

	static final String[] topics = new String[] { "rpc" };

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("HookEventHandlerService.activate.props:" + props);
		try {
			Bundle b = bundleContext.getBundle();
			m_bundleContext = bundleContext;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void callHooks(Map props){
		System.out.println("handleEvent");	
		String serviceName = (String)props.get(SERVICENAME);
		String methodName = (String)props.get(METHODNAME);
		Object methodParams = props.get(METHODPARAMS);
		Object at = props.get(AT);
		Object result = props.get(METHODRESULT);
		String ns = getNamespace(methodParams);
		if( ns == null ){
			return;
		}
		List<Map> hookList = getHooks(ns);
		if( hookList == null ) return;
		for(Map<String,String> hook : hookList){
			if( at.equals(hook.get(AT)) && ns.equals(hook.get(StoreDesc.NAMESPACE)) && serviceName.equals(hook.get(SERVICENAME)) && methodName.equals(hook.get(METHODNAME))){
				String action = hook.get(ACTION);
				info("HookServiceImpl.camelAction: service:" + serviceName + ",Method:" + methodName+"/params:"+methodParams);
				camelAction( ns, action, methodParams,result);
			}
		}
	}

	public void update(Map<String, Object> props) {
		info("HookServiceImpl.updated:" + props);
	}

	protected void deactivate() throws Exception {
		info("HookServiceImpl.deactivate");
	}

	@Reference(dynamic = true, optional = true)
	public void setCamelService(CamelService paramCamelService) {
		this.m_camelService = paramCamelService;
		System.out.println("HookServiceImpl.setCamelService:" + paramCamelService);
	}

	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		System.out.println("HookServiceImpl.setGitService:" + gitService);
		this.m_gitService = gitService;
	}
	@Reference(dynamic = true, optional = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("HookServiceImpl.setPermissionService:" + paramPermissionService);
	}
}
