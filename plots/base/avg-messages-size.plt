set terminal x11 3

set xlabel "Rounds"
set ylabel "Average message size"

set xtics 0,20,140
set ytics 200

set key on

plot "res/base/precise-oldest" using 1:2 title "Precise-oldest message size" with linespoints, \
    	"res/base/precise-newest" using 1:2 title "Precise-newest message size" with linespoints, \
    	"res/base/scuttle-depth" using 1:2 title "Scuttle-depth message size" with linespoints, \
    	"res/base/scuttle-breadth" using 1:2 title "Scuttle-breadth message size" with linespoints
