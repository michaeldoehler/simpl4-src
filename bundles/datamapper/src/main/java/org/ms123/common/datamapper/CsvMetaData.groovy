/*
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
package org.ms123.common.datamapper;
import flexjson.JSONSerializer;
import java.util.*;
import java.io.*;
import flexjson.JSONSerializer;
import java.math.BigInteger;
import  au.com.bytecode.opencsv.*;

public final class CsvMetaData implements MetaData,Constants {

	protected static JSONSerializer js = new JSONSerializer();
	private Map m_config;

	public CsvMetaData(Map config) {
		js.prettyPrint(true);
		m_config = config;
	}

	public Map generateMetadata(File stream) throws Exception {
		Reader bufferedReader = new FileReader(stream);
		return generateMetadata(bufferedReader);
	}
	public Map generateMetadata(Reader reader) throws Exception {
		Map rootMap  = new HashMap();
		rootMap.put(NODENAME,CSV_RECORD);
		if( m_config.side == "input"){
			rootMap.put(NODETYPE,NODETYPE_ELEMENT);
		}else{
			rootMap.put(NODETYPE,NODETYPE_COLLECTION);
		}
		rootMap.put(ROOT,true);
		rootMap.put(FORMAT,FORMAT_CSV);
		rootMap.put(CSV_DELIM,m_config.columnDelim);
		rootMap.put(CSV_QUOTE,m_config.quote);
		rootMap.put(CSV_HEADER,m_config.header);
		rootMap.put(CHILDREN,new ArrayList());


		boolean withHeader = m_config.header as boolean;
		char sep  = ((String)m_config.columnDelim)[0];
		char quote  = ((String)m_config.quote)[0];

		CSVReader csvReader = new CSVReader(reader, sep, quote, 0);
		String[] nextLine = csvReader.readNext();
		for( String col in nextLine){
			Map map  = new HashMap();
			map.put(NODENAME,col);
			map.put(NODETYPE,NODETYPE_ATTRIBUTE);
			map.put(FIELDTYPE,FIELDTYPE_STRING);
			map.put(CHILDREN,new ArrayList());
			rootMap.get(CHILDREN).add(map);
		}
		return rootMap;
	}
}

