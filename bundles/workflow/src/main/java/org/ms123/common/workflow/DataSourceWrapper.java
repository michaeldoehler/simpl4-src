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
package org.ms123.common.workflow;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;

/**
 */
public class DataSourceWrapper implements DataSource {
	private DataSource dataSource;

	List<Connection> m_connList = new ArrayList();
	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	public void destroy(){
		try{
			for(Connection c : m_connList){
				System.out.println("DataSourceWrapper.destroy:"+c+"/"+c.isClosed());
				c.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	@Override
	public Connection getConnection() throws SQLException {
		Connection c = dataSource.getConnection();
		m_connList.add(c);
		return c;
	}

	public Connection getConnection(String username, String password) throws SQLException {
		Connection c = dataSource.getConnection(username, password);
		return c;
	}

	public PrintWriter getLogWriter() throws SQLException {
		return dataSource.getLogWriter();
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException{
		return dataSource.getParentLogger();
	}

	public int getLoginTimeout() throws SQLException {
		return dataSource.getLoginTimeout();
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return dataSource.isWrapperFor(iface);
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		dataSource.setLogWriter(out);
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		dataSource.setLoginTimeout(seconds);
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		return dataSource.unwrap(iface);
	}
}
