#!/bin/bash

# $1 holds folder with configuration files
# $2 holds output folder

CONF="conf/scuttle-mixed2"
RES="res/scuttle-mixed2"

echo $CONF
echo $RES

for i in {0..100..10}
  do 
	sed -i 's/protocol.app.percent .*/protocol.app.percent '$i'/g' conf/scuttle-mixed2/*
	java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator $CONF/conf-precise-oldest > $RES/precise-oldest-$i
	java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator $CONF/conf-precise-newest > $RES/precise-newest-$i
	java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator $CONF/conf-depth > $RES/scuttle-depth-$i
	java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator $CONF/conf-breadth > $RES/scuttle-breadth-$i
	java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator $CONF/conf-mixed > $RES/scuttle-mixed-$i
done

