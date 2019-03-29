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

    // Holds the last time node j has updated entry (i,k)
    private static long times[][];

    protected static void setK(int k) { K = k; }

    public ScuttlebuttObserver(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        times = new long[N][K];
    }

    @Override
    public boolean execute() {
        if (CommonState.getTime() == 0) {
            System.out.println("t,reconciled,values,mappings,staleness");
            return false;
        }
        int countVal = 0, countEnt = 0; long maxStale = 0;
        boolean isStale[][] = new boolean[N][K];
        for (int i = 0; i < N; i++) {
            Node node = Network.get(i);
            DbContainer scuttlebutt = (DbContainer) node.getProtocol(pid);
            int n1 = (int) node.getID();
            for (int j = 0; j < N; j++) {
                Node node2 = Network.get(j);
                if (node.getID() == node2.getID()) continue;
                DbContainer scuttlebutt2 = (DbContainer) node2.getProtocol(pid);
                int n2 = (int) node2.getID();
                for (int k = 0; k < K; k++) {
                    // From the definition of stale entry
                    if (scuttlebutt.db.getVersion(n1,k) != scuttlebutt2.db.getVersion(n1,k)) {
                        maxStale = Long.max((times[n1][k] > 0 ? CommonState.getTime() - times[n1][k] : 0), maxStale);
                        countEnt++;
                        if (!isStale[n1][k]) countVal++;
                        isStale[n1][k] = true;
                    }
                }
            }
        }

        System.out.println(CommonState.getTime()/10000 + ", " + reconciledCount + ", " + countVal + ", " + countEnt + ", " + maxStale/10000);
        reconciledCount = 0;
        avgMessageRate = 0;
        return false;
    }

    protected static void signalUpdate(int node, int key) {
        times[node][key] = CommonState.getTime();
    }
}
