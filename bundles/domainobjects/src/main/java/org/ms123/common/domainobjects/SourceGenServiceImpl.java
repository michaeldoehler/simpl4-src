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
package org.ms123.common.domainobjects;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import flexjson.JSON;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Future;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Pattern.Flag;
import javax.validation.constraints.Size;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import org.hibernate.validator.constraints.CreditCardNumber;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.constraints.URL;
import org.ms123.common.entity.api.EntityService;
import static org.ms123.common.entity.api.Constants.LEFT_ENTITY;
import static org.ms123.common.entity.api.Constants.RIGHT_ENTITY;
import static org.ms123.common.entity.api.Constants.LEFT_FIELD;
import static org.ms123.common.entity.api.Constants.RIGHT_FIELD;
import static org.ms123.common.entity.api.Constants.RELATION;
import org.ms123.common.utils.ScriptEngineService;
import org.ms123.common.utils.annotations.RelatedTo;
import org.tearsinrain.jcodemodel.JAnnotationArrayMember;
import org.tearsinrain.jcodemodel.JAnnotationUse;
import org.tearsinrain.jcodemodel.JBlock;
import org.tearsinrain.jcodemodel.JCodeModel;
import org.tearsinrain.jcodemodel.JDefinedClass;
import org.tearsinrain.jcodemodel.JExpression;
import org.tearsinrain.jcodemodel.JFieldVar;
import org.tearsinrain.jcodemodel.JMethod;
import org.tearsinrain.jcodemodel.JPackage;
import org.tearsinrain.jcodemodel.JMod;
import org.tearsinrain.jcodemodel.JType;
import org.ms123.common.libhelper.Inflector;
import org.osgi.framework.BundleContext;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.nucleus.api.NucleusService;

