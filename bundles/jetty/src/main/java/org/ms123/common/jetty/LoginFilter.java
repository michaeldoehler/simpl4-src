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
import java.util.*;
import java.sql.Timestamp;
import com.Ostermiller.util.*;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.system.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginFilter implements Filter {

	private FilterConfig config = null;

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
			namespace = arr[1];
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
				req.setAttribute("username", username);
			}
		}
		boolean ok = 
				(arr.length>3 && "sw".equals(arr[1]) && "website".equals(arr[3])) ||
				pathInfo.startsWith("/" + namespace + "/common") || 
				pathInfo.startsWith("/robots.txt") || 
				pathInfo.startsWith("/" + namespace + "/website") || 
				(pathInfo.startsWith("/" + namespace + "/") && pathInfo.endsWith(".html")) ||
				(pathInfo.startsWith("/" + namespace + "/") && pathInfo.endsWith(".html.gz")) ||
				(pathInfo.startsWith("/" + namespace + "/") && pathInfo.endsWith(".js")) ||
				(pathInfo.startsWith("/" + namespace + "/") && pathInfo.endsWith(".js.gz")) ||
				(pathInfo.startsWith("/" + namespace + "/") && pathInfo.endsWith(".css")) ||
				(pathInfo.startsWith("/" + namespace + "/") && pathInfo.endsWith(".css.gz")) ||
				(pathInfo.startsWith("/" + namespace + "/") && pathInfo.endsWith(".adoc")) ||
				(pathInfo.startsWith("/" + namespace + "/") && pathInfo.endsWith(".adoc.gz")) ||
				(pathInfo.startsWith("/" + namespace + "/") && pathInfo.endsWith(".png")) ||
				(pathInfo.startsWith("/" + namespace + "/") && pathInfo.endsWith(".jpg")) ||
				(pathInfo.startsWith("/" + namespace + "/") && pathInfo.endsWith(".gif")) ||
				(pathInfo.startsWith("/" + namespace + "/") && pathInfo.endsWith(".svg")) ||
				(pathInfo.startsWith("/" + namespace + "/") && pathInfo.endsWith(".jpeg")) ||
				pathInfo.startsWith("/" + namespace + "/dav") || 
				pathInfo.startsWith("/" + namespace + "/legacy") || 
				pathInfo.startsWith("/" + namespace + "/resource");
		if (!ok) {
			String origin = request.getHeader("Origin");
			String method = request.getMethod();
			debug("Origin:" + origin + "," + method+"|pi:"+pathInfo);
			if (origin != null && method != null && "options".equals(method.toLowerCase())) {
				ok = true;
			}
		}
		info(pathInfo + ";" + credentials+"/ok:"+ok);
		if (ok || checkCredentials(namespace, credentials, false)) {
			info(">>>> OK");
			RequestMapper rm = new RequestMapper(request);
			ThreadContext.loadThreadContext(rm, response);
			info(request.getPathInfo() + "|" + request.getMethod()+"|Uri:"+request.getRequestURI());
			info(request.getRequestURL() + "|Url:" + request.getServletPath()+"|QS:"+request.getQueryString());
			chain.doFilter(rm, response);
			info(">>>> End FILTER:"+ThreadContext.getThreadContext().get(ThreadContext.SESSION_MANAGER)+"/"+new Date().getTime());
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
	 public void displayInfo( String where, Date startTime ) {
    long time = new Date().getTime() - startTime.getTime();
    long fm = Runtime.getRuntime().freeMemory() / ( 1024 * 1024 );
    long tm = Runtime.getRuntime().totalMemory() / ( 1024 * 1024 );
    info( "Memory(" + where + "):free=" + fm + "M,total=" + tm + "M,time:" + time +" mSec");
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
