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
package org.ms123.common.libhelper;
import flexjson.*;
import flexjson.transformer.*;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Bean2Map {
	private TypeTransformerMap typeTransformerMap = new TypeTransformerMap(TransformerUtil.getDefaultTypeTransformers());
	private Map<Path, Transformer> pathTransformerMap = new HashMap<Path, Transformer>();
	private List<PathExpression> pathExpressions = new ArrayList<PathExpression>();

	public Bean2Map() { 
		exclude("class");
	}
	public Map<String, Object> transform(Object target) throws Exception{
		Map<String,Object> ret = new HashMap<String,Object>();
		return transform(target, ret );
	}

	public <T extends Map<String,Object>> T transform(Object target, T ret) throws Exception{
		JSONContext context = JSONContext.get();
		try {
			context.setTypeTransformers(typeTransformerMap);
			context.setPathTransformers(pathTransformerMap);
			context.setPathExpressions(pathExpressions);
      context.setRootName( null );

			Path path = context.getPath();
			BeanAnalyzer analyzer = BeanAnalyzer.analyze(target.getClass());
			for (BeanProperty prop : analyzer.getProperties()) {
				String name = prop.getName();
        path.enqueue(name);
				if (context.isIncluded(prop)) {
					Object value = prop.getValue(target);
					ret.put(name, value);
				}
        path.pop();
			}

		} finally {
			JSONContext.cleanup();

		}
		return ret;
	}

	protected void addExclude(String field) {
		int index = field.lastIndexOf('.');
		if (index > 0) {
			PathExpression expression = new PathExpression(field.substring(0, index), true);
			if (!expression.isWildcard()) {
				pathExpressions.add(expression);
			}
		}
		pathExpressions.add(new PathExpression(field, false));
	}

	protected void addInclude(String field) {
		pathExpressions.add(new PathExpression(field, true));
	}

	public Bean2Map exclude(String... fields) {
		for (String field : fields) {
			addExclude(field);
		}
		return this;
	}

	public Bean2Map include(String... fields) {
		for (String field : fields) {
			addInclude(field);
		}
		return this;
	}

	public void setIncludes(List<String> fields) {
		for (String field : fields) {
			pathExpressions.add(new PathExpression(field, true));
		}
	}

	public void setExcludes(List<String> fields) {
		for (String field : fields) {
			addExclude(field);
		}
	}

/*	public static void main(String args[])throws Exception {

		Module mod = new Module();

		mod.setId("ID1");
		mod.setCategory("data");
		mod.setBaseurl("http:xxx");

		Bean2Map b2m = new Bean2Map();
		Map<String,Object> m = b2m.transform(mod);
		
		System.out.println("m:" + m);

	}*/
}
