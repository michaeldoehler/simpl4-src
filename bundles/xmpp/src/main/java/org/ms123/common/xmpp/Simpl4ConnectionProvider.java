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

import java.sql.Connection;
import java.sql.SQLException;
import java.io.File;
import java.util.Properties;
import javax.sql.DataSource;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jivesoftware.database.ConnectionProvider;
import bitronix.tm.resource.jdbc.PoolingDataSource;

/**
 */
public class Simpl4ConnectionProvider implements ConnectionProvider {

	private static final Logger Log = LoggerFactory.getLogger(Simpl4ConnectionProvider.class);

	private PoolingDataSource m_poolingDataSource;

	public Simpl4ConnectionProvider() {
	}

	public boolean isPooled() {
		return true;
	}

	public String getBaseDir() {
		return new File(System.getProperty("workspace"), "openfire").toString();
	}

	public void start() {
		try {
			m_poolingDataSource = getPoolingDataSource();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void restart() {
		destroy();
		start();
	}

	public Connection getConnection() throws SQLException {
		if (m_poolingDataSource == null) {
			throw new SQLException("Simpl4ConnectionProvider.DataSource has not been initialized.");
		}
		return m_poolingDataSource.getConnection();
	}

	private PoolingDataSource getPoolingDataSource() {
		PoolingDataSource ds = new PoolingDataSource();
		ds.setClassName("org.h2.jdbcx.JdbcDataSource");
		ds.setUniqueName("openfire");
		ds.setMaxPoolSize(15);
		ds.setAllowLocalTransactions(true);
		ds.setTestQuery("SELECT 1");
		ds.getDriverProperties().setProperty("user", "sa");
		ds.getDriverProperties().setProperty("password", "sa");
		ds.getDriverProperties().setProperty("URL", "jdbc:h2:file:" + getBaseDir() + "/dbh2;TRACE_LEVEL_FILE=2;TRACE_LEVEL_SYSTEM_OUT=1;MV_STORE=TRUE;MVCC=TRUE;CACHE_SIZE=33107;DB_CLOSE_ON_EXIT=FALSE");
		return ds;
	}

	public void destroy() {
		try {
			if (m_poolingDataSource != null) {
				m_poolingDataSource.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
