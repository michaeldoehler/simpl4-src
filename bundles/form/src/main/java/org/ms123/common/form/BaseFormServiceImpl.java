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
package org.ms123.common.form;

import flexjson.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.Reader;
import java.lang.reflect.*;
import org.osgi.framework.wiring.*;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Persistent;
import java.lang.annotation.Annotation;
import org.ms123.common.git.GitService;
import org.ms123.common.stencil.api.StencilService;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import javax.validation.ValidatorFactory;
import javax.validation.Validator;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.apache.commons.beanutils.PropertyUtils;
import org.osgi.framework.BundleContext;
import org.apache.commons.beanutils.BeanMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import org.apache.commons.beanutils.ConvertUtils;

/**
 *
 */
@SuppressWarnings("unchecked")
class BaseFormServiceImpl {

	protected BundleContext m_bc;

	protected DataLayer m_dataLayer;

	protected GitService m_gitService;

	protected StencilService m_stencilService;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	private ValidatorFactory m_factory = Validation.buildDefaultValidatorFactory();

	private Validator m_validator = null;

	protected Map _validateForm(String namespace, String name, Map data, boolean cleanData) throws Exception {
		String json = m_gitService.searchContent(namespace, name, "sw.form");
		Map rootShape = (Map) m_ds.deserialize(json);
		Map<String, List> constraintsMeta = getConstraintsFromStencil(namespace);
		List<Map> inputShapes = new ArrayList();
		List<String> idList = new ArrayList();
		idList.add("input");
		idList.add("textarea");
		traverseElement(inputShapes, rootShape, idList);
		long time = new java.util.Date().getTime();
		BundleDelegatingClassLoader bdc = new BundleDelegatingClassLoader(m_bc.getBundle());
		ClassBuilder cb = new ClassBuilder(bdc, inputShapes, constraintsMeta);
		Class clazz = cb.getClazz();
		findAnnotatedFields(clazz);
		Object obj = clazz.newInstance();
		BeanMap bm = new BeanMap(obj);
		try {
			bm.putAll(cleanData(data, inputShapes, true));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("FormService._validateForm:", e);
		}
		Set cv = validate(obj);
		List<Map> errors = new ArrayList();
		if (cv.size() > 0) {
			errors = constructConstraitViolationList(cv, inputShapes, constraintsMeta);
			System.out.println("cv:" + errors);
		} else {
			StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
			SessionContext sc = m_dataLayer.getSessionContext(sdesc);
			for (Map shape : inputShapes) {
				Map filterCheck = getFilterCheck(shape);
				if (filterCheck == null) {
					continue;
				}
				String fieldName = getName(shape);
				Object value = data.get(fieldName);
				boolean ok = executeFilter(sc, filterCheck, fieldName, value);
				if (!ok) {
					Map error = new HashMap();
					errors.add(error);
					error.put("path", fieldName);
					String msg = (String)filterCheck.get("message");
					error.put("message", msg != null ? msg : "%field_not_in_database");
				}
			}
		}
		Map ret = new HashMap();
		ret.put("errors", errors);
		if (cleanData) {
			inputShapes = new ArrayList();
			idList = new ArrayList();
			idList.add("input");
			idList.add("textarea");
			idList.add("gridinput");
			idList.add("tableselect");
			idList.add("enumselect");
			idList.add("alert");
			idList.add("actionbutton");
			traverseElement(inputShapes, rootShape, idList);
			ret.put("cleanData", cleanData(data, inputShapes, false));
		}
		ret.put("postProcess", getPostProcessScript(rootShape));
		return ret;
	}

	private boolean executeFilter(SessionContext sc, Map filterCheck, String fieldName, Object value) throws Exception {
		Map fdesc = filterCheck;
		String filterName = (String) fdesc.get("name");
		List<Map> items = (List) fdesc.get("items");
		Map params = new HashMap();
		for (Map<String, String> item : items) {
			String param = item.get("param");
			params.put(param, value);
			break;
		}
		System.out.println("FormService.executeFilter.params:" + params);
		Map result = sc.executeNamedFilter(filterName, params);
		List rows = (List) result.get("rows");
		System.out.println("FormService.executeFilter.rows:" + rows);
		return rows.size() > 0;
	}

	private Set validate(Object obj) {
		if (m_validator == null) {
			m_validator = m_factory.getValidator();
		}
		ClassLoader clParent = Thread.currentThread().getContextClassLoader();
		try {
			BundleDelegatingClassLoader bdc = new BundleDelegatingClassLoader(m_bc.getBundle(), clParent);
			Thread.currentThread().setContextClassLoader(bdc);
			return m_validator.validate(obj);
		} finally {
			Thread.currentThread().setContextClassLoader(clParent);
		}
	}

	public void findAnnotatedFields(Class clazz) throws Exception {
		Field[] declaredFields = clazz.getDeclaredFields();
		List<Field> annotatedFields = new ArrayList<Field>(declaredFields.length);
		for (Field field : declaredFields) {
			//System.out.println("Field:" + field);
			Annotation[] ann = findFieldAnnotations(field);
			for (Annotation a : ann) {
				//System.out.println("\tAnno:" + a);
			}
		}
	}

