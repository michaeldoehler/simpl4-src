#!/bin/bash
if [ -z "$SWDIR" ] ; then
  thisdir=$(readlink $0)
  if [ "${thisdir}" != "" ]
  then
		 export SWDIR=$(dirname $(cd `dirname $thisdir`; pwd))
  else
     export SWDIR=$(dirname $(cd `dirname $0`; pwd))
  fi
  echo "using $SWDIR"
fi

if [ ! -e "${SWDIR}/server/felix/config.ini" ] ; then
  echo "Not a simplflo installation"
  exit 1
fi



if (( $# != 1 )); then
    echo "createclasses namespace"
		exit 1
fi
NAMESPACE=$1
touch $SWDIR/noauth
$SWDIR/bin/sw generateclasses -storeId ${NAMESPACE}_data
$SWDIR/bin/sw generateclasses -storeId ${NAMESPACE}_meta
rm $SWDIR/noauth


