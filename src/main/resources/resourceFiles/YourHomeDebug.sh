#!/bin/bash
SCRIPT=$(readlink -f "$0")
DIR=$(dirname "$SCRIPT")
JAR="$( ls -v "$DIR/bin" | grep Home | tail -n 1 )"
(
    echo "Starting HomeServer from version $DIR/bin/$JAR";
    until java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n -jar "$DIR/bin/$JAR"; do
		if [ "$?" = "121" ]; then
		   echo "YourHome Server is restarting" >&2
		else
		   echo "YourHome Server is closed" >&2
		   break;
		fi;
    done
) &


