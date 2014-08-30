CLASSPATH=
for i in lib/*.[bj]ar
do
  CLASSPATH=$CLASSPATH:$i
done

java -cp $CLASSPATH ImportConverter 0.85 0.65 0.84 0.80

