#!/bin/bash

for i in `find . -name "*.html"| grep -v demo|grep -v test| grep -v index|grep -v metadata|grep -v "/src/"|grep -v "/classes/" `
do
	i=${i:2}
	echo "<link rel=\"import\" href=\"$i\">"
done
