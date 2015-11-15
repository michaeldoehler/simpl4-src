#!/bin/bash

#########################################################
# globals
#########################################################
OVERWRITE=
#########################################################
# functions
#########################################################
#########################################################
# usages
#########################################################
usage() {
   echo "usage: jsstrip.sh [-o] in.js [out.js]"
}
#########################################################
# main
#########################################################
shortoptions='o'
longoptions='overwrite'
getopt=$(getopt -o $shortoptions --longoptions  $longoptions -- "$@")
if [ $? != 0 ]; then
   usage
   exit 1;
fi

eval set -- "$getopt"
while true; do
   case "$1" in
      -h|--help)
         usage
         exit 1
      ;;
      -o|--overwrite)
         OVERWRITE=true
         shift 1
      ;;
      --)
         shift 1
         break
      ;;
      
      *)
         break
      ;;
   esac
done

INFILE=$1
java -jar bin/compiler.jar --charset UTF-8 -O WHITESPACE_ONLY $INFILE >/tmp/.js
if [ -e "../../../tools/lib/js-beautify/js/bin/js-beautify.js" ] ; then
	if [ -n "$OVERWRITE" ] ; then
		 /usr/local/bin/js-beautify   /tmp/.js >$INFILE
	else
		 /usr/local/bin/js-beautify   /tmp/.js
	fi
else
	cp /tmp/.js $INFILE
fi
