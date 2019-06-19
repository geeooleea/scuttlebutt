set terminal x11 1

set xlabel "Rounds"
set ylabel "# of stale mappings"

set xtics 0,20,140
set ytics 500

set key on

exp="scuttle-mixed2"

n = 6

plot "res/".exp."/precise-oldest-".(n*10) using 1:3 title "Precise-oldest ".(n*10)."%" with linespoints
replot "res/".exp."/precise-newest-".(n*10) using 1:3 title "Precise-newest ".(n*10)."%" with linespoints
replot "res/".exp."/scuttle-depth-".(n*10) using 1:3 title "Scuttle-depth ".(n*10)."%" with linespoints
replot "res/".exp."/scuttle-breadth-".(n*10) using 1:3 title "Scuttle-breadth ".(n*10)."%" with linespoints
replot "res/".exp."/scuttle-mixed-".(n*10) using 1:3 title "Scuttle-mixed ".(n*10)."%" with linespoints
