#!/bin/bash

#simpl4-deployed/bin/start.sh start


if [ -z "$SIMPL4DIR" ] ; then
  export SIMPL4DIR=`pwd`
  echo "SIMPL4DIR not set, using $SIMPL4DIR"
fi

if [ ! -e "${SIMPL4DIR}/server/felix/config.ini" ] ; then
  echo "Not a simpl4 installation"
  exit 1
fi
export PATH=$SIMPL4DIR/bin:$PATH

echo "PATH:$PATH"

mkdir -p $SIMPL4DIR/gitrepos/global_data
cd $SIMPL4DIR/gitrepos/global_data
git init

cd ..
git clone git://swstore.ms123.org/global.git





touch $SIMPL4DIR/noauth
sw generateclasses -storeId global_data
sw generateclasses -storeId global_meta
rm $SIMPL4DIR/noauth

rm -fr $SIMPL4DIR/gitrepos
rm -fr $SIMPL4DIR/workspace/activiti


#simpl4-deployed/bin/start.sh stop
