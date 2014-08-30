
import org.apache.commons.beanutils.converters.*;
import org.apache.commons.beanutils.*;
import java.util.Locale;
public final class Main {

	private Main() {
	}

	public static void main(String[] args) throws Exception {
System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		Main m = new Main();
	/*	m.str2bool("true");
		m.str2bool("yes");
		m.str2bool("0");
		m.str2date("12.10.1986");
		m.str2date("12.10.1986", "dd.MM.yyyy");
		m.str2calendar("12.10.1986", "dd.MM.yyyy");
		m.str2decimal("0.11");
		m.str2decimal("0,11");
		m.str2decimal("0,11", "0.00");
		m.str2decimal("0.11", "0.00", Locale.US);
		m.str2decimal("0.11", "0.00", Locale.GERMAN);

		m.str2double("0.11");
		m.str2double("0,11");
		m.str2double("0,11", "0.00");
		m.str2double("0.11", "0.00", Locale.US);
		m.str2double("0.11", "0.00", Locale.GERMAN);

		m.num2str(0.11);
		m.num2str(11.1, "0.00");
		m.num2str(112345.1f);
		m.num2str(new BigDecimal("111"));

		m.num2bool(1);


		m.long2date(1111111111111);
		m.long2integer(1111111111111);
		m.long2integer(1111111111.123);
		m.double2integer(1111111111.123);
		m.decimal2double(1111111111111.123);
		m.decimal2long(1111111111111.123);
		m.bool2num(true);
		m.bool2num(false);*/
		m.date2str(new Date());
		m.date2num(new Date());
		m.date2long(null);
		/*m.date2calendar(new Date());

		m.calendar2date(Calendar.getInstance());
		m.calendar2num(Calendar.getInstance());
		m.calendar2long(Calendar.getInstance());
		m.calendar2str(Calendar.getInstance(), "dd.MM.yyyy");*/
	}
	public static Long str2long(Object s){
		try{
			LongConverter converter = new LongConverter();
			return (Long)converter.convert(Long.class,s);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	private Map converters = [
 		"str2bool":[ctClazz:BooleanConverter.class,toClazz:Boolean.class],
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
	def methodMissing(String name, args) {
		//System.out.println("methodMissing:"+name);
		Map c = converters[name];
		//System.out.println("c:"+c);
		if( c == null){
			throw new RuntimeException("Missing method:"+name)
		}
		//System.out.println("ctClazz:"+c.ctClazz);
		def conv = c.ctClazz.newInstance();
		for( int i=1; i < args.length;i++){
			Object arg = args[i];
			if( arg instanceof Locale){
				conv.setLocale(arg);
			}
			if( arg instanceof String){
				conv.setPattern(arg);
			}
		}

		try{
			System.out.print("calling:"+name+"("+args[0]+")");
			def ret = conv.convert( c.toClazz, args[0]);
			System.out.println(" -> "+ret+"/"+ret.getClass());
			return ret;
		}catch(Exception e){
			println(" -> Error:"+e.getMessage());
		}
	}
}

