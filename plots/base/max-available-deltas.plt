set terminal x11 6

set xlabel "Rounds"
set ylabel "Maximum available deltas"

set xtics 0,20,140
set ytics 200

set key on

plot "res/".exp."/precise-oldest" using 1:7 title "Precise-oldest max differences" with linespoints, \
	"res/".exp."/precise-newest" using 1:7 title "Precise-newest max differences" with linespoints, \
	"res/".exp."/scuttle-depth" using 1:7 title "Scuttle-depth max differences" with linespoints, \
	"res/".exp."/scuttle-breadth" using 1:7 title "Scuttle-breadth max differences" with linespoints
