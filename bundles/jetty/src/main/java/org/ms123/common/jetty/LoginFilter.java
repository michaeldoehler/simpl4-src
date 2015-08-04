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
package org.ms123.common.jetty;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Enumeration;
import java.sql.Timestamp;
import com.Ostermiller.util.*;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.system.thread.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import flexjson.*;
import org.springframework.util.AntPathMatcher;
import static org.apache.commons.io.FileUtils.readFileToString;

public class LoginFilter implements Filter {


	private String PERMITTED_USERS = "permittedUsers";
	private String PERMITTED_ROLES = "permittedRoles";
	private String USERNAME = "username";
	private FilterConfig config = null;
	private JSONDeserializer ds = new JSONDeserializer();

	private PermissionService m_permissionService;

	public LoginFilter(PermissionService ss) {
		m_permissionService = ss;
	}

	public void init(FilterConfig config) throws ServletException {
		this.config = config;
	}

	public void destroy() {
		config = null;
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		long before = System.currentTimeMillis();
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		String pathInfo = request.getPathInfo();
		infob("\n\n<==========================================> ");
		info("doFilter -> " + pathInfo);
		infob("date: " + new Date());
		Enumeration headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = (String) headerNames.nextElement();
			debug("\t" + headerName + " = " + request.getHeader(headerName));
		}
		if (pathInfo == null) {
			info("==== NOK:pathInfo is null");
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			infob("<##############################################\n\n");
			return;
		}
		String[] arr = pathInfo.split("/");
		String namespace = null;
		if (arr.length < 2) {
			namespace = "RPC";
		} else {
			if (arr[1].equals("repo")) {
				namespace = arr[2];
			} else {
				namespace = arr[1];
			}
		}
		for (int i = 0; i < arr.length; i++) {
			debug("\tpathinfo[" + i + "]:" + arr[i]);
		}
		debug("\tNamespace:" + namespace);
		if (pathInfo.indexOf("/checkcredentials") != -1) {
			String cred = request.getParameter("credentials");
			if (checkCredentials(namespace, cred, true)) {
				response.setStatus(HttpServletResponse.SC_OK);
			} else {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			}
			infob("<##############################################\n\n");
			return;
		}
		String auth = request.getHeader("Authorization");
		debug("auth:" + auth);
		String credentials = null;
		if (auth != null || request.getParameter("credentials") != null) {
			String a = null;
			if (auth != null && auth.toLowerCase().startsWith("basic ")) {
				a = auth.trim().split(" ")[1];
			} else {
				a = request.getParameter("credentials");
			}
			credentials = Base64.decode(a);
			int ind = credentials.indexOf(":");
			if (ind != -1) {
				String username = credentials.substring(0, ind);
				req.setAttribute(USERNAME, username);
			}
		}
		String firstSegment = arr[1];
		String method = request.getMethod();
		boolean isStaticResource = false;
		boolean ok = false;
		if (method != null && "get".equals(method.toLowerCase())) {
			if (arr.length > 3 && "sw".equals(arr[1]) && "website".equals(arr[3])) {
				ok = true;
			} else if ("sw".equals(firstSegment) || "repo".equals(firstSegment)) {
				if (isStaticResource(pathInfo)) {
					ok = true;
					isStaticResource = true;
				}
			} else if (pathInfo.equals("/robots.txt")) {
				ok = true;
			} else if (pathInfo.startsWith("/openfire")) {
				ok = true;
			} else if (pathInfo.startsWith("/sw/common") || pathInfo.startsWith("/sw/website") || pathInfo.startsWith("/sw/legacy") || pathInfo.startsWith("/sw/resource")) {
				ok = true;
			}
		} else if (request.getHeader("Origin") != null && method != null && "options".equals(method.toLowerCase())) {
			ok = true;
		}

		if (request.getParameter("rpc") != null || request.getRequestURI().startsWith("/rpc")) {
			ok = false;
		}
		if (request.getParameter("ws") != null || request.getRequestURI().startsWith("/ws")) {
			ok = false;
		}

		Map<String, Object> accessRule = null;
		if (isStaticResource && isRepoRequest(pathInfo)) {
			List<Map<String, Object>> rules = getAccessRules(namespace);
			accessRule = getMatchingAccessRule(namespace, pathInfo, rules);
			if (accessRule != null) {
				ok = false;
			}
		}

