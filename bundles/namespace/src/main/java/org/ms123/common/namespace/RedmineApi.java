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
package org.ms123.common.namespace;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Date;
import java.util.Properties;
import java.sql.*;
import java.io.*;
import java.net.*;
import flexjson.*;
import com.taskadapter.redmineapi.*;
import com.taskadapter.redmineapi.bean.*;

/**
 *
 */
@SuppressWarnings("unchecked")
class RedmineApi {

	protected JSONDeserializer m_ds = new JSONDeserializer();

	private RedmineManager m_mgr;

	/**
	 */
	public RedmineApi(String gitHost, String apikey) {
		try {
			String redmineHost = "http://" + gitHost;
			String apiAccessKey = apikey;
			m_mgr = new RedmineManager("http://"+gitHost, apikey);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int createProject(String name) throws Exception {
		return createProject(name,-1);
	}
	public int createProject(String name, int parentId) throws Exception {
		System.out.println("createProject:" + name);
		Project project = new Project();
		project.setIdentifier(name);
		project.setName(name);
		if( parentId!=-1){
			project.setParentId(parentId);
		}
		Project rp = m_mgr.createProject(project);
		int id = rp.getId();
		System.out.println("id:" + id);
		return id;
	}

	public List<Map> getProjects() throws Exception {
		List<Project> plist = m_mgr.getProjects();
		List<Map> ret = new ArrayList();
		for (Project p : plist) {
			Map pmap = new HashMap();
			pmap.put("name", p.getIdentifier());
			ret.add(pmap);
		}
		return ret;
	}

	public void deleteProject(String name) throws Exception {
		System.out.println("deleteProject:" + name);
		m_mgr.deleteProject(name);
	}

	private String getApiKey() {
		Connection conn = null;
		Statement st = null;
		try {
			DriverManager.registerDriver(new org.postgresql.Driver());
			String url = "jdbc:postgresql://localhost/redmine";
			Properties props = new Properties();
			props.setProperty("user", "postgres");
			conn = DriverManager.getConnection(url, props);
			st = conn.createStatement();
			ResultSet rs = st.executeQuery("select value from tokens where user_id=1 and action='api'");
			if (rs.next()) {
				String apikey = rs.getString(1);
				System.out.println("ApiKey:" + apikey);
				return apikey;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (st != null)
					st.close();
				if (conn != null)
					conn.close();
			} catch (Exception e2) {
			}
		}
		return null;
	}

	private String getHost() {
		String host = "localhost:3001";
		return host;
	}
}
