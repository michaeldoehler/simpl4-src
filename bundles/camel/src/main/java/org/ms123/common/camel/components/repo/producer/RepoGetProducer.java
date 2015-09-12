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
package org.ms123.common.camel.components.repo.producer;

import org.apache.camel.component.file.*;
import org.apache.camel.Exchange;
import org.apache.camel.CamelContext;
import org.ms123.common.camel.components.repo.RepoConfiguration;
import org.ms123.common.camel.components.repo.RepoEndpoint;
import org.slf4j.Logger;
import java.io.File;
import org.slf4j.LoggerFactory;
import org.ms123.common.git.GitService;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import org.apache.camel.util.FileUtil;

public class RepoGetProducer extends RepoProducer {

	private static final transient Logger LOG = LoggerFactory.getLogger(RepoGetProducer.class);

	public RepoGetProducer(RepoEndpoint endpoint, RepoConfiguration configuration) {
		super(endpoint, configuration);
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		String path = configuration.getPath();
		String repo = configuration.getRepo();
		String type = configuration.getType();
		String target = configuration.getTarget();
		String header = configuration.getHeader();
		GitService gitService = getGitService();
		info("producer --> get:repo: " + repo + ",path:" + path + ",type:" + type+"/target:"+target);
		File file = gitService.searchFile(repo, path, type);
		if( "body".equals(target)){
			exchange.getIn().setBody(file);
		}else{
			exchange.getIn().setHeader(header, file );
		}
	}
	private void info(String msg) {
		System.out.println(msg);
		LOG.info(msg);
	}
}
