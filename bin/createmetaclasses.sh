#!/bin/bash

#simplflo-deployed/bin/start.sh start


if [ -z "$SWDIR" ] ; then
  export SWDIR=`pwd`
  echo "SWDIR not set, using $SWDIR"
fi

if [ ! -e "${SWDIR}/server/felix/config.ini" ] ; then
  echo "Not a simplflo installation"
  exit 1
fi
export PATH=$SWDIR/bin:$PATH

echo "PATH:$PATH"

mkdir -p $SWDIR/gitrepos/global_data
cd $SWDIR/gitrepos/global_data
git init

cd ..
git clone git://swstore.ms123.org/global.git





touch $SWDIR/noauth
sw generateclasses -storeId global_data
sw generateclasses -storeId global_meta
rm $SWDIR/noauth

rm -fr $SWDIR/gitrepos
rm -fr $SWDIR/workspace/activiti


#simplflo-deployed/bin/start.sh stop
