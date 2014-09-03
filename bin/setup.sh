#!/bin/bash

SIMPL4DIR=`pwd`
echo "Setup in $SIMPL4DIR"

if [ ! -e "${SIMPL4DIR}/server/felix/config.ini" ] ; then
  echo "Not a simpl4 installation"
  exit 1
fi

cd $SIMPL4DIR
tar xf gitrepos.tgz
mkdir -p gitrepos/global_data/settings

LOGDIR=$SIMPL4DIR/log
mkdir -p log

sed    "s!_BASEDIR_!$SIMPL4DIR!g" etc/logging.config.tpl >  etc/logging.config
sed -i "s!_LOGDIR_!$LOGDIR!g" etc/logging.config
sed    "s!_BASEDIR_!$SIMPL4DIR!g" etc/logback.xml.tpl > etc/logback.xml
sed -i "s!_LOGDIR_!$LOGDIR!g" etc/logback.xml
sed    "s!_BASEDIR_!$SIMPL4DIR!g" etc/config/org/ops4j/pax/logging.config.tpl > etc/config/org/ops4j/pax/logging.config
sed    "s!_BASEDIR_!$SIMPL4DIR!g" server/run.bat.tpl > server/run.bat
cp etc/branding.example etc/branding


mkdir -p workspace/configadm/org/ops4j/pax/
cp etc/logging.config workspace/configadm/org/ops4j/pax/


