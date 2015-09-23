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
package org.ms123.common.workflow.tasks;

import java.util.*;
import java.io.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.el.Expression;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.scripting.ScriptingEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.ms123.common.docbook.DocbookService;
import org.apache.commons.beanutils.*;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricProcessInstance;
import flexjson.*;

@SuppressWarnings("unchecked")
public class TaskDocumentExecutor extends TaskBaseExecutor implements JavaDelegate {

	protected JSONDeserializer m_ds = new JSONDeserializer();

	private Expression documentname;
	private Expression filename;

	private Expression variablesmapping;

	@Override
	public void execute(DelegateExecution execution) {
		TaskContext tc = new TaskContext();
		tc.setExecution(execution);
		showVariablenNames(tc);
		setCategory(tc);
		DocbookService ds = getDocbookService(execution);
		String namespace = tc.getCategory();
		String fn = getName(documentname.getValue(execution).toString());
		List ret = null;
		try {
			Map<String, Object> vars = new HashMap(execution.getVariables());
			Map<String, Object> fparams = getParams(execution, variablesmapping, "documentvar");
			vars.putAll(fparams);
			System.out.println("TaskDocumentExecutor,fparams:" + fparams);
			System.out.println("TaskDocumentExecutor,filename:" + filename);
			if (filename == null || filename.getValue(execution) == null) {
				throw new RuntimeException("TaskDocumentExecutor.execute:Filename is null");
			}
			OutputStream os = new FileOutputStream(new File(getProcessDocBasedir(execution), filename.getValue(
					execution).toString()));
			String json = getFileContentFromGit(execution, namespace, fn, "sw.document");
			ds.jsonToDocbookPdf(namespace, json, vars, os);
			os.close();
		} catch (Exception e) {
			throw new RuntimeException("TaskDocumentExecutor.execute:", e);
		} finally {
		}
	}

	private String getName(String s) {
		if (s == null) {
			throw new RuntimeException("TaskDocumentExecutor.documentname is null");
		}
		if (s.indexOf(",") == -1)
			return s;
		return s.split(",")[1];
	}
}

