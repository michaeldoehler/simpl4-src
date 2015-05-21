#!/bin/sh

#rm -rf bower_components  
#bower install

#sed -i '/slide-up-scale-down-animation.html/d'  bower_components/neon-animation/neon-animations.html

vulcanize --config vulcan.config --inline index.html
mv vulcanized.html domelements.html
