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
package org.ms123.common.system.thread;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.util.AbstractMap;
import javax.servlet.http.*;
import flexjson.*;
import eu.bitwalker.useragentutils.*;

/**
 *
 */
@SuppressWarnings("unchecked")
public class ThreadContext {

	private static ThreadLocal<ThreadContext> m_threadLocal = new InheritableThreadLocal<ThreadContext>();

	public static String SESSION_MANAGER = "sessionManager";

	private UserAgent userAgent;
	private String stringUserAgent;
	private String namespace;

	private String userName;
	private Map m_properties = new HashMap();
	private Date startTime;

	private ThreadContext() {
	}

	;

	public static ThreadContext getThreadContext() {
		return m_threadLocal.get();
	}

	public static void loadThreadContext(HttpServletRequest request, HttpServletResponse response) {
		String r[] = request.getPathInfo().split("/");
		String namespace = (r.length < 2) ? "RPC" : r[1];
		String username = (String) request.getAttribute("username");
		String sua = request.getHeader("user-agent");
		if( sua == null) sua = "UNKNOWN";
		UserAgent userAgent = UserAgent.parseUserAgentString(sua);
		loadThreadContext( namespace, username,userAgent,sua);
	}
	public static void loadThreadContext(String namespace,String username,UserAgent ua, String sua) {
		ThreadContext current = new ThreadContext();
		current.namespace = namespace;
		current.userName = username;
		current.userAgent = ua;
		current.stringUserAgent = sua;
		current.startTime = new Date();
		m_threadLocal.set(current);
	}
	public static void loadThreadContext(String namespace,String username,UserAgent ua) {
		ThreadContext current = new ThreadContext();
		current.namespace = namespace;
		current.userName = username;
		current.userAgent = ua;
		current.startTime = new Date();
		m_threadLocal.set(current);
	}
	public static void loadThreadContext(String namespace,String username) {
		ThreadContext current = new ThreadContext();
		current.namespace = namespace;
		current.userName = username;
		current.startTime = new Date();
		m_threadLocal.set(current);
	}

	public static void loadThreadContext(ThreadContext rc) {
		ThreadContext current = new ThreadContext();
		current.namespace = rc.getAppName();
		current.userName = rc.getUserName();
		current.userAgent = rc.getUserAgent();
		current.stringUserAgent = rc.getStringUserAgent();
		current.startTime = new Date();
		m_threadLocal.set(current);
	}

	public void remove(){
		m_threadLocal.remove();
	}
	public String getAppName() {
		return namespace;
	}
	public UserAgent getUserAgent() {
		return userAgent;
	}
	public String getStringUserAgent() {
		return stringUserAgent;
	}

	public String getUserName() {
		return userName;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void put(String key, Object value){
		m_properties.put(key,value);
	}
	public Object get(String key){
		return m_properties.get(key);
	}
	public synchronized void finalize(Throwable t){
		for( Object prop : m_properties.values()){
			if( prop instanceof ThreadFinalizer){
				((ThreadFinalizer)prop).finalize(t);
			}
		}
		m_properties=new HashMap();
	}
	/*public void setSessionManager(SessionManager sc) {
		m_sc = sc;
	}
	public SessionManager  getSessionManager() {
		return m_sc;
	}*/
}
