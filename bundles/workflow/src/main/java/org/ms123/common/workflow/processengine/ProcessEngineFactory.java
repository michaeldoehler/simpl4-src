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
package org.ms123.common.workflow.processengine;


import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.ProcessEngineImpl;
import org.osgi.framework.Bundle;
import org.ms123.common.libhelper.ClassLoaderWrapper;
import org.ms123.common.libhelper.BundleDelegatingClassLoader;


/**
 * @author Guillaume Nodet
 */
public class ProcessEngineFactory {

	protected ProcessEngineConfiguration processEngineConfiguration;
	protected Bundle bundle;

	protected ProcessEngineImpl processEngine;

	public void init(ClassLoader fsClassLoader) throws Exception {
		//ClassLoader previous = Thread.currentThread().getContextClassLoader();

		try {
			ClassLoader bundleClassLoader = new BundleDelegatingClassLoader(bundle,fsClassLoader);

			//Thread.currentThread().setContextClassLoader(new ClassLoaderWrapper(bundleClassLoader, fsClassLoader, ProcessEngineFactory.class.getClassLoader(), ProcessEngineConfiguration.class.getClassLoader(), previous));

			processEngineConfiguration.setClassLoader(bundleClassLoader);

			processEngine = (ProcessEngineImpl) processEngineConfiguration.buildProcessEngine();

		} finally {
		//	Thread.currentThread().setContextClassLoader(previous);
		}
	}

	public void destroy() throws Exception {
		if (processEngine != null) {
			processEngine.close();
		}
	}

	public void setFsClassLoader(ClassLoader fsClassLoader){
		ClassLoader bundleClassLoader = new BundleDelegatingClassLoader(bundle,fsClassLoader);
		processEngineConfiguration.setClassLoader(bundleClassLoader);
	}
	public ProcessEngine getObject() throws Exception {
		return processEngine;
	}

	public ProcessEngineConfiguration getProcessEngineConfiguration() {
		return processEngineConfiguration;
	}

	public void setProcessEngineConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
		this.processEngineConfiguration = processEngineConfiguration;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}
}