/**
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true)
public class SourceGenServiceImpl implements SourceGenService {

	private JSONSerializer m_js = new JSONSerializer();

	private JSONDeserializer m_ds = new JSONDeserializer();

	protected Inflector m_inflector = Inflector.getInstance();

	private ScriptEngineService m_scriptEngineService;

	private int VARCHAR_LEN = 128;

	private int TEXT_LEN = 128000;

	protected EntityService m_entityService;

	protected NucleusService m_nucleusService;

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("SourceGenServiceImpl.activate");
		try {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<String> generate(StoreDesc sdesc, List<Map> modules, String outDir) throws Exception {
		JCodeModel cm = new JCodeModel();
		System.out.println("------------>>>>generate:" + sdesc.getString());
		for (int i = 0; i < modules.size(); i++) {
			Map m = modules.get(i);
			String name = (String) m.get("name");
			String classname = getFQN(sdesc, m);
			System.out.println("\tclassname:" + classname);
			List fields = null;
			//(List) m.get("fields");
			if (fields == null) {
				fields = getEntityMetaData(sdesc, name);
			}
			generateSource(sdesc, cm, fields, classname, true);
		}
		List<Map> rels = getRelations(sdesc);
		System.out.println("xrel:" + rels);
		for (int i = 0; rels != null && i < rels.size(); i++) {
			generateRelationSource(cm, rels.get(i), sdesc);
		}
		for (int i = 0; i < modules.size(); i++) {
			Map m = modules.get(i);
			String classname = getFQN(sdesc, m);
			boolean genDefFields = (Boolean) m.get("default_fields");
			boolean team_security = (m.get("team_security") != null) ? (Boolean) m.get("team_security") : false;
			if (genDefFields) {
				JDefinedClass dc = cm._getClass(classname);
				generateDefaultFields(cm, dc, sdesc, classname, team_security);
			}
		}
		File out = new File(outDir);
		if (!out.exists()) {
			out.mkdirs();
		}
		System.out.println("before build");
		cm.build(out);
		List<String> retList = new ArrayList();
		Iterator<JPackage> itp = cm.packages();
		while (itp.hasNext()) {
			JPackage jp = itp.next();
			Iterator<JDefinedClass> itc = jp.classes();
			System.out.println("pack:" + jp.name());
			while (itc.hasNext()) {
				JDefinedClass dc = itc.next();
				System.out.println("\t:" + dc.fullName());
				retList.add(outDir + "/" + dc.fullName().replace('.', File.separatorChar) + ".java");
			}
		}
		System.out.println("retList:" + retList);
		return retList;
	}

	protected void generateDefaultFields(JCodeModel cm, JDefinedClass dc, StoreDesc sdesc, String module, boolean team_security) throws Exception {
		if (sdesc.isAidPack()) {
			return;
		}
		List<Map> defaultFields = m_entityService.getDefaultFields();
		System.out.println("generateDefaultFields:" + module + "/" + sdesc);
		String fqn = sdesc.getJavaPackage() + "." + m_inflector.getClassName(module);
		for (Map<String, String> field : defaultFields) {
			String datatype = field.get("datatype");
			if (datatype.startsWith("array")) {
				if (team_security) {
					Map<String, Object> rel = new HashMap();
					rel.put(RELATION, "one-to-many");
					rel.put(LEFT_ENTITY, module);
					rel.put(LEFT_FIELD, field.get("name"));
					rel.put(RIGHT_ENTITY, StoreDesc.PACK_AID + ".Team");
					rel.put("dependent", true);
					generateRelationSource(cm, rel, sdesc);
				}
			} else {
				generateField(dc, field.get("name"), datatype, null, null, fqn, null, true);
			}
		}
		if (sdesc.isAidPack()) {
			return;
		}
	}

	protected void generateRelationSource(JCodeModel cm, Map<String, Object> rel, StoreDesc sdesc) throws Exception {
		String app = sdesc.getNamespace();
		if (rel.get(LEFT_ENTITY) == null) {
			System.out.println("generateRelationSource.leftmodule is null");
			return;
		}
		String relation = (String) rel.get(RELATION);
		String leftmodule = sdesc.insertJavaPackage((String) rel.get(LEFT_ENTITY));
		String leftfield = (String) rel.get(LEFT_FIELD);
		String rightmodule = sdesc.insertJavaPackage((String) rel.get(RIGHT_ENTITY));
		String rightfield = (String) rel.get(RIGHT_FIELD);
		boolean dependent = getBoolean(rel.get("dependent"), false);
		System.out.println("generateRelationSource:" + leftmodule + "/" + rightmodule + "/" + dependent + "/" + relation);
		boolean manyToMany = "many-to-many".equals(relation);
		boolean oneToMany = "one-to-many".equals(relation);
		boolean oneToManyBi = "one-to-many-bi".equals(relation);
		boolean oneToOne = "one-to-one".equals(relation);
		boolean oneToOneBi = "one-to-one-bi".equals(relation);
		if (leftfield == null || "".equals(leftfield)) {
			if (manyToMany) {
				leftfield = m_inflector.pluralize(rightmodule).toLowerCase();
			}
			if (oneToMany || oneToManyBi || oneToOne || oneToOneBi) {
				leftfield = m_inflector.pluralize(rightmodule).toLowerCase();
			}
		}
		if (oneToOne || oneToOneBi) {
			leftfield = m_inflector.singularize(leftfield).toLowerCase();
		}
		if (rightfield == null || "".equals(rightfield)) {
			if (manyToMany) {
				rightfield = m_inflector.pluralize(leftmodule).toLowerCase();
			}
			if (oneToMany || oneToOne) {
			}
			if (oneToManyBi || oneToOneBi) {
				rightfield = m_inflector.singularize(leftmodule).toLowerCase();
			}
		}
		rightfield = removePackageName(rightfield);
		leftfield = removePackageName(leftfield);
		System.out.println("rightfield:" + rightfield + "/" + leftfield + "/" + oneToMany + "/" + oneToOneBi + "/" + manyToMany + "/" + oneToOne + "/" + oneToOneBi);
		JDefinedClass dc = cm._getClass(leftmodule);
		System.out.println("dc:" + dc);
		if (dc == null) {
			return;
		}
		if (oneToMany || oneToManyBi || manyToMany) {
			System.out.println("relation:" + relation + ",lm:" + leftmodule + "/" + leftfield + ",rm:" + rightmodule + "/" + rightfield);
			Class type = Set.class;
			JFieldVar f = dc.field(JMod.PRIVATE, type, leftfield);
			JType jtype = cm.parseType(rightmodule);
			System.out.println("jtype:" + jtype);
			if (oneToManyBi) {
				f.annotate(javax.jdo.annotations.Persistent.class).param("mappedBy", rightfield);
			}
			if (manyToMany) {
				f.annotate(javax.jdo.annotations.Join.class).param("column", rightfield.toLowerCase() + "_id");
				// @@@MS
				f.annotate(javax.jdo.annotations.Persistent.class).param("table", /*app + "." +*/
				getJoinTableName(leftmodule, leftfield, rightmodule, rightfield));
			}
			if (oneToManyBi || manyToMany) {
				createRightField(cm, leftmodule, rightmodule, leftfield, rightfield, manyToMany);
			}
			if (oneToMany) {
				f.annotate(javax.jdo.annotations.Element.class).param("types", jtype).param("column", (leftfield + "_" + leftmodule).replace('.', '_').toLowerCase());
			} else if (manyToMany) {
				// f.annotate(javax.jdo.annotations.Element.class).param("types", jtype).param("column", (leftfield + "_" + leftmodule).toLowerCase());
				f.annotate(javax.jdo.annotations.Element.class).param("types", jtype).param("column", (leftfield + "_id").toLowerCase());
			} else {
				if (dependent) {
					f.annotate(javax.jdo.annotations.Element.class).param("types", jtype).param("dependent", "true");
				} else {
					f.annotate(javax.jdo.annotations.Element.class).param("types", jtype);
				}
			}
			JMethod setter = dc.method(JMod.PUBLIC, void.class, "set" + firstToUpper(leftfield));
			JBlock block = setter.body();
			JExpression e = setter.param(type, "data");
			block.assign(f, e);
			JMethod getter = dc.method(JMod.PUBLIC, type, "get" + firstToUpper(leftfield));
			block = getter.body();
			block._return(f);
		} else if (oneToOne || oneToOneBi) {
			JType jtype = cm.parseType(rightmodule);
			System.out.println("jtype2:" + jtype);
			JFieldVar f = dc.field(JMod.PRIVATE, jtype, leftfield);
			if (dependent) {
				f.annotate(javax.jdo.annotations.Persistent.class).param("dependent", "true");
			} else {
				f.annotate(javax.jdo.annotations.Persistent.class);
			}
			// f.annotate(flexjson.JSON.class).param("include", false); 
			JMethod setter = dc.method(JMod.PUBLIC, void.class, "set" + firstToUpper(leftfield));
			JBlock block = setter.body();
			JExpression e = setter.param(jtype, "data");
			block.assign(f, e);
			JMethod getter = dc.method(JMod.PUBLIC, jtype, "get" + firstToUpper(leftfield));
			block = getter.body();
			block._return(f);
			if (oneToOneBi) {
				createRightFieldOneToOneBi(cm, leftmodule, rightmodule, leftfield, rightfield, manyToMany);
			}
		}
	}

	protected void createRightField(JCodeModel cm, String leftmodule, String rightmodule, String leftfield, String rightfield, boolean many) throws Exception {
		JDefinedClass dc = cm._getClass(rightmodule);
		System.out.println("createRightField:" + rightmodule + "/" + dc);
		Class type = Set.class;
		if (!many) {
			if (dc == null || dc.fields() == null) {
				System.out.println("SourceGenService.createRightField:" + rightmodule + " not exists");
				return;
			}
			if (dc.fields().get(rightfield) != null) {
				// RelatedTo
				return;
			}
			JType jtype = cm.parseType(leftmodule);
			JFieldVar f = dc.field(JMod.PRIVATE, jtype, rightfield);
			f.annotate(javax.jdo.annotations.Persistent.class);
			// @@@MS in some cases not Persistent 
			JMethod setter = dc.method(JMod.PUBLIC, void.class, "set" + firstToUpper(m_inflector.singularize(rightfield)));
			JBlock block = setter.body();
			JExpression e = setter.param(jtype, "data");
			block.assign(f, e);
			JMethod getter = dc.method(JMod.PUBLIC, jtype, "get" + firstToUpper(m_inflector.singularize(rightfield)));
			block = getter.body();
			block._return(f);
		} else {
			JFieldVar f = dc.field(JMod.PRIVATE, type, rightfield);
			f.annotate(javax.jdo.annotations.Persistent.class).param("mappedBy", leftfield);
			JType jtype = cm.parseType(leftmodule);
			f.annotate(javax.jdo.annotations.Element.class).param("types", jtype);
			JMethod setter = dc.method(JMod.PUBLIC, void.class, "set" + firstToUpper(rightfield));
			JBlock block = setter.body();
			JExpression e = setter.param(type, "data");
			block.assign(f, e);
			JMethod getter = dc.method(JMod.PUBLIC, type, "get" + firstToUpper(rightfield));
			block = getter.body();
			block._return(f);
		}
	}

	protected void createRightFieldOneToOneBi(JCodeModel cm, String leftmodule, String rightmodule, String leftfield, String rightfield, boolean many) throws Exception {
		JDefinedClass dc = cm._getClass(rightmodule);
		JType jtype = cm.parseType(leftmodule);
		JFieldVar f = dc.field(JMod.PRIVATE, jtype, rightfield);
		f.annotate(javax.jdo.annotations.Persistent.class).param("mappedBy", leftfield);
		JMethod setter = dc.method(JMod.PUBLIC, void.class, "set" + firstToUpper(rightfield));
		JBlock block = setter.body();
		JExpression e = setter.param(jtype, "data");
		block.assign(f, e);
		JMethod getter = dc.method(JMod.PUBLIC, jtype, "get" + firstToUpper(rightfield));
		block = getter.body();
		block._return(f);
	}

	protected void generateSource(StoreDesc sdesc, JCodeModel cm, List fields, String module, boolean withAnnotation) throws Exception {
		JDefinedClass dc = cm._class(module);
		dc._implements(java.io.Serializable.class);
		String appName = sdesc.getNamespace();
		if (withAnnotation) {
			//dc.annotate(javax.jdo.annotations.PersistenceCapable.class).param("identityType", javax.jdo.annotations.IdentityType.APPLICATION).param("schema", appName);
			dc.annotate(javax.jdo.annotations.PersistenceCapable.class).param("identityType", javax.jdo.annotations.IdentityType.APPLICATION);
		}
		String idField = "id";
		Class idClass = String.class;
		Object idConstraint = null;
		boolean hasPrimaryKey = false;
		if (fields != null) {
			Iterator it = fields.iterator();
			while (it.hasNext()) {
				Map m = (Map) it.next();
				if (m.get("primary_key") != null && ((Boolean) m.get("primary_key")) == true) {
					System.out.println("primary_key:" + m.get("name"));
					idField = (String) m.get("name");
					hasPrimaryKey = true;
					idClass = ("number".equals(m.get("datatype")) ? Long.class : String.class);
					idConstraint = m.get("constraints");
				}
			}
		}
		JFieldVar f = dc.field(JMod.PRIVATE, idClass, idField);
		if (withAnnotation) {
			if (!hasPrimaryKey) {
				f.annotate(javax.jdo.annotations.Persistent.class).param("valueStrategy", javax.jdo.annotations.IdGeneratorStrategy.UUIDHEX);
			}
			f.annotate(javax.jdo.annotations.PrimaryKey.class);
			if (idConstraint != null) {
				if (idConstraint instanceof String) {
					idConstraint = JSONArray.fromObject((String) idConstraint);
				}
				generateConstraints(f, (List) idConstraint);
			}
		}
		JMethod setter = dc.method(JMod.PUBLIC, void.class, "set" + firstToUpper(idField));
		JBlock block = setter.body();
		JExpression e = setter.param(idClass, "data");
		block.assign(f, e);
		JMethod getter = dc.method(JMod.PUBLIC, idClass, "get" + firstToUpper(idField));
		block = getter.body();
		block._return(f);
		if (fields == null) {
			return;
		}
		Iterator it = fields.iterator();
		while (it.hasNext()) {
			Map m = (Map) it.next();
			String[] val = new String[2];
			if (m.get("enabled") != null && ((Boolean) m.get("enabled") == false)) {
				System.out.println("SourceGenService.generateSource(" + module + "," + m.get("name") + "):disabled");
				continue;
			}
			val[0] = (String) (m.get("name"));
			val[1] = (String) m.get("datatype");
			String edittype = (String) m.get("edittype");
			String defaultValue = (m.get("default_value") != null) ? (m.get("default_value") + "") : null;
			if (val[0].equals(idField)) {
				continue;
			}
			System.out.println("\tentity:" + module + "/" + val[0] + "/" + val[1]);
			if (val[0].equals("traits")) {
			} else if (val[1].startsWith("object") || val[1].startsWith("related")) {
				int first = val[1].indexOf("/");
				int last = val[1].lastIndexOf("/");
				String datatype = null;
				String relatedToField = null;
				if (first == last) {
					datatype = val[1].substring(first + 1);
				} else {
					datatype = val[1].substring(first + 1, last);
					relatedToField = val[1].substring(last + 1);
				}
				System.out.println("\tdatatype2:" + datatype);
				System.out.println("\tdatatype2:" + relatedToField);
				String fieldName = relatedToField != null ? relatedToField : val[0];
				JType jtype = cm.parseType(datatype);
				f = dc.field(JMod.PRIVATE, jtype, fieldName.toLowerCase());
				if (val[1].startsWith("related")) {
					if (relatedToField != null) {
						f.annotate(org.ms123.common.utils.annotations.RelatedTo.class).param("value", relatedToField);
					} else {
						f.annotate(org.ms123.common.utils.annotations.RelatedTo.class);
					}
					f.annotate(javax.jdo.annotations.Persistent.class);
					// @@@MS in some cases not Persistent 
					f.annotate(flexjson.JSON.class).param("include", false);
				}
				setter = dc.method(JMod.PUBLIC, void.class, "set" + firstToUpper(fieldName));
				block = setter.body();
				e = setter.param(jtype, "data");
				block.assign(f, e);
				getter = dc.method(JMod.PUBLIC, jtype, "get" + firstToUpper(fieldName));
				block = getter.body();
				block._return(f);
			} else {
				if ("decimalnumber".equals(val[0])) {
					System.out.println("m:" + m);
				}
				generateField(dc, val[0], val[1], edittype, defaultValue, module, m.get("constraints"), withAnnotation);
			}
		}
	}

	private Class getClass(String dt) {
		Class type = String.class;
		if (dt.equals("date")) {
			type = Date.class;
		} else if (dt.equals("boolean")) {
			type = Boolean.class;
		} else if (dt.equals("float")) {
			type = Float.class;
		} else if (dt.equals("double")) {
			type = Double.class;
		} else if (dt.equals("decimal")) {
			type = Double.class;
		} else if (dt.equals("integer")) {
			type = Integer.class;
		} else if (dt.equals("array/string")) {
			type = String.class;
		} else if (dt.equals("text")) {
			type = String.class;
		} else if (dt.equals("binary")) {
			type = byte[].class;
		} else if (dt.equals("geopoint")) {
			type = String.class;
		} else if (dt.startsWith("related")) {
		} else if (dt.equals("number")) {
			type = Integer.class;
		}
		return type;
	}

	private boolean noDefaultFields(String module) {
		if (module.toLowerCase().equals("common.logmessage")) {
			return true;
		}
		return false;
	}

	private void generateField(JDefinedClass dc, String name, String datatype, String edittype, String defaultValue, String module, Object co, boolean withAnnotation) {
		//		System.out.println("\t\tgenerateField:" + dc + "/" + name + "/" + module + "/def:" + datatype + "/co:" + co);
		Class type = getClass(datatype);
		JFieldVar f = dc.field(JMod.PRIVATE, type, name);
		if (withAnnotation) {
			boolean isGraphical = (edittype != null && edittype.startsWith("graphical"));
			if ("textarea".equals(edittype) || isGraphical || datatype.equals("text") || datatype.equals("array/string")) {
				int len = isGraphical ? 128000 : TEXT_LEN;
				if (defaultValue != null) {
					f.annotate(javax.jdo.annotations.Column.class).param("defaultValue", defaultValue).param("jdbcType", "VARCHAR").param("length", len);
				} else {
					f.annotate(javax.jdo.annotations.Column.class).param("jdbcType", "VARCHAR").param("length", len);
				}
			} else if ("fulltext".equals(datatype)) {
				JAnnotationUse au = f.annotate(javax.jdo.annotations.Extensions.class);
				JAnnotationArrayMember aam = au.paramArray("value");
				aam.annotate(javax.jdo.annotations.Extension.class).param("vendorName", "datanucleus").param("key", "update-function").param("value", "to_tsvector('simple', ?)");
				aam.annotate(javax.jdo.annotations.Extension.class).param("vendorName", "datanucleus").param("key", "insert-function").param("value", "to_tsvector('simple', ?)");
				f.annotate(javax.jdo.annotations.Column.class).param("jdbcType", "tsvector");
				f.annotate(javax.jdo.annotations.Persistent.class).param("defaultFetchGroup", "false");
			} else {
				if (defaultValue != null) {
					f.annotate(javax.jdo.annotations.Column.class).param("defaultValue", defaultValue);
				}
			}
			if ("auto".equals(edittype) && type.equals(Integer.class)) {
				f.annotate(javax.jdo.annotations.Persistent.class).param("customValueStrategy", "increment");
			} else {
				if (co != null) {
					if (co instanceof String) {
						co = JSONArray.fromObject((String) co);
					}
					generateConstraints(f, (List) co);
				}
			}
			if (!datatype.equals("binary") && !isGraphical) {
				f.annotate(javax.jdo.annotations.Index.class).param("name", "index_" + module.replaceAll("\\.", "_").toLowerCase() + "_" + name);
			}
		}
		JMethod setter = dc.method(JMod.PUBLIC, void.class, "set" + firstToUpper(name));
		JBlock block = setter.body();
		JExpression e = setter.param(type, "data");
		block.assign(f, e);
		JMethod getter = dc.method(JMod.PUBLIC, type, "get" + firstToUpper(name));
		block = getter.body();
		block._return(f);
	}

	protected void generateConstraints(JFieldVar f, List<Map> constraints) {
		if (constraints == null) {
			return;
		}
		Map<String, Object[]> cMap = initConstraints();
		Map<String, Boolean> processed = new HashMap<String, Boolean>();
		while (true) {
			List<Map> cl = getNextConstraint(constraints, processed);
			if (cl.size() == 0) {
				break;
			}
			if (cl.size() == 1) {
				Map<String, Object> c = cl.get(0);
				String a = (String) c.get("annotation");
				Object[] params = cMap.get(a);
				Object p1 = (c.get("parameter1") instanceof JSONNull) ? null : c.get("parameter1");
				Object p2 = (c.get("parameter2") instanceof JSONNull) ? null : c.get("parameter2");
				String msg = (c.get("message") instanceof JSONNull) ? null : (String) c.get("message");
				if (params == null) {
					return;
				}
				Map<String, Object> pmap = parseParameter(p1, p2, msg, params);
				Class annoClass = (Class) params[0];
				JAnnotationUse au = f.annotate(annoClass);
				for (String key : pmap.keySet()) {
					if (pmap.get(key) instanceof Integer) {
						au = au.param(key, (Integer) pmap.get(key));
					}
					if (pmap.get(key) instanceof Double) {
						au = au.param(key, (Double) pmap.get(key));
					}
					if (pmap.get(key) instanceof String) {
						au = au.param(key, (String) pmap.get(key));
					}
					if (pmap.get(key) instanceof Pattern.Flag) {
						au = au.param(key, (Pattern.Flag) pmap.get(key));
					}
				}
			} else {
				String a = (String) cl.get(0).get("annotation");
				Object[] params = cMap.get(a);
				Object[] paramsList = cMap.get(a + ".List");
				JAnnotationUse au = f.annotate((Class) paramsList[0]);
				JAnnotationArrayMember aam = au.paramArray("value");
				for (Map c : cl) {
					Object p1 = (c.get("parameter1") instanceof JSONNull) ? null : c.get("parameter1");
					Object p2 = (c.get("parameter2") instanceof JSONNull) ? null : c.get("parameter2");
					String msg = (c.get("message") instanceof JSONNull) ? null : (String) c.get("message");
					Map<String, Object> pmap = parseParameter(p1, p2, msg, params);
					au = aam.annotate((Class) params[0]);
					for (String key : pmap.keySet()) {
						if (pmap.get(key) instanceof Integer) {
							au = au.param(key, (Integer) pmap.get(key));
						}
						if (pmap.get(key) instanceof Double) {
							au = au.param(key, (Double) pmap.get(key));
						}
						if (pmap.get(key) instanceof String) {
							au = au.param(key, (String) pmap.get(key));
						}
						if (pmap.get(key) instanceof Pattern.Flag) {
							au = au.param(key, (Pattern.Flag) pmap.get(key));
						}
					}
				}
			}
		}
	}

	protected List<Map> getNextConstraint(List<Map> constraints, Map<String, Boolean> processed) {
		List<Map> ret = new ArrayList();
		for (Map<String, String> constraint : constraints) {
			String annotation = constraint.get("annotation");
			if (processed.get(annotation) != null) {
				continue;
			} else {
				for (Map<String, String> c : constraints) {
					String a = c.get("annotation");
					if (a.equals(annotation)) {
						ret.add(c);
					}
				}
				processed.put(annotation, true);
				return ret;
			}
		}
		return ret;
	}

	protected Map<String, Object> parseParameter(Object p1, Object p2, String msg, Object[] params) {
		Map<String, Object> paramMap = new HashMap();
		try {
			Object[] values = { p1, p2 };
			for (int i = 0; i < params.length; i++) {
				Object val = "";
				if (values != null && i < values.length && values[i] != null) {
					val = values[i];
				}
				String param = "message:o";
				if (i < (params.length - 1)) {
					param = (String) params[i + 1];
				}
				int ind = param.indexOf(":");
				if (ind != -1) {
					String suffix = param.substring(ind + 1);
					Object value = getValue(suffix, val);
					if (value != null) {
						param = param.substring(0, ind);
						paramMap.put(param, value);
					}
				} else {
					paramMap.put(param, val);
				}
			}
			if (msg != null && msg.trim().length() > 0) {
				paramMap.put("message", msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return paramMap;
	}

	protected Object getValue(String s, Object value) {
		boolean optional = s.indexOf("o") != -1;
		if (s.indexOf("d") != -1) {
			return getDecimal(value);
		}
		if (s.indexOf("i") != -1) {
			return getInteger(value);
		}
		if (s.indexOf("f") != -1) {
			return getFlags((String) value);
		}
		if ("".equals(value) && optional) {
			return null;
		}
		return value;
	}

	protected Double getDecimal(Object value) {
		try {
			if (value instanceof String) {
				return Double.parseDouble((String) value);
			}
			if (value instanceof Double) {
				return (Double) value;
			}
		} catch (Exception e) {
		}
		return null;
	}

	protected Integer getInteger(Object value) {
		try {
			if (value instanceof Integer) {
				return (Integer) value;
			}
			if (value instanceof String) {
				return Integer.parseInt((String) value);
			}
		} catch (Exception e) {
		}
		return null;
	}

	protected Pattern.Flag getFlags(String s) {
		Pattern.Flag pf = null;
		if (s.indexOf("i") != -1) {
			pf = Flag.CASE_INSENSITIVE;
		} else if (s.indexOf("d") != -1) {
			pf = Flag.DOTALL;
		}
		return pf;
	}

	public Map<String, Object[]> initConstraints() {
		Map<String, Object[]> mMap = new HashMap<String, Object[]>();
		mMap.put("AssertFalse", new Object[] { AssertFalse.class });
		mMap.put("AssertFalse.List", new Object[] { AssertFalse.List.class });
		mMap.put("AssertTrue", new Object[] { AssertTrue.class });
		mMap.put("AssertTrue.List", new Object[] { AssertTrue.List.class });
		mMap.put("CreditCardNumber", new Object[] { CreditCardNumber.class });
		mMap.put("CreditCardNumber.List", new Object[] { CreditCardNumber.List.class });
		mMap.put("DecimalMax", new Object[] { DecimalMax.class, "value" });
		mMap.put("DecimalMax.List", new Object[] { DecimalMax.List.class });
		mMap.put("DecimalMin", new Object[] { DecimalMin.class, "value" });
		mMap.put("DecimalMin.List", new Object[] { DecimalMin.List.class });
		mMap.put("Digits", new Object[] { Digits.class, "integer:i", "fraction:i" });
		mMap.put("Digits.List", new Object[] { Digits.List.class });
		mMap.put("Email", new Object[] { Email.class });
		mMap.put("Email.List", new Object[] { Email.List.class });
		mMap.put("Future", new Object[] { Future.class });
		mMap.put("Future.List", new Object[] { Future.List.class });
		mMap.put("Length", new Object[] { Length.class, "min:io", "max:io" });
		mMap.put("Length.List", new Object[] { Length.List.class });
		mMap.put("Max", new Object[] { Max.class, "value:i" });
		mMap.put("Max.List", new Object[] { Max.List.class });
		mMap.put("Min", new Object[] { Min.class, "value:i" });
		mMap.put("Min.List", new Object[] { Min.List.class });
		mMap.put("NotBlank", new Object[] { NotBlank.class });
		mMap.put("NotBlank.List", new Object[] { NotBlank.List.class });
		mMap.put("NotEmpty", new Object[] { NotEmpty.class });
		mMap.put("NotEmpty.List", new Object[] { NotEmpty.List.class });
		mMap.put("NotNull", new Object[] { NotNull.class });
		mMap.put("NotNull.List", new Object[] { NotNull.List.class });
		mMap.put("Null", new Object[] { Null.class });
		mMap.put("Null.List", new Object[] { Null.List.class });
		mMap.put("Past", new Object[] { Past.class });
		mMap.put("Past.List", new Object[] { Past.List.class });
		mMap.put("Pattern", new Object[] { Pattern.class, "regexp", "flags:fo" });
		mMap.put("Pattern.List", new Object[] { Pattern.List.class });
		mMap.put("Range", new Object[] { Range.class, "min:io", "max:io" });
		mMap.put("Range.List", new Object[] { Range.List.class });
		// mMap.put("ScriptAssert",				new Object[] { ScriptAssert.class,		"script","lang","alias:o"});
		// mMap.put("ScriptAssert.List",		new Object[] { ScriptAssert.List.class});
		mMap.put("Size", new Object[] { Size.class, "min:io", "max:io" });
		mMap.put("Size.List", new Object[] { Size.List.class });
		mMap.put("URL", new Object[] { URL.class });
		mMap.put("URL.List", new Object[] { URL.List.class });
		return mMap;
	}

	private String firstToUpper(String s) {
		String fc = s.substring(0, 1);
		return fc.toUpperCase() + s.substring(1);
	}

	protected List getEntityMetaData(StoreDesc sdesc, String entity) throws Exception {
		List list = m_entityService.getFields(sdesc, entity, false);
		return list;
	}

	private String removePackageName(String s) {
		if (s == null) {
			return s;
		}
		int dot = s.lastIndexOf(".");
		if (dot == -1) {
			return s;
		}
		return s.substring(dot + 1);
	}

	protected List getRelations(StoreDesc sdesc) throws Exception {
		List list = m_entityService.getRelations(sdesc);
		System.out.println("getRelations:" + list);
		return list;
	}

	private String getFQN(StoreDesc sdesc, Map module) {
		String className = m_inflector.getClassName((String) module.get("name"));
		return sdesc.getJavaPackage() + "." + className;
	}

	private String getJoinTableName(String leftmodule, String leftfield, String rightmodule, String rightfield) {
		String ret = removePackageName(leftmodule);
		if (leftfield != null) {
			ret += "_" + leftfield;
		}
		ret += "_" + removePackageName(rightmodule);
		if (rightfield != null) {
			ret += "_" + rightfield;
		}
		return ret.toLowerCase();
	}

	private boolean getBoolean(Object o, boolean def) {
		try {
			boolean b = (Boolean) o;
			return b;
		} catch (Exception e) {
		}
		return def;
	}

	@Reference(dynamic = true)
	public void setNucleusService(NucleusService paramNucleusService) {
		this.m_nucleusService = paramNucleusService;
		System.out.println("SourceGenServiceImpl.setNucleusService:" + paramNucleusService);
	}

	@Reference
	public void setEntityService(EntityService paramEntityService) {
		m_entityService = paramEntityService;
		System.out.println("SourceGenServiceImpl.setEntityService:" + paramEntityService);
	}

	@Reference
	public void setScriptEngineService(ScriptEngineService paramService) {
		m_scriptEngineService = paramService;
		System.out.println("SourceGenServiceImpl.setScriptEngineService:" + paramService);
	}
}
