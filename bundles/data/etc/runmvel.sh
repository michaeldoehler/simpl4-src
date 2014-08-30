export CLASSPATH=.
CLASSPATH=$CLASSPATH:../../repository/mvel2-2.1.RC1.jar
CLASSPATH=$CLASSPATH:build/classes
java -classpath $CLASSPATH org.ms123.common.data.scripting.MVELDataDsl
