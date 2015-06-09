#!/bin/sh

WHO=tparker@usgs.gov
HOST=`hostname`
WHAT="The pensive instance on $HOST has been started."
BASE=/usr/local/pensive
PID_FILE=/tmp/pensive.pid
NOT_RUNNING=0

ckStatus() {
	if [ -f $PID_FILE ]; then
		ps -fp `cat $PID_FILE` | grep java > /dev/null
		NOT_RUNNING=$?
	else
		NOT_RUNNING=1
	fi
}

case "$1" in
'start')
	ckStatus

	if [ $NOT_RUNNING -gt 0 ]; then
		rm -f $PID_FILE
        echo $WHAT | mail -s "Starting subnetogram on $HOST" $WHO

		cd $BASE
		/usr/local/java/bin/java -jar pensive.jar > /dev/null 2>&1 &
		echo $! > $PID_FILE
	else
		echo "Pensive is already running. No action taken."
		exit 1
	fi

	;;

'stop')
	kill -9 `cat $PID_FILE`
	rm $PID_FILE
    ;;

'restart')
	$0 stop
	$0 start
        ;;

'status')
	ckStatus
	if [ $NOT_RUNNING -gt 0 ]; then
		echo "Pensive is not running"
	else
		echo "Pensive is running"
	fi
	exit $NOT_RUNNING
	;;

*)
        echo "Usage: $0 { start | stop | status | restart }"
        exit 1
        ;;
esac
