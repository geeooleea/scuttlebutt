set terminal x11 0

set xlabel "Rounds"
set ylabel "Maximum staleness"

set xtics 0,20,140
set ytics 5

set key on

exp = "scuttle-mixed2"

plot "res/".exp."/precise-oldest-60" using 1:5 title "Precise-oldest 60%" with linespoints, \
    "res/".exp."/precise-newest-60" using 1:5 title "Precise-newest 60%" with linespoints, \
    "res/".exp."/scuttle-depth-60" using 1:5 title "Scuttle-depth 60%" with linespoints, \
    "res/".exp."/scuttle-breadth-60" using 1:5 title "Scuttle-breadth 60%" with linespoints, \
	"res/".exp."/scuttle-mixed-60" using 1:5 title "Scuttle-mixed 60%" with linespoints
