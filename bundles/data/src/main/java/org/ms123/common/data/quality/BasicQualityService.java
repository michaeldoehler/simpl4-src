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
package org.ms123.common.data.quality;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ms123.common.data.*;
import org.apache.commons.beanutils.*;
import flexjson.*;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.setting.api.SettingService;

@SuppressWarnings("unchecked")
public class BasicQualityService {

	private final String ENTITY = "entity";

	protected NucleusService m_nucleusService;

	protected SettingService m_settingService;

	protected DataLayer m_dataLayer;

	private Map<String, QualityBatch> m_batches = new HashMap();

	protected List<Map> _dupCheck(String namespace, String entityName, List<Map> candidateList, String state, String id, boolean dry) throws Exception {
		QualityBatch b = m_batches.get(namespace + "_" + entityName);
		if (b == null) {
			b = new QualityBatch(namespace, entityName, m_dataLayer, m_settingService, m_nucleusService);
			m_batches.put(namespace + "_" + entityName, b);
		}
		if (candidateList != null) {
			return b.doCheckFromData(candidateList);
		} else {
			return b.doCheckFromDb(state, id, dry);
		}
	}

	protected void debug(String message) {
		m_logger.debug(message);
	}

	protected void info(String message) {
		m_logger.info(message);
		System.out.println(message);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(QualityServiceImpl.class);
}
