#!/bin/sh

#bower install
#rm -rf bower_components/polymer/  
#find . -name "*.html" -exec sed -i "/polymer.html/d" {} \;


vulcanize --inline index.html
mv vulcanized.html elements.html
