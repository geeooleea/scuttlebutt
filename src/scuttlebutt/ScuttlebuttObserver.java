package scuttlebutt;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class ScuttlebuttObserver implements Control {

    private static final String PAR_PROT = "protocol";

    private final int pid;

    private static final int N = Network.size();

    private static int K;

    // Holds the last time node j has updated entry (i,k)
    private static long times[][][];

    protected static void setK(int k) { K = k; }

    public ScuttlebuttObserver(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        times = new long[N][N][K];
    }

    @Override
    public boolean execute() {
        int countVal = 0, countEnt = 0; long maxStale = 0;
        boolean isStale[][] = new boolean[N][K];
        for (int i = 0; i < N; i++) {
            Node node = Network.get(i);
            Scuttlebutt scuttlebutt = (Scuttlebutt) node.getProtocol(pid);
            int n1 = (int) node.getID();
            for (int j = 0; j < N; j++) {
                if (i == j) {
                    continue;
                }
                Node node2 = Network.get(j);
                Scuttlebutt scuttlebutt2 = (Scuttlebutt) node2.getProtocol(pid);
                int n2 = (int) node2.getID();
                for (int k = 0; k < K; k++) {
                    if (scuttlebutt.db.getVersion(n1,k) != scuttlebutt2.db.getVersion(n1,k)) {
                        countEnt++;
                        if (!isStale[n1][k]) countVal++;
                        isStale[n1][k] = true;
                    }
                    //maxStale = (times[n2][n1][k] > 0 ?
                    //       Long.max(times[n1][n1][k] - times[n2][n1][k], maxStale) : maxStale);
                }
            }
        }

        System.out.println(/*maxStale + ", " +*/ countVal + ", " + countEnt);
        return false;
    }

    protected static void signalUpdate(int self, int node, int key) {
        times[self][node][key] = CommonState.getTime();
    }
}
