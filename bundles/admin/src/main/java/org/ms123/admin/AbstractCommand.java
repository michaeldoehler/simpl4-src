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
package org.ms123.admin;


import java.net.*;
import java.util.*;
import java.io.*;
import org.ms123.common.libhelper.Base64;
import com.beust.jcommander.*;
import flexjson.*;


public abstract class AbstractCommand {
	@Parameter(names = "-help", help = true)
	public boolean help;

	public abstract void execute();

	protected void remoteCall(Map postBodyMap) {
		try {
			String postBody = toJson(postBodyMap);
			URL url = new URL(getUrlString());
			System.out.println("===>>>>Calling url:"+url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			setBasicAuth(conn);
			setAdditionalHeaders(conn);
	
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);

			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.writeBytes(postBody);
			wr.flush();
			wr.close();

			System.out.println("code:" + conn.getResponseCode());
			if (conn.getResponseCode() != 200) {
				throw new IOException(conn.getResponseMessage());
			}

			// Buffer the result into a string
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = rd.readLine()) != null) {
				System.out.println(line);
			}
			rd.close();

			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	protected void setAdditionalHeaders( HttpURLConnection conn ){
	}

	protected void setBasicAuth( HttpURLConnection conn ){
		String credentials = "admin:admin";
		String encodedAuthorization = Base64.encodeBytes(credentials.getBytes());
		conn.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
	}

	protected String getUrlString(){
		String host = System.getProperty("SWHOST");
		if (host == null) {
			host = "localhost";
		}
		String urlStr = "http://" + host + "/rpc/xyz?pretty";
		return urlStr;
	}

	private String toJson(Object obj) {
		JSONSerializer serializer = new JSONSerializer();
		serializer.prettyPrint(true);
		String jsonObject = serializer.deepSerialize(obj);
		System.out.println("jsonObject:" + jsonObject);
		return jsonObject;
	}
}

