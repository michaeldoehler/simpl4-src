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
  echo "Not a simpl4 installation"
  exit 1
fi

if [ $# = 1 ] ; then
  ACTION=$1
else
  ACTION=start
fi


export JETTY_PORT=80
export CONTROL_PORT=8070
export START_CONSOLE=false

cd $SWDIR
export PATH=.:/opt/java/bin:$PATH

case "$ACTION" in
   start)
      echo  "Starting: $SWDIR"
      cd server
      run.sh >save 2>&1 &
      echo
   ;;

   stop)
      echo -n "Stopping: $SWDIR:"
      for i in `ps ax | grep "org.apache.felix.main.Main" | grep -v grep | grep -v stop | sed 's/[ ]*//' | cut -d" " -f1`
      do
         echo -n $i
         kill  $i
      done
      sleep 2
      for i in `ps ax | grep "org.apache.felix.main.Main" | grep -v grep | grep -v stop | sed 's/[ ]*//' | cut -d" " -f1`
      do
         echo -n $i
         kill -9  $i
      done
      echo
   ;;

   restart)
      $SWDIR/bin/start.sh stop
      sleep 1
      $SWDIR/bin/start.sh start
      exit $?
   ;;

   *)
      echo "Usage: $SWDIR/bin/start.sh {start|stop|restart}"
      exit 1
   ;;
esac
