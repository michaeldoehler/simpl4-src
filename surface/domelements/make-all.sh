#!/bin/sh

#rm -rf bower_components  
#bower install

#sed -i '/slide-up-scale-down-animation.html/d'  bower_components/neon-animation/neon-animations.html

/usr/bin/vulcanize  --strip-comments --inline-css --inline-scripts --inline index.html >domelements.html