		info(pathInfo + ";" + credentials + "/ok:" + ok + "/accessRule:"+accessRule);
		if (ok || checkCredentials(namespace, credentials, false)) {
			if (accessRule != null) {
				String username = (String) req.getAttribute(USERNAME);
				if (!isPermitted(username, getUserRoles(username), (List) accessRule.get(PERMITTED_USERS), (List) accessRule.get(PERMITTED_ROLES))) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					return;
				}
			}
			info(">>>> OK," + Thread.currentThread().getName());
			RequestMapper rm = new RequestMapper(request);
			ThreadContext.loadThreadContext(rm, response);
			info(request.getPathInfo() + "|" + request.getMethod() + "|Uri:" + request.getRequestURI());
			info(request.getRequestURL() + "|Url:" + request.getServletPath() + "|QS:" + request.getQueryString());
			chain.doFilter(rm, response);
			info(">>>> End FILTER:" + ThreadContext.getThreadContext().get(ThreadContext.SESSION_MANAGER) + "/"
					+ new Date().getTime());
			Date startTime = ThreadContext.getThreadContext().getStartTime();
			ThreadContext.getThreadContext().finalize(null);
			ThreadContext.getThreadContext().remove();
			displayInfo("Finish", startTime);
		} else {
			info("==== NOK");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		}
		infob("<##############################################\n\n");
	}

	private boolean checkCredentials(String namespace, String cred, boolean noAuthAllowed) {
		if (noAuthAllowed && noAuth()) {
			return true;
		}
		if (cred == null || "".equals(cred)) {
			return false;
		}
		String[] credentials = cred.split("[:/]");
		if (credentials.length != 2) {
			info("Wrong credentials:" + cred);
			return false;
		}
		try {
			return m_permissionService.login(namespace, credentials[0], credentials[1]);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private List<String> getUserRoles(String userName) {
		List<String> userRoleList = null;
		try {
			userRoleList = m_permissionService.getUserRoles(userName);
		} catch (Exception e) {
			userRoleList = new ArrayList();
		}
		return userRoleList;
	}

	private boolean isPermitted(String userName, List<String> userRoleList, List<String> permittedUserList, List<String> permittedRoleList) {
		info("UserRoleList:" + userRoleList);
		info("PermittedUserList:" + permittedUserList);
		if (permittedUserList != null && permittedUserList.contains(userName)) {
			info("userName(" + userName + ") is allowed:" + permittedUserList);
			return true;
		}
		info("permittedRoleList:" + permittedRoleList);
		for (String userRole : userRoleList) {
			if (permittedRoleList != null && permittedRoleList.contains(userRole)) {
				info("userRole(" + userRole + ") is allowed:" + permittedRoleList);
				return true;
			}
		}
		return false;
	}

	private boolean isRepoRequest(String pathInfo) {
		return pathInfo.startsWith("/repo");
	}

	private List<Map<String, Object>> getAccessRules(String namespace) {
		File file = new File(System.getProperty("git.repos") + "/" + namespace + "/.etc/access-rules.json");
		String content = null;
		try {
			content = readFileToString(file);
		} catch (Exception e) {
			info("LoginFilter.getAccessRules:" + e.getMessage());
			return null;
		}
		return (List) this.ds.deserialize(content);
	}

	private Map<String, Object> getMatchingAccessRule(String ns, String pathInfo, List<Map<String, Object>> rules) {
		if (rules == null) {
			return null;
		}
		String resPath = pathInfo.substring(("/repo/" + ns + "/").length());
		AntPathMatcher a = new AntPathMatcher();
		for (Map<String, Object> rule : rules) {
			String pat = (String) rule.get("pattern");
			boolean res = a.match(pat, resPath);
			info("\tmatch:" + pat + " -> " + res);
			if (res) {
				return rule;
			}
		}
		return null;
	}

	private boolean noAuth() {
		String sh = System.getProperty("workspace");
		try {
			String basedir = new File(sh).getCanonicalFile().getParent();
			info("NOAUTH:" + new File(basedir, "noauth").exists());
			return new File(basedir, "noauth").exists();
		} catch (Exception e) {
		}
		return false;
	}

	private boolean isStaticResource(String pathInfo) {
		if (pathInfo.endsWith(".html") || pathInfo.endsWith(".html.gz") || pathInfo.endsWith(".js")
				|| pathInfo.endsWith(".js.gz") || pathInfo.endsWith(".css") || pathInfo.endsWith(".css.gz")
				|| pathInfo.endsWith(".adoc") || pathInfo.endsWith(".adoc.gz") || pathInfo.endsWith(".json")
				|| pathInfo.endsWith(".json.gz") || pathInfo.endsWith(".yaml") || pathInfo.endsWith(".yml")
				|| pathInfo.endsWith(".png") || pathInfo.endsWith(".jpg") || pathInfo.endsWith(".gif")
				|| pathInfo.endsWith(".odt") || pathInfo.endsWith(".svg") || pathInfo.endsWith(".xml")
				|| pathInfo.endsWith(".jpeg") || pathInfo.endsWith(".txt") || pathInfo.endsWith(".woff")
				|| pathInfo.endsWith(".woff.gz")) {
			return true;
		}
		return false;
	}

	public void displayInfo(String where, Date startTime) {
		long time = new Date().getTime() - startTime.getTime();
		long fm = Runtime.getRuntime().freeMemory() / (1024 * 1024);
		long tm = Runtime.getRuntime().totalMemory() / (1024 * 1024);
		info("Memory(" + where + "):free=" + fm + "M,total=" + tm + "M,time:" + time + " mSec");
	}

	protected void debug(String msg) {
		m_logger.debug(msg);
	}

	private void infob(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	protected void info(String msg) {
		System.out.println("LoginFilter:" + msg);
		m_logger.info(msg);
	}

	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(LoginFilter.class);
}

