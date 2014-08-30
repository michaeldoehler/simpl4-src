/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ms123.common.data;

import org.junit.After
import org.junit.Before
import org.junit.Test

import groovyx.net.http.RESTClient
import groovyx.net.http.HTTPBuilder
import groovy.util.slurpersupport.GPathResult
import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT
import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.ContentType.JSON
import java.net.URLEncoder;
/**
 * Test case for TomcatPluginConvention.
 *
 * @author Benjamin Muschko
 */
class RestTest {

		def callTest(String _request,method, String _data){
			def s = _request.replaceAll( "\\{", "%7B");
			s = s.replaceAll( "\\}", "%7D");
			println("\n====> " + s + " /"+method);

			def http = new HTTPBuilder( s )
			http.headers[ 'Authorization' ] = "Basic "+ "admin:admin".getBytes().encodeBase64()

			def ret = http.request(method,JSON) { req ->
				headers.'User-Agent' = 'Mozilla/5.0'
				if( method != DELETE && method != GET ){
					body = _data
				}
				response.success = { resp, json ->
					json
				}
				response.'500' = { resp -> 
					'Error500'
				}

				response.'404' = { resp -> 
					'Error404'
				}
				response.'400' = { resp, json -> 
					json
				}
			}
			
			println("<==== " + ret);
			return ret;
		}
}
