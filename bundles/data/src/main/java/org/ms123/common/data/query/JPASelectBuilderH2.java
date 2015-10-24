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
package org.ms123.common.data.query;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.beanutils.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.data.api.SessionContext;

@SuppressWarnings({"unchecked","deprecation"})
public class JPASelectBuilderH2 extends JPASelectBuilderPostgresql implements SelectBuilder {

	private static final Logger m_logger = LoggerFactory.getLogger(JPASelectBuilderH2.class);

	public JPASelectBuilderH2(QueryBuilder qb, StoreDesc sdesc, String entityName, List<String> joinFields, Map filters, Map fieldSets) {
		super(qb, sdesc, entityName, joinFields, filters, fieldSets);
	}

	protected String getContains(String f, String d, String dt) {
		if( "".equals(d)){
			return f +" is null or " +f + ".regexCI(\"" + d + "\")";
		}else{
			return f + ".regexCI(\"" + d + "\")";
		}
	}
}
