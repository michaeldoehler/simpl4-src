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


import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.activiti.engine.impl.cmd.ExecuteJobsCmd;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.jobexecutor.*;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.runtime.Job;
import org.ms123.common.system.thread.ThreadContext;
import org.ms123.common.permission.api.PermissionService;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;


/**
 */
@SuppressWarnings("unchecked")
public class ShiroExecuteJobsRunnable implements Runnable {

  private final List<String> jobIds;
  private final JobExecutor jobExecutor;
	private Map<String,String> info;
	private ProcessEngine m_pe;
	private EventAdmin m_eventAdmin;
  
  public ShiroExecuteJobsRunnable(JobExecutor jobExecutor, Map<String,String> info, List<String> jobIds) {
    this.jobExecutor = jobExecutor;
    this.jobIds = jobIds;
    this.info = info;
  }

  public void run() {
    final JobExecutorContext jobExecutorContext = new JobExecutorContext();
    final List<String> currentProcessorJobQueue = jobExecutorContext.getCurrentProcessorJobQueue();
    final CommandExecutor commandExecutor = jobExecutor.getCommandExecutor();
		PermissionService ps = ((ShiroJobExecutor)jobExecutor).getPermissionService();
		m_pe = ((ShiroJobExecutor)jobExecutor).getProcessEngine();
		m_eventAdmin = ((ShiroJobExecutor)jobExecutor).getEventAdmin();

    currentProcessorJobQueue.addAll(jobIds);
		log("ShiroExecuteJobsRunnable.start:"+jobIds);
    Context.setJobExecutorContext(jobExecutorContext);
		String ns = info.get("namespace");
		ThreadContext.loadThreadContext(ns, info.get("user"));
		ps.loginInternal(ns);
		String jobId = null;
    try {
      while (!currentProcessorJobQueue.isEmpty()) {
				jobId = currentProcessorJobQueue.remove(0);
				setRetries(commandExecutor, jobId, 0);
        commandExecutor.execute(new ExecuteJobsCmd(jobId));
      }      
		} catch( Exception e){
		log("createLogEntry.ShiroExecuteJobsRunnable");
			createLogEntry(jobId,ns,e);
			if( e instanceof RuntimeException){
				throw (RuntimeException)e;
			}else{
				throw new RuntimeException(e);
			}
    } finally {
      Context.removeJobExecutorContext();
			log("ShiroExecuteJobsRunnable.finish");
    }
  }

	private void setRetries(CommandExecutor commandExecutor, final String jobId, final int retries) {
		ManagementService ms = m_pe.getManagementService();
		final Job job = ms.createJobQuery().jobId(jobId).singleResult();
    commandExecutor.execute(new Command<Void>() {
      
      public Void execute(CommandContext commandContext) {
        JobEntity jobEntity = commandContext.getDbSqlSession().selectById(JobEntity.class, job.getId());
        jobEntity.setRetries(retries);
        return null;
      }
      
    });
  }

	private void createLogEntry(String jobId, String namespace, Exception e){
		ManagementService ms = m_pe.getManagementService();
		Job job = ms.createJobQuery().jobId(jobIds.get(0)).singleResult();
		Map props = new HashMap();
		props.put("namespace", namespace);
		props.put("type", "exception/job");
		String key = namespace +"/"+getName(job.getProcessDefinitionId())+"/"+job.getProcessInstanceId();
		props.put("key", key);
		log("props:" + props);
		Throwable rc = getRootCause(e);
		props.put("msg", getStackTrace(rc != null ? rc : e));
		m_eventAdmin.sendEvent(new Event("log", props));
	}
	private String getName(String id){
		int ind = id.indexOf(":");
		if( ind != -1){
			return id.substring(0,ind);
		}
		return id;
	}
	private void log(String message) {
		m_logger.info(message);
		System.err.println(message);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(ShiroExecuteJobsRunnable.class);
}
