#!/bin/sh

# $1 holds folder with configuration files
# $2 holds output folder

java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator $1/conf-breadth > $2/scuttle-breadth

java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator $1/conf-depth > $2/scuttle-depth

java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator $1/conf-precise-oldest > $2/precise-oldest 

java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator $1/conf-precise-newest > $2/precise-newest
