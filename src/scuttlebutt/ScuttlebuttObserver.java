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
    private static long times[][][];

    protected static void setK(int k) { K = k; }

    public ScuttlebuttObserver(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        times = new long[N][N][K];
    }

    @Override
    public boolean execute() {
        if (CommonState.getTime() == 0) {
            System.out.println("a,b,c,d,e,f");
            return false;
        }
        int countVal = 0, countEnt = 0; long maxStale = 0;
        int staleO = 0,staleN = 0,staleK = 0; long staleV = -1;
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
                        if (/*scuttlebutt2.db.getVersion(n1,k) >= 0 && CommonState.getTime()*/ times[n1][n2][k] - times[n2][n1][k] >= maxStale) {
                            maxStale = Long.max(CommonState.getTime() - times[n2][n1][k], maxStale);
                            staleN=n1; staleO = n2; staleK = k; staleV = scuttlebutt2.db.getVersion(n1,k);
                        }
                        countEnt++;
                        if (!isStale[n1][k]) countVal++;
                        isStale[n1][k] = true;
                    }
                }
            }
        }

        System.out.println(CommonState.getTime()/10000 + ", " + reconciledCount + ", " + avgMessageRate + ", " + countVal + ", " + countEnt + ", " + maxStale/10000);
        // System.out.println("MaxStale on " + staleO + ": (" + staleN + ", " + staleK + ", "+ staleV +")");
        reconciledCount = 0;
        avgMessageRate = 0;
        return false;
    }

    protected static void signalUpdate(int self, int node, int key) {
        times[self][node][key] = CommonState.getTime();
    }
}