	public Annotation[] findFieldAnnotations(Field field) throws Exception {
		return field.getAnnotations();
	}

	protected Map cleanData(Map data, List<Map> shapes, boolean convert) {
		Map newMap = new HashMap();
		System.out.println("FormService.cleanData.start("+convert+"):"+data);
		for (Map shape : shapes) {
			String name = getName(shape);
			Class type = ConstraintsUtils.getType(shape);
			Object value = data.get(name);
			System.out.println("\tname:"+name+"/"+type+"="+value);
			if( name == null || "".equals(name.trim())) continue;
			if (value != null && convert) {
				if (type == Double.class || type == Integer.class) {
					if (value instanceof String && "".equals(value)) {
						value = null;
					}
				}
				if (type == Date.class) {
					try {
						if( value instanceof String){
							value= new SimpleDateFormat("yyyy-MM-dd").parse((String)value,new ParsePosition(0));
						}else{
							Calendar cal = Calendar.getInstance();
							cal.setTimeInMillis(((Long) value));
							value = cal.getTime();
						}
					} catch (Exception e) {
						System.out.println("DateException:" + name + "=" + value + ":" + e.getMessage());
						value = null;
					}
				}
			}
			newMap.put(name, value);
		}
		System.out.println("FormService.cleanData.end:" + newMap);
		return newMap;
	}

	protected String getName(Map element) {
		Map properties = (Map) element.get("properties");
		return ((String) properties.get("xf_id"));
	}

	protected String getPostProcessScript(Map element) {
		Map properties = (Map) element.get("properties");
		return ((String) properties.get("xf_postprocess"));
	}

	protected Map getFilterCheck(Map element) {
		Map properties = (Map) element.get("properties");
		if (properties.get("xf_filtercheck") instanceof Map) {
			return ((Map) properties.get("xf_filtercheck"));
		}
		return null;
	}

	private List<Map> constructConstraitViolationList(Set<ConstraintViolation> constraintViolations, List<Map> shapes, Map<String, List> constraintsMeta) {
		List<Map> cvList = new ArrayList();
		for (ConstraintViolation cv : constraintViolations) {
			Map<String, Object> mapCV = new HashMap();
			String path = cv.getPropertyPath() + "";
			String message = cv.getMessage() + "";
			String annoName = cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
			if (annoName.equals("DateMax") || annoName.equals("DateMin")) {
				Map shape = getShape(path, shapes);
				Map cd = ConstraintsUtils.getConstraintsData(shape);
				List vals = (List) cd.get(annoName);
				//Map cm = ConstraintsUtils.getConstraintsMeta(annoClass, shape, constraintsMeta);
				mapCV.put("time", vals.get(1));
			}
			mapCV.put("message", message);
			mapCV.put("path", path);
			cvList.add(mapCV);
		}
		return cvList;
	}

	private Map getShape(String name, List<Map> shapes) {
		for (Map shape : shapes) {
			if (getName(shape).equals(name)) {
				return shape;
			}
		}
		return null;
	}

	private String getCleanMessage(String msg) {
		String pattern = ".*RuntimeClassFile\\$\\d+\\.(.*)";
		String ret = msg.replaceAll(pattern, "$1");
		return ret;
	}

	private Map<String, List> getConstraintsFromStencil(String namespace) throws Exception {
		Map<String, List> retMap = new HashMap();
		JSONDeserializer ds = new JSONDeserializer();
		Map<String, List> ssMap = (Map) ds.deserialize(m_stencilService.getStencilSet(namespace, "form"));
		List<Map> stencilList = ssMap.get("stencils");
		List<String> retList = new ArrayList();
		for (Map stencil : stencilList) {
			if ("input".equals(((String) stencil.get("id")).toLowerCase())) {
				List<Map> properties = (List) stencil.get("properties");
				for (Map prop : properties) {
					String pid = ((String) prop.get("id")).toLowerCase();
					if (pid.startsWith("xf_constraint")) {
						retMap.put(pid.substring("xf_constraint_".length()), (List) prop.get("config"));
					}
				}
			}
		}
		System.out.println("retMap:" + retMap);
		return retMap;
	}

	private void traverseElement(List<Map> fields, Map shape, List<String> idList) throws Exception {
		String id = getStencilId(shape);
		if (idList.contains(id)) {
			//System.out.println("Form.traverseElement:id_used:"+id);
			fields.add(shape);
		}else{
			//System.out.println("Form.traverseElement:id_not_used:"+id);
		}
		List<Map> childShapes = (List) shape.get("childShapes");
		for (Map child : childShapes) {
			traverseElement(fields, child, idList);
		}
	}

	private String getStencilId(Map element) {
		Map stencil = (Map) element.get("stencil");
		String id = ((String) stencil.get("id")).toLowerCase();
		return id;
	}
}
