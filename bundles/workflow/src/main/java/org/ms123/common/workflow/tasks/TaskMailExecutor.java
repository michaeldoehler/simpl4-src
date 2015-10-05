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
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.ms123.common.docbook.DocbookService;
import org.apache.commons.beanutils.*;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricProcessInstance;
import flexjson.*;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.mail.EmailAttachment;

public class TaskMailExecutor extends TaskBaseExecutor implements JavaDelegate {
	protected JSONDeserializer m_ds = new JSONDeserializer();
	protected JSONSerializer m_js = new JSONSerializer();

	protected Expression to;
	protected Expression from;
	protected Expression cc;
	protected Expression bcc;
	protected Expression subject;
	protected Expression text;
	protected Expression html;
	protected Expression attachment;
	protected Expression charset;

	public void execute(DelegateExecution execution) {
		TaskContext tc = new TaskContext();
		tc.setExecution(execution);
		setCategory(tc);
		showVariablenNames(tc);
		String toStr = getStringFromField(to, execution);
		String fromStr = getStringFromField(from, execution);
		String ccStr = getStringFromField(cc, execution);
		String bccStr = getStringFromField(bcc, execution);
		String subjectStr = getStringFromField(subject, execution);
		String textStr = getStringFromField(text, execution);
		String htmlStr = getStringFromField(html, execution);
		String attachmentStr = getStringFromField(attachment, execution);
		String charSetStr = getStringFromField(charset, execution);

		Email email = createEmail(execution, textStr, htmlStr, attachmentStr);

		addTo(email, toStr);
		setFrom(email, fromStr);
		addCc(email, ccStr);
		addBcc(email, bccStr);
		setSubject(email, subjectStr);
		setMailServerProperties(email);
		//setCharset(email, charSetStr);

		try {
			email.setCharset("utf-8");
			email.send();
		} catch (EmailException e) {
			throw new RuntimeException("TaskMailExecutor.Could not send e-mail", e);
		}
	}

	protected Email createEmail(DelegateExecution execution, String text, String html, String attachmentPath) {
		if (html != null || attachmentPath != null) {
			return createHtmlEmail(execution, text, html, attachmentPath);
		} else if (text != null) {
			return createTextOnlyEmail(text);
		} else {
			throw new RuntimeException(
					"TaskMailExecutor:'html' or 'text' is required to be defined when using the mail activity");
		}
	}

	protected HtmlEmail createHtmlEmail(DelegateExecution execution, String text, String html, String attachmentPath) {
		HtmlEmail email = new HtmlEmail();
		try {
			if (html != null) {
				email.setHtmlMsg(html);
			}
			if (text != null) { // for email clients that don't support html
				email.setTextMsg(text);
			}
			if (attachmentPath != null) {
				email.attach(createAttachment(new File(getProcessDocBasedir(execution), attachmentPath).toString()));
			}
			return email;
		} catch (EmailException e) {
			throw new RuntimeException("TaskMailExecutor:Could not create HTML email", e);
		}
	}

	protected EmailAttachment createAttachment(String attachmentPath) {
		EmailAttachment attachment = new EmailAttachment();
		attachment.setPath(attachmentPath);
		attachment.setDisposition(EmailAttachment.ATTACHMENT);
		attachment.setDescription("");
		attachment.setName(getBasename(attachmentPath));
		return attachment;
	}

	protected SimpleEmail createTextOnlyEmail(String text) {
		SimpleEmail email = new SimpleEmail();
		try {
			email.setMsg(text);
			return email;
		} catch (Exception e) {
			throw new RuntimeException("TaskMailExecutor:Could not create text-only email", e);
		}
	}

	protected void addTo(Email email, String to) {
		String[] tos = splitAndTrim(to);
		if (tos != null) {
			for (String t : tos) {
				try {
					email.addTo(t);
				} catch (EmailException e) {
					throw new RuntimeException("TaskMailExecutor:Could not add " + t + " as recipient", e);
				}
			}
		} else {
			throw new RuntimeException("TaskMailExecutor:No recipient could be found for sending email");
		}
	}

	protected void setFrom(Email email, String from) {
		String fromAddres = null;

		if (from != null) {
			fromAddres = from;
		} else { // use default configured from address in process engine config
			fromAddres = Context.getProcessEngineConfiguration().getMailServerDefaultFrom();
		}

		try {
			email.setFrom(fromAddres);
		} catch (EmailException e) {
			throw new RuntimeException("TaskMailExecutor:Could not set " + from + " as from address in email", e);
		}
	}

	protected void addCc(Email email, String cc) {
		String[] ccs = splitAndTrim(cc);
		if (ccs != null) {
			for (String c : ccs) {
				try {
					email.addCc(c);
				} catch (EmailException e) {
					throw new RuntimeException("TaskMailExecutor:Could not add " + c + " as cc recipient", e);
				}
			}
		}
	}

	protected void addBcc(Email email, String bcc) {
		String[] bccs = splitAndTrim(bcc);
		if (bccs != null) {
			for (String b : bccs) {
				try {
					email.addBcc(b);
				} catch (EmailException e) {
					throw new RuntimeException("TaskMailExecutor:Could not add " + b + " as bcc recipient", e);
				}
			}
		}
	}

	protected void setSubject(Email email, String subject) {
		email.setSubject(subject != null ? subject : "");
	}

	protected void setMailServerProperties(Email email) {
		ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();

		String host = processEngineConfiguration.getMailServerHost();
		if (host == null) {
			throw new RuntimeException("TaskMailExecutor:Could not send email: no SMTP host is configured");
		}
		email.setHostName(host);

		int port = processEngineConfiguration.getMailServerPort();
		email.setSmtpPort(port);

		email.setSSL(processEngineConfiguration.getMailServerUseSSL());
		email.setTLS(processEngineConfiguration.getMailServerUseTLS());

		String user = processEngineConfiguration.getMailServerUsername();
		String password = processEngineConfiguration.getMailServerPassword();
		if (user != null && password != null) {
			email.setAuthentication(user, password);
		}
	}

	protected void setCharset(Email email, String charSetStr) {
		if (charset != null) {
			email.setCharset(charSetStr);
		}
	}

	protected String[] splitAndTrim(String str) {
		if (str != null) {
			String[] splittedStrings = str.split(",");
			for (int i = 0; i < splittedStrings.length; i++) {
				splittedStrings[i] = splittedStrings[i].trim();
			}
			return splittedStrings;
		}
		return null;
	}

	private String getBasename(String path) {
		String e[] = path.split("/");
		return e[e.length - 1];
	}

	protected String getStringFromField(Expression expression, DelegateExecution execution) {
		if (expression != null) {
			Object value = expression.getValue(execution);
			if (value != null) {
				return value.toString();
			}
		}
		return null;
	}

}

