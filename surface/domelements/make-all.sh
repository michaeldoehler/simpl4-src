#!/bin/sh

#rm -rf bower_components
if [ ! -d "bower_components" ] ; then
   echo "No bower"
   bower install
   
   sed -i '/slide-up-scale-down-animation.html/d'  bower_components/neon-animation/neon-animations.html
   sed -i 's!value: Polymer.IronOverlayManager!value: function(){ return Polymer.IronOverlayManager}!' bower_components/iron-overlay-behavior/*.html
fi

/usr/bin/vulcanize  --strip-comments --inline-css --inline-scripts --inline index.html >domelements.html
