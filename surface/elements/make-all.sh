#!/bin/sh

bower install

rm -f index.html
ALL=`fgrep -l polymer-element */*/*.html`

for i in $ALL
do
  echo '<link rel="import" href="'$i'">' >>index.html
done

vulcanize --inline index.html
mv vulcanized.html elements.html
