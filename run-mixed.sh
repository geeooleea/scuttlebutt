#!/bin/bash

# $1 holds folder with configuration files
# $2 holds output folder

for i in $(seq 0 100); do
	sed -i 's/protocol.prec.percent .*/protocol.prec.percent '$i'/g' $1/conf-precise-mixed
	java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator $1/conf-precise-mixed > $2/precise-mixed-$i
done

