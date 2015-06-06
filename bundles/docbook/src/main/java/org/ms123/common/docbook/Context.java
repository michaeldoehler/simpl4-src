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
package org.ms123.common.docbook;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import nu.xom.*;
import org.zkoss.zuss.Zuss;
import org.zkoss.zuss.Resolver;
import org.zkoss.zuss.Locator;
import org.zkoss.zuss.metainfo.ZussDefinition;
import org.zkoss.zuss.impl.in.Parser;
import org.zkoss.zuss.impl.out.Translator;
import org.ms123.common.docbook.xom.html5.*;
import org.ms123.common.data.api.DataLayer;
import org.osgi.framework.BundleContext;
import eu.bitwalker.useragentutils.*;


@SuppressWarnings("unchecked")
public	class Context {

	int id = 0;


	UserAgent userAgent;

	boolean isOldBrowser;

	String namespace;

	String pageName;

	boolean isStartpage;

	Map paramsIn;

	Map paramsOut;
	Map rundataMap = new HashMap();

	public Context(String namespace, String pageName, boolean isStartpage, Map paramsIn, Map paramsOut) {
		this.paramsIn = paramsIn;
		this.paramsOut = paramsOut;
		this.namespace = namespace;
		this.isStartpage = isStartpage;
		this.pageName = pageName;
		this.userAgent = org.ms123.common.system.thread.ThreadContext.getThreadContext().getUserAgent();
		System.out.println("UserAgent:" + userAgent);
		Version browserVersion = userAgent.getBrowserVersion();
		String browserName = userAgent.getBrowser().toString().toLowerCase();
		System.out.println("browserVersion:" + browserVersion);
		System.out.println("browserName:" + browserName);
		int majVersion = getMajorVersion(browserVersion);
		System.out.println("majVersion:" + majVersion);
		isOldBrowser = false;
		if (browserName.indexOf("firefox") != -1 && majVersion < 21) {
			isOldBrowser = true;
		}
		System.out.println("isFirefox:" + browserName.indexOf("firefox"));
		System.out.println("majorLower21:" + (majVersion < 21));
		System.out.println("isOldBrowser:" + isOldBrowser);
	}


	private int getMajorVersion(Version browserVersion) {
		try {
			int majVersion = Integer.parseInt(browserVersion.getMajorVersion());
			return majVersion;
		} catch (Exception e) {
		}
		return -1;
	}
	protected String getString(Map properties, String key, String def) {
		try {
			String val = (String) properties.get(key);
			if (val == null || "".equals(val.trim())) {
				return def;
			}
			return val;
		} catch (Exception e) {
			return def;
		}
	}

	String nextId() {
		id++;
		return "wsid" + id;
	}
	public void set(String name, Object obj){
		rundataMap.put(name,obj);
	}
	public Object get(String name){
		return rundataMap.get(name);
	}
}
