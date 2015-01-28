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

import flexjson.JSONDeserializer;
import java.util.*;
import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.*;

/**
 */
@SuppressWarnings("unchecked")
public class ClassBuilder {

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected ClassFile mClassFile;
	protected CtClass mCtClass;


	protected List<Map> mInputShapes;

	protected Map<String, List> mConstraintsMeta;

	public ClassBuilder(ClassLoader loader, List<Map> inputShapes, Map<String, List> constraints) throws Exception{
		mInputShapes = inputShapes;
		mConstraintsMeta = constraints;

		ClassPool cp = ClassPool.getDefault();
		mCtClass = cp.makeClass("sw.Clazz$"+new Date().getTime());
		mClassFile = mCtClass.getClassFile();
		mClassFile.setVersionToJava5();
		for (Map shape : mInputShapes) {
			definePropertyBeanMethods(shape);
		}
	}

	public Class getClazz() throws Exception{
		//mCtClass.writeFile("/tmp");
		Class c = mCtClass.toClass();
		mCtClass.detach();
		return c;
	}

	protected void definePropertyBeanMethods(Map shape) throws Exception{
		Class type = ConstraintsUtils.getType(shape);
		if (type == null) {
			return;
		}
		String name = getName(shape);
		if (name == null || "".equals(name.trim())) {
			//System.out.println("Shape:" + shape);
			return;
		}
		String simpleTypeName = type.getName();
		String getName = ConstraintsUtils.makeReadMethodName(name,type);
		String setName = ConstraintsUtils.makeWriteMethodName(name);


		CtField field = new CtField(ClassPool.getDefault().get(type.getName()), name, mCtClass);
		field.setModifiers(AccessFlag.PRIVATE);
		mCtClass.addField(field);
		AnnotationsAttribute attribute = createAnnotations(shape);
		field.getFieldInfo().addAttribute(attribute);

		CtMethod nameGetMethod = CtNewMethod.make("public "+simpleTypeName+" "+getName+"(){return "+name+";}", mCtClass);
		mCtClass.addMethod(nameGetMethod);

		CtMethod nameSetMethod = CtNewMethod.make("public void "+setName+" ("+simpleTypeName+" "+name+"){this."+name+"="+name+";}", mCtClass);
		mCtClass.addMethod(nameSetMethod);

	}


	protected String getName(Map element) {
		Map properties = (Map) element.get("properties");
		return ((String) properties.get("xf_id")).toLowerCase();
	}


	private AnnotationsAttribute createAnnotations(Map shape) {
		AnnotationsAttribute attribute = new AnnotationsAttribute(mClassFile.getConstPool(), AnnotationsAttribute.visibleTag);
		Map<String, List> consDataMap = ConstraintsUtils.getConstraintsData(shape);
		if (consDataMap == null) {
			return attribute;
		}
		String stype = ConstraintsUtils.getStringType(shape);
		List<Map> consMetaList = mConstraintsMeta.get(stype);
		for (String annoName : consDataMap.keySet()) {
			Map consMetaMap = ConstraintsUtils.getConsMetaMap(annoName, consMetaList);
			List consDataList = consDataMap.get(annoName);
			if (consDataList.size() == 0 || ((Boolean) consDataList.get(0)) == false){
				continue;
			}
			//System.out.println("annoName:" + annoName);
			//System.out.println("consMetaMap:" + consMetaMap);
			//System.out.println("consDataList:" + consDataList);
			if( consMetaMap == null){
				continue;
			}
			String clazzName = (String) consMetaMap.get("clazz");
			List<Map> options = (List) consMetaMap.get("options");
			if (clazzName.indexOf('.') == -1) {
				String pkg = (String) consMetaMap.get("pkg");
				if (pkg == null) {
					pkg = "javax.validation.constraints";
				}
				clazzName = pkg + "." + clazzName;
			}
			Annotation annotation = new Annotation(clazzName,mClassFile.getConstPool());
			for (int j = 0; options != null && j < options.size(); j++) {
				Map o = (Map) options.get(j);
				String typeChar = ConstraintsUtils.getTypeChar((String) o.get("type"));
				String optname = (String) o.get("optname");
				if (optname == null) {
					optname = "value";
				}
				if( consDataList.get(j + 1) != null){
					MemberValue mv = getMemberValue((String) o.get("type"), consDataList.get(j + 1));
					annotation.addMemberValue(optname,mv);
				}
			}
			attribute.addAnnotation(annotation);
		}
		//System.out.println("Annotations:" + attribute);
		return attribute;
	}

	protected MemberValue getMemberValue(String type,Object value) {
		type = type.toLowerCase();
		if( type.indexOf(",")!=-1){
			type = type.split(",")[1];
		}
		//System.out.println("getMemberValue:"+type+",value:"+value+"/class:"+value.getClass());
		if ("double".equals(type) || "decimal".equals(type))
			return new DoubleMemberValue(getDouble(value), mClassFile.getConstPool());
		if ("string".equals(type))
			return new StringMemberValue(getString(value),mClassFile.getConstPool());
		if ("int".equals(type))
			return new IntegerMemberValue(mClassFile.getConstPool(),getInteger(value));
		if ("long".equals(type))
			return new LongMemberValue(getLong(value), mClassFile.getConstPool());
		return new StringMemberValue(getString(value),mClassFile.getConstPool());
	}
	private String getString(Object value){
		return String.valueOf(value);
	}
	private double getDouble(Object value){
		return ((Number)value).doubleValue();
	}
	private long getLong(Object value){
		return ((Number)value).longValue();
	}
	private int getInteger(Object value){
		return ((Number)value).intValue();
	}

}
