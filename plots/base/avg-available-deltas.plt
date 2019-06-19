set terminal x11 4

set xlabel "Rounds"
set ylabel "Average available deltas"

set xtics 0,20,140
set ytics 200

set key on

plot "res/".exp."/precise-oldest" using 1:6 title "Precise-oldest differences" with linespoints, \
	"res/".exp."/precise-newest" using 1:6 title "Precise-newest differences" with linespoints, \
	"res/".exp."/scuttle-depth" using 1:6 title "Scuttle-depth differences" with linespoints, \
	"res/".exp."/scuttle-breadth" using 1:6 title "Scuttle-breadth differences" with linespoints
