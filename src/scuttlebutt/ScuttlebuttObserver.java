package scuttlebutt;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class ScuttlebuttObserver implements Control {

    private static final String PAR_PROT = "protocol";

    protected static int reconciledCount = 0;
    protected static double avgMessageRate = 0;

    private final int pid;

    private static final int N = Network.size();

    private static int K;

    private static final int CYCLE = Configuration.getInt("global.cycle");

    // Holds the last time node i has updated entry (i,k)
    private static long times[][];

    protected static void setK(int k) { K = k; }

    public ScuttlebuttObserver(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        times = new long[N][K];
    }

    /**
     * At every round, it counts the metrics for the protocol's evaluation.
     *
     * @return
     */
    @Override
    public boolean execute() {
        if (CommonState.getTime() == 0) {
            System.out.println("t,reconciled,values,mappings,staleness");
            for (int i=0; i<N; i++) {
                Node node = Network.get(i);
                ((DbContainer)node.getProtocol(pid)).db.self = node.getID();
            }
            return false;
        }

        int countVal = 0, countEnt = 0; long maxStale = 0;
        boolean isStale[][] = new boolean[N][K];
        for (int i = 0; i < N; i++) {
            Node node = Network.get(i);
            DbContainer prot1 = (DbContainer) node.getProtocol(pid);
            int n1 = (int) node.getID();

            for (int j = 0; j < N; j++) {
                Node node2 = Network.get(j);
                int n2 = (int) node2.getID();

                if (n1 == n2) continue; // Same node, must not be compared

                DbContainer prot2 = (DbContainer) node2.getProtocol(pid);

                for (int k = 0; k < K; k++) {
                    // From the definition of stale entry
                    if (prot1.db.getVersion(n1,k) != prot2.db.getVersion(n1,k)) {
                        // If the version numbers are different, (n1,k) is updated at least once and is stale at node n2
                        maxStale = Long.max(CommonState.getTime()-times[n1][k], maxStale);
                        countEnt++;
                        if (!isStale[n1][k]) countVal++;
                        isStale[n1][k] = true;
                    }
                }
            }
        }

        System.out.println(CommonState.getTime()/CYCLE + ", " + reconciledCount/(2*N) + ", " + countVal + ", "
                                + countEnt + ", " + maxStale/CYCLE);
        reconciledCount = 0;
        avgMessageRate = 0;
        return false;
    }

    protected static void signalUpdate(int node, int key) {
        times[node][key] = CommonState.getTime();
    }

    /**
     * Time that has elapsed since (node,key) was updated
     * @param node
     * @param key
     * @return
     */
    protected static int getDelay(int node, int key) {
        return (int) ((CommonState.getTime() - times[node][key])/CYCLE);
    }
}
