#!/bin/bash

if [ -z "$SIMPL4DIR" ] ; then
   thisdir=$(readlink $0)
   if [ "${thisdir}" != "" ]
   then
      export SIMPL4DIR=$(dirname $(cd `dirname $thisdir`; pwd))
   else
      export SIMPL4DIR=$(dirname $(cd `dirname $0`; pwd))
   fi
   echo "using $SIMPL4DIR"
fi

cd $SIMPL4DIR

CLASSPATH="."
for i in server/bundles/*.jar
do
   CLASSPATH=$CLASSPATH:$i
done


CONFIG=
BUILD=
#########################################################
# usage
#########################################################
usage() {
   echo "usage: $0 [-b] -c config.xml"
}

#########################################################
# parameter
#########################################################
shortoptions='c:b'
longoptions='config:build'
getopt=$(getopt -o $shortoptions --longoptions  $longoptions -- "$@")
if [ $? != 0 ]; then
   usage
   exit 1;
fi

eval set -- "$getopt"
while true; do
   case "$1" in
      -c|--config)
         shift
         CONFIG=$1
         shift
      ;;
      -b|--build)
         BUILD=true
         shift
      ;;
      *)
         break
      ;;
   esac
done

if [ -z "$CONFIG" -a -z "$BUILD" ] ; then
   usage;
   exit 1
fi

BUILDIR=$SIMPL4DIR/workspace/jooq/build
GENDIR=$SIMPL4DIR/workspace/jooq/gen
CONFIGFILE=$SIMPL4DIR/etc/jooq/$CONFIG

echo "BUILDIR:$BUILDIR"
echo "GENDIR:$GENDIR"
echo "CONFIGFILE=$CONFIGFILE"

if [ -n "$CONFIG" ] ; then
   echo "================="
   echo "generate ->"
   echo "================="
   java -classpath $CLASSPATH org.jooq.util.GenerationTool $CONFIGFILE
fi

if [ -n "$BUILD" ] ; then
   echo "================="
   echo "build ->"
   echo "================="
   rm -rf $BUILDIR
   mkdir -p $BUILDIR
   javac -d $BUILDIR -cp $CLASSPATH `find $GENDIR -name "*.java"`
fi
