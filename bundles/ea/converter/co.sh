CLASSPATH=
for i in lib/*.[bj]ar
do
  CLASSPATH=$CLASSPATH:$i
done
javac -cp $CLASSPATH *.java

echo $CLASSPATH
