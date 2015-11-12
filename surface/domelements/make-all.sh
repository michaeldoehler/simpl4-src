#!/bin/sh

#rm -rf bower_components
if [ ! -d "bower_components" ] ; then
	 if [ -f "bin/bower_components.tgz" ] ; then
		 tar xf bin/bower_components.tgz
	 else
		 echo "No bower"
		 bower install
		 
		 sed -i '/slide-up-scale-down-animation.html/d'  bower_components/neon-animation/neon-animations.html
		 sed -i 's!value: Polymer.IronOverlayManager!value: function(){ return Polymer.IronOverlayManager}!' bower_components/iron-overlay-behavior/*.html
		 sed -i '/    html {/,+14d'  bower_components/mat-typography/mat-typography.html
		 sed -e '/target.insertBefore/ {' -e 'r polymer.patch' -e 'd' -e '}' -i bower_components/polymer/polymer.html


		sed -i 's/console.\(log\|warn\|error\).apply/Function.prototype/' bower_components/polymer/polymer-micro.html
	#	sed -i '/function saveLightChildrenIfNeeded/a if( node == null){ console.log("saveLightChildrenIfNeeded:node is null"); return; }' bower_components/polymer/polymer-mini.html
		 sed -e '/saveLightChildrenIfNeeded(\s*c.parentNode\s*)/ {' -e 'r polymer2.patch' -e 'd' -e '}' -i bower_components/polymer/polymer-mini.html
		patch bower_components/iron-overlay-behavior/iron-overlay-behavior.html < polymer3.patch
	fi 
fi

if [ -e "/usr/bin/vulcanize" ] ; then
	/usr/bin/vulcanize  --strip-comments --inline-css --inline-scripts --inline index.html >domelements.html
else
	cp bin/_domelements.html.gz domelements.html.gz
fi
