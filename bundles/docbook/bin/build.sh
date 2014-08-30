#!/bin/sh

#CLASSPATH=.
#for i in lib/*.jar
#do
#  CLASSPATH=$CLASSPATH:$i
#done
#echo $CLASSPATH

rm -rf src/main/java/org/ms123/common/docbook/jaxb
bin/xjc.sh -p org.ms123.common.docbook.jaxb -d src/main/java -nv   etc/docbook.xsd

#javac -encoding ISO-8859-1 -cp $CLASSPATH `find org -name "*.java"`

#rm -f docbook.[bj]ar
#zip -r docbook.jar org
#bnd wrap docbook.jar 
#rm docbook.jar
