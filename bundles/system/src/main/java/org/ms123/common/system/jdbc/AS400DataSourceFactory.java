/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ms123.common.system.jdbc;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.CommonDataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.osgi.service.jdbc.DataSourceFactory;

public class AS400DataSourceFactory implements DataSourceFactory {

    private static final String AS400_DATASOURCE_CLASS = "com.ibm.as400.access.AS400JDBCDataSource";
    private static final String AS400_XA_DATASOURCE_CLASS = "com.ibm.as400.access.AS400JDBCXADataSource";
    private static final String AS400_CONNECTIONPOOL_DATASOURCE_CLASS = "com.ibm.as400.access.AS400JDBCConnectionPoolDataSource";
    private static final String AS400_DRIVER_CLASS = "com.ibm.as400.access.AS400JDBCDriver";
    private final Class<?> as400DataSourceClass;
    private final Class<?> as400ConnectionPoolDataSourceClass;
    private final Class<?> as400XaDataSourceClass;
    private final Class<?> as400DriverClass;

    public AS400DataSourceFactory() throws ClassNotFoundException {
        ClassLoader classLoader = AS400DataSourceFactory.class.getClassLoader();
        this.as400DataSourceClass = classLoader.loadClass(AS400_DATASOURCE_CLASS);
        this.as400ConnectionPoolDataSourceClass = classLoader.loadClass(AS400_CONNECTIONPOOL_DATASOURCE_CLASS);
        this.as400XaDataSourceClass = classLoader.loadClass(AS400_XA_DATASOURCE_CLASS);
        this.as400DriverClass = classLoader.loadClass(AS400_DRIVER_CLASS);
    }


    private void setProperties(CommonDataSource ds, Class<?> clazz, Properties properties) throws Exception {
				System.out.println("As400.setProperties:"+properties);
        Properties props = (Properties) properties.clone();

        String databaseName = (String) props.remove(DataSourceFactory.JDBC_DATABASE_NAME);
        if (databaseName == null) {
            throw new SQLException("missing required property " + DataSourceFactory.JDBC_DATABASE_NAME);
        }
        clazz.getMethod("setDatabaseName", String.class).invoke(ds, databaseName);


        String serverName = (String) props.remove(DataSourceFactory.JDBC_SERVER_NAME);
        clazz.getMethod("setServerName", String.class).invoke(ds, serverName);


        String user = (String) props.remove(DataSourceFactory.JDBC_USER);
        clazz.getMethod("setUser", String.class).invoke(ds, user);

        String password = (String) props.remove(DataSourceFactory.JDBC_PASSWORD);
        clazz.getMethod("setPassword", String.class).invoke(ds, password);

        clazz.getMethod("setPrompt", boolean.class).invoke(ds, false);
        if (!props.isEmpty()) {
            //throw new SQLException("cannot set properties " + props.keySet());
						System.out.println("As400.setProperties:not empty:"+props);
        }
    }

    @Override
    public DataSource createDataSource(Properties props) throws SQLException {
        try {
            DataSource ds = DataSource.class.cast(as400DataSourceClass.newInstance());
            setProperties(ds, as400DataSourceClass, props);
						System.out.println("as400.createDataSource:"+ds.getClass());
            return ds;
        }
        catch (Exception ex) {
            throw new SQLException(ex);
        }
    }

    @Override
    public ConnectionPoolDataSource createConnectionPoolDataSource(Properties props) throws SQLException {
        try {
            ConnectionPoolDataSource ds = ConnectionPoolDataSource.class.cast(as400ConnectionPoolDataSourceClass.newInstance());
            setProperties(ds, as400XaDataSourceClass, props);
						System.out.println("as400.createPooledDataSource:"+ds);
            return ds;
        }
        catch (Exception ex) {
            throw new SQLException(ex);
        }
    }

    @Override
    public XADataSource createXADataSource(Properties props) throws SQLException {
        try {
            XADataSource ds = XADataSource.class.cast(as400XaDataSourceClass.newInstance());
            setProperties(ds, as400XaDataSourceClass, props);
						System.out.println("as400.createXADataSource:"+ds);
            return ds;
        }
        catch (Exception ex) {
            throw new SQLException(ex);
        }
    }

    @Override
    public Driver createDriver(Properties props) throws SQLException {
        try {
					Driver driver = Driver.class.cast(as400DriverClass.newInstance());
					System.out.println("as400.createDriver:"+driver);
            return driver;
        }
        catch (InstantiationException ex) {
            throw new SQLException(ex);
        }
        catch (IllegalAccessException ex) {
            throw new SQLException(ex);
        }
    }
}
