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
package org.ms123.common.xmpp;

import flexjson.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.OutputStream;
import java.io.InputStream;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.libhelper.Inflector;
import org.osgi.framework.BundleContext;
import org.ms123.common.activiti.ActivitiService;

/**
 *
 */
class BaseXmppServiceImpl {

	protected Inflector m_inflector = Inflector.getInstance();

	protected DataLayer m_dataLayer;

	protected PermissionService m_permissionService;

	protected NucleusService m_nucleusService;
	protected ActivitiService m_activitiService;

	protected UtilsService m_utilsService;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

}
