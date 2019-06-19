set terminal x11 2

set xlabel "Rounds"
set ylabel "# of stale mappings"

set xtics 0,20,140
set ytics 20

set key on

exp="scuttle-mixed2"

plot for [n=0:10] "res/".exp."/precise-oldest-".(n*10) using 1:2 title "Precise-oldest ".(n*10)."%" with linespoints
replot for [n=0:10] "res/".exp."/precise-newest-".(n*10) using 1:2 title "Precise-newest ".(n*10)."%" with linespoints
replot for [n=0:10] "res/".exp."/scuttle-depth-".(n*10) using 1:2 title "Scuttle-depth ".(n*10)."%" with linespoints
replot for [n=0:10] "res/".exp."/scuttle-breadth-".(n*10) using 1:2 title "Scuttle-breadth ".(n*10)."%" with linespoints
replot for [n=0:10] "res/".exp."/scuttle-mixed-".(n*10) using 1:2 title "Scuttle-mixed ".(n*10)."%" with linespoints
