#!/bin/bash

SWDIR=`pwd`
echo "Setup in $SWDIR"

if [ ! -e "${SWDIR}/server/felix/config.ini" ] ; then
  echo "Not a simpl4 installation"
  exit 1
fi


cd $SWDIR
mkdir -p gitrepos/global_data
cd $SWDIR/gitrepos/global_data
mkdir -p store/data
git init
cp  $SWDIR/etc/users-init-script.sql $SWDIR/gitrepos/global_data/store/data/script.sql
echo '* -text' > $SWDIR/gitrepos/global_data/.gitattributes
git config user.name "admin"
git config user.email "admin@someplace.com"
git add .
git commit -m. -a

cd ..
git clone git://swstore.ms123.org/global.git

cd $SWDIR
mkdir -p gitrepos/global_data/settings

LOGDIR=$SWDIR/log
mkdir -p log

sed    "s!_BASEDIR_!$SWDIR!g" etc/logging.config.tpl >  etc/logging.config
sed -i "s!_LOGDIR_!$LOGDIR!g" etc/logging.config
sed    "s!_BASEDIR_!$SWDIR!g" etc/logback.xml.tpl > etc/logback.xml
sed -i "s!_LOGDIR_!$LOGDIR!g" etc/logback.xml
sed    "s!_BASEDIR_!$SWDIR!g" etc/config/org/ops4j/pax/logging.config.tpl > etc/config/org/ops4j/pax/logging.config
sed    "s!_BASEDIR_!$SWDIR!g" server/run.bat.tpl > server/run.bat
cp etc/branding.example etc/branding


mkdir -p workspace/configadm/org/ops4j/pax/
cp etc/logging.config workspace/configadm/org/ops4j/pax/


