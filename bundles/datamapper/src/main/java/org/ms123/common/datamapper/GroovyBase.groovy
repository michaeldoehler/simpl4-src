/*
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
package org.ms123.common.datamapper;


import java.io.*;
import java.util.*;
import groovy.lang.*;
import java.text.*;
import java.util.concurrent.TimeUnit;
import org.apache.commons.beanutils.converters.*;
import org.apache.commons.beanutils.*;



public abstract class GroovyBase extends Script implements Constants {
	private Map m_properties = [:];

	public String dec2(Object o){
		if( o instanceof Integer){
			o = ((Integer)o).floatValue();
		}
		return String.format('%1$,.2f',o);
	}
	public String today(Object o){
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat formater = new SimpleDateFormat();
		return formater.format(cal.getTime() );
	}

	private Map converters = [
		"str2bool":[ctClazz:BooleanConverter.class,toClazz:Boolean.class],
		"str2boolean":[ctClazz:BooleanConverter.class,toClazz:Boolean.class],
		"str2date":[ctClazz:DateConverter.class,toClazz:Date.class],
		"str2decimal":[ctClazz:BigDecimalConverter.class,toClazz:BigDecimal.class],
		"str2double":[ctClazz:DoubleConverter.class,toClazz:Double.class],
		"str2integer":[ctClazz:IntegerConverter.class,toClazz:Integer.class],
		"str2long":[ctClazz:LongConverter.class,toClazz:Long.class],
		"str2calendar":[ctClazz:CalendarConverter.class,toClazz:Calendar.class],

		"string2boolean":[ctClazz:BooleanConverter.class,toClazz:Boolean.class],
		"string2date":[ctClazz:DateConverter.class,toClazz:Date.class],
		"string2decimal":[ctClazz:BigDecimalConverter.class,toClazz:BigDecimal.class],
		"string2double":[ctClazz:DoubleConverter.class,toClazz:Double.class],
		"string2integer":[ctClazz:IntegerConverter.class,toClazz:Integer.class],
		"string2long":[ctClazz:LongConverter.class,toClazz:Long.class],
		"string2calendar":[ctClazz:CalendarConverter.class,toClazz:Calendar.class],



		"num2str":[ctClazz:LongConverter.class,toClazz:String.class],
		"num2bool":[ctClazz:BooleanConverter.class,toClazz:Boolean.class],
		"num2boolean":[ctClazz:BooleanConverter.class,toClazz:Boolean.class],

		"number2string":[ctClazz:LongConverter.class,toClazz:String.class],
		"number2boolean":[ctClazz:BooleanConverter.class,toClazz:Boolean.class],


		"long2date":[ctClazz:DateConverter.class,toClazz:Date.class],
		"long2integer":[ctClazz:LongConverter.class,toClazz:Integer.class],
		"long2calendar":[ctClazz:CalendarConverter.class,toClazz:Calendar.class],

		"double2integer":[ctClazz:DoubleConverter.class,toClazz:Integer.class],
		"double2long":[ctClazz:DoubleConverter.class,toClazz:Long.class],

		"decimal2integer":[ctClazz:BigDecimalConverter.class,toClazz:Integer.class],
		"decimal2long":[ctClazz:BigDecimalConverter.class,toClazz:Long.class],
		"decimal2double":[ctClazz:BigDecimalConverter.class,toClazz:Double.class],


		"bool2num":[ctClazz:IntegerConverter.class,toClazz:Integer.class],
		"boolean2number":[ctClazz:IntegerConverter.class,toClazz:Integer.class],


		"date2str":[ctClazz:DateConverter.class,toClazz:String.class],
		"date2num":[ctClazz:LongConverter.class,toClazz:Long.class],
		"date2string":[ctClazz:DateConverter.class,toClazz:String.class],
		"date2number":[ctClazz:LongConverter.class,toClazz:Long.class],


		"date2long":[ctClazz:LongConverter.class,toClazz:Long.class],
		"date2calendar":[ctClazz:DateConverter.class,toClazz:Calendar.class],

		"calendar2date":[ctClazz:CalendarConverter.class,toClazz:Date.class],
		"calendar2long":[ctClazz:LongConverter.class,toClazz:Long.class],

		"calendar2num":[ctClazz:LongConverter.class,toClazz:Long.class],
		"calendar2str":[ctClazz:CalendarConverter.class,toClazz:String.class],

		"calendar2number":[ctClazz:LongConverter.class,toClazz:Long.class],
		"calendar2string":[ctClazz:CalendarConverter.class,toClazz:String.class]
	]

	public void setProperty(String name,Object value){
		//System.out.println("GroovyBase.setProperty:"+value);
		m_properties.put(name,value);
	}
	public Object getProperty(String name){
		//System.out.println("GroovyBase.getProperty:"+name);
		Object o = m_properties.get(name);
		if( o != null ) return o;
		return super.getProperty(name);
	}

	def propertyMissing(String name) { null }

	def methodMissing(String name, args) {
		Map c = converters[name];
		if( c == null){
			throw new RuntimeException("<b>Datamapper("+getConfigName()+"):</b>Missing method:\""+name +"\" in GroovyBase")
		}
		if( args[0] == null ){
			return null;
		}
		String argsStr = "("+args[0];
		def conv = c.ctClazz.newInstance();
		if( conv instanceof NumberConverter || conv instanceof DateTimeConverter){
			for( int i=1; i < args.length;i++){
				Object arg = args[i];
				if( arg instanceof Locale){
					conv.setLocale(arg);
					argsStr += ","+arg;
				}
				if( arg instanceof String){
					conv.setPattern(arg);
					argsStr += ","+arg;
				}
			}
		}
		argsStr = ")";

		try{
			//print("calling:"+name+argsStr);
			def ret = conv.convert( c.toClazz, args[0]);
			//println(" -> "+ret+"/"+ret.getClass());
			return ret;
		}catch(Exception e){
			throw new RuntimeException("<b>Datamapper("+getConfigName()+"):</b>"+constructErrorMessage(e,name));
		}
	}

	private String getConfigName(){
		return getProperty(DATAMAPPER_CONFIG);
	}

	private String constructErrorMessage(Exception e,String method){
		return e.getMessage()+": ("+method+" -> "+ this.getBinding().getVariable(SCRIPT_NAME)+")";
	}

	public long 	toDays(long duration){
		return TimeUnit.MILLISECONDS.toDays(duration);
	}
	public long	toHours(long duration){
		return TimeUnit.MILLISECONDS.toHours(duration);
	}
	public long	toMicros(long duration){
		return TimeUnit.MILLISECONDS.toMicros(duration);
	}
	public long	toMillis(long duration){
		return TimeUnit.MILLISECONDS.toMillis(duration);
	}
	public long	toMinutes(long duration){
		return TimeUnit.MILLISECONDS.toMinutes(duration);
	}
	public long	toNanos(long duration){
		return TimeUnit.MILLISECONDS.toNanos(duration);
	}
	public long	toSeconds(long duration){
		return TimeUnit.MILLISECONDS.toSeconds(duration);
	}
}


