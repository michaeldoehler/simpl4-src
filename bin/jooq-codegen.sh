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

#if [ ! -e "${SIMPL4DIR}/server/felix/config.ini" ] ; then
#  echo "Not a simpl4 installation"
#  exit 1
#fi

cd $SIMPL4DIR

CLASSPATH="."
for i in server/bundles/*.jar
do
  CLASSPATH=$CLASSPATH:$i
done


CONFIG=
NAMESPACE=global
DATABASENAME=
#########################################################
# usage
#########################################################
usage() {
    echo "usage: $0 -c config.xml"
}

#########################################################
# parameter
#########################################################
shortoptions='c:'
longoptions='config:'
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
      *)
				break
      ;;
   esac
done

if [ -z "$CONFIG" ] ; then
	usage;
	exit 1
fi
echo "================="
echo "generate ->"
echo "================="
echo "SIMPL4DIR:$SIMPL4DIR"
echo "CONFIG=$CONFIG"
echo "NAMESPACE=$NAMESPACE"
echo "DATABASENAME=$DATABASENAME"


java -classpath $CLASSPATH org.jooq.util.GenerationTool $SIMPL4DIR/etc/jooq/$CONFIG
