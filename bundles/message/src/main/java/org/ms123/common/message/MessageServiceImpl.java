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
package org.ms123.common.message;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import com.Ostermiller.util.*;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.jdo.JDOObjectNotFoundException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.data.api.SessionContext;
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
import org.ms123.common.utils.UtilsService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** MessageService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=message" })
public class MessageServiceImpl implements MessageService {

	private static final Logger m_logger = LoggerFactory.getLogger(MessageServiceImpl.class);

	private static final String MESSAGE_ENTITY = "message";

	private static final String NAME = "name";

	protected MetaData m_gitMetaData;

	private PermissionService m_permissionService;

	private UtilsService m_utilsService;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	public MessageServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("MessageServiceImpl.activate.props:" + props);
	}

	protected void deactivate() throws Exception {
	}

	/* BEGIN JSON-RPC-API*/
	@RequiresRoles("admin")
	public void createMessages(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("lang")             String lang, 
			@PName("fileMap")          @POptional Map fileMap) throws RpcException {
		try {
			System.out.println("fileMap:" + fileMap);
			List<Map> msgList = null;
			if (fileMap != null) {
				Map map = (Map) fileMap.get("importfile");
				Reader is = new FileReader(new File((String) map.get("storeLocation")));
				msgList = m_gitMetaData.parseCSV(is);
			} else {
				msgList = new ArrayList();
			}
			try {
				m_gitMetaData.saveMessages(namespace, lang, msgList);
			} catch (Exception e) {
			}
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "MessageServiceImpl.createMessage:", e);
		} finally {
		}
	}

	public List getLanguages(
			@PName(StoreDesc.NAMESPACE) String namespace) throws RpcException {
		try {
			return m_gitMetaData.getLanguages(namespace);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "MessageServiceImpl.getLanguages:", e);
		} finally {
		}
	}

	public List<Map> getMessages(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("lang")             String lang, 
			@PName("filter")           @POptional Map filter) throws RpcException {
		try {
			return m_gitMetaData.getMessages(namespace, lang, filter);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "MessageServiceImpl.getMessages:", e);
		}
	}

	public Map getMessage(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("lang")             String lang, 
			@PName("id")               String id) throws RpcException {
		try {
			return m_gitMetaData.getMessage(namespace, lang, id);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "MessageServiceImpl.getMessage:", e);
		}
	}

	public void saveMessage(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("lang")             String lang, 
			@PName("msg")              Map msg,
			@PName("overwrite")       @POptional @PDefaultBool(true) Boolean overwrite
				) throws RpcException {
		try {
			m_gitMetaData.saveMessage(namespace, lang, msg,overwrite);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "MessageServiceImpl.saveMessage:", e);
		}
	}

	public void addMessages(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("lang")             String lang, 
			@PName("msgs")              List<Map> msgs,
			@PName("overwrite")       @POptional @PDefaultBool(true) Boolean overwrite
			) throws RpcException {
		try {
			m_gitMetaData.addMessages(namespace, lang, msgs,overwrite);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "MessageServiceImpl.addMessages:", e);
		}
	}

	public void saveMessages(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("lang")             String lang, 
			@PName("msgList")          List<Map> msgList
					) throws RpcException {
		try {
			m_gitMetaData.saveMessages(namespace, lang, msgList);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "MessageServiceImpl.saveMessages:", e);
		}
	}

	public void deleteMessages(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("lang")             String lang,
			@PName("msgIds")        @POptional  List<String> msgIds
					) throws RpcException {
		try {
			m_gitMetaData.deleteMessages(namespace, lang,msgIds);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "MessageServiceImpl.deleteMessages:", e);
		}
	}

	public void deleteMessage(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("lang")             String lang, 
			@PName("id")               String id) throws RpcException {
		try {
			m_gitMetaData.deleteMessage(namespace, lang, id);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "MessageServiceImpl.deleteMessage:", e);
		}
	}

	/* END JSON-RPC-API*/
	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		System.out.println("MessageServiceImpl.setGitService:" + gitService);
		m_gitMetaData = new GitMetaDataImpl(gitService);
	}

	@Reference(dynamic = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("MessageServiceImpl.setPermissionService:" + paramPermissionService);
	}

	@Reference(dynamic = true)
	public void setUtilsService(UtilsService paramUtilsService) {
		this.m_utilsService = paramUtilsService;
		System.out.println("MessageServiceImpl.setUtilsService:" + paramUtilsService);
	}
}
