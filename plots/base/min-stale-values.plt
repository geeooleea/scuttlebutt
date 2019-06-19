set terminal x11 7

set xlabel "Rounds"
set ylabel "minumum # of stale mappings"

set xtics 0,20,140
set ytics 500

set key on

plot "res/".exp."/precise-oldest" using 1:9 title "Precise-oldest" with linespoints, \
    "res/".exp."/precise-newest" using 1:9 title "Precise-newest" with linespoints, \
    "res/".exp."/scuttle-depth" using 1:9 title "Scuttle-depth" with linespoints, \
    "res/".exp."/scuttle-breadth" using 1:9 title "Scuttle-breadth" with linespoints, \
	"res/".exp."/scuttle-mixed" using 1:9 title "Scuttle-mixed 20%" with linespoints
