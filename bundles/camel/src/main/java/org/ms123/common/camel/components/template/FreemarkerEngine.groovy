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
package org.ms123.common.camel.components.template;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import org.apache.camel.Component;
import org.apache.camel.Message;
import org.apache.camel.CamelContext;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.cache.NullCacheStorage;

@SuppressWarnings("unchecked")
@groovy.transform.CompileStatic
public class FreemarkerEngine extends Engine{

	private Map<String, Template> m_templateCache = new LinkedHashMap();
	private Configuration m_configuration;
	FreemarkerTemplateLoader m_loader;

	public FreemarkerEngine(CamelContext cc) {
		m_loader = new FreemarkerTemplateLoader(cc);
	}


	public String convert( String text, Map<String,Object> variableMap){
		String key = getMD5OfUTF8(text);
		Template template = m_templateCache.get(key);
		if (template == null) {
			Reader reader = new StringReader(text);
			template = new Template("temp", reader, getConfiguration());
			m_templateCache.put(key, template);
		}
		StringWriter buffer = new StringWriter();
		Map binding = [:].withDefault { x -> new DefaultBinding(x) }
		binding.putAll( variableMap);
		info("Template is writing using attributes:" + binding);
		template.process(binding, buffer);
		buffer.flush();
		return buffer.toString();
	}


	private synchronized Configuration getConfiguration() {
		if (m_configuration == null) {
			m_configuration = new Configuration(Configuration.VERSION_2_3_23);
			m_configuration.setTemplateLoader(m_loader);
			m_configuration.setLocalizedLookup(false);
			m_configuration.setCacheStorage(new NullCacheStorage());
		}
		return m_configuration;
	}
	private void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}

	private void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(FreemarkerEngine.class);
}

