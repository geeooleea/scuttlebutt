java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator conf/conf-breadth > ../thesis/plots/scuttle-breadth

java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator conf/conf-depth > ../thesis/plots/scuttle-depth

java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator conf/conf-precise-oldest > ../thesis/plots/precise-oldest 

java -cp "lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:scuttlebutt.jar" peersim.Simulator conf/conf-precise-newest > ../thesis/plots/precise-newest
