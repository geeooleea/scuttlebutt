# Scuttlebutt
Implementation of the [Efficient Reconciliation and Flow Control for Anti-Entropy Protocols](https://www.cs.cornell.edu/home/rvr/papers/flowgossip.pdf) for the [Peersim network simulator](http://peersim.sourceforge.net/)

This code can simulate both the reconciliation mechanisms described in the paper above (Precise reconciliation and Scuttlebutt). Each configuration file runs a different experiment:
* Scuttlebutt with scuttle-breadth ordering
* Scuttlebutt with scuttle-depth ordering
* Precise reconciliation with precise-oldest ordering
* Precise reconciliation with precise-newest ordering

Each experiment runs on a network of 128 nodes, each holding 64 key-value mappings.
