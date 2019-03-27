package scuttlebutt;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class PreciseReconciliation  extends DbContainer implements CDProtocol, EDProtocol {
    private static final String PAR_PROT = "loadbalance";
    private static final String PAR_ORD = "order";
    private static final String PAR_K = "keys";
    private static final String PAR_MTU = "MTU";


    /**
     * Maximum size of a scuttlebutt message. If no value is specified then a
     * message can contain at most Integer.MAX_VALUE bytes.
     */
    protected static int MTU = Integer.MAX_VALUE;

    /**
     * 0 for scuttle-depth (default), 1 for scuttle-breadth. Values for
     * configuration file are "depth" and "breadth"
     */
    private static int order;

    private static int N;
    private static int K;

    // protected Database db;

    public PreciseReconciliation(String prefix) {
        super(prefix);
        MTU = Configuration.getInt(prefix + "." + PAR_MTU, Integer.MAX_VALUE);
        // Use default if value is other than "breadth"
        order = Configuration.getString(prefix + "." + PAR_ORD, "newest").equals("oldest") ? 1 : 0;
        System.err.println("---> Using precise-" + (order == 1 ? "oldest" : "newest") + " ordering");
        N = Network.size();
        K = Configuration.getInt(prefix + "." + PAR_K);
        db = new Database(N,K);
        ScuttlebuttObserver.setK(K);
    }

    @Override
    public void nextCycle(Node node, int pid) {
        db.self = (int) node.getID();
        Linkable linkable
                = (Linkable) node.getProtocol(FastConfig.getLinkable(pid));
        // Obtain peer to initiate gossiping
        Node peer = linkable.getNeighbor(
                CommonState.r.nextInt(linkable.degree()));
        // Send first digest message
        ((Transport) node.getProtocol(FastConfig.getTransport(pid))).
                send(node, peer, new Message(node, db.getVersions(), Message.ACTION.DIGEST), pid);
    }

    @Override
    public void processEvent(Node node, int pid, Object o) {
        if (o instanceof Message) {
            Message m = (Message) o;
            if (m.action == Message.ACTION.DIGEST)
                ((Transport) node.getProtocol(FastConfig.getTransport(pid))).
                        send(node, m.sender, new Message(node, db.getVersions(), Message.ACTION.DIGEST_RESPONSE), pid);
            if (m.action == Message.ACTION.DIGEST || m.action == Message.ACTION.DIGEST_RESPONSE) {
                DeltaSet diff = getDifference((long[][]) m.payload);
                ((Transport) node.getProtocol(FastConfig.getTransport(pid))).
                        send(node, m.sender, new Message(node, diff, Message.ACTION.DELTA_SET), pid);
            }
            if (m.action == Message.ACTION.DELTA_SET)
                db.reconcile((DeltaSet) m.payload);
        }
    }

    private DeltaSet getDifference(long[][] digest) {
        ArrayList<Delta> deltas = new ArrayList<>();

        for (int i=0; i<=N; i++) {
            for (int j=0; j<K; j++) {
                if (digest[i][j] < db.getVersion(i,j)) {
                    deltas.add(new Delta(i,j,db.getVersion(i,j)));
                }
            }
        }

        deltas.sort(new DeltaComparator());
        if (order == 0) {
            Collections.reverse(deltas);
        }
        DeltaSet deltaSet = new DeltaSet(Math.min(100,MTU));
        for (int i=0; i<MTU && i<deltas.size(); i++) {
            deltaSet.add(deltas.get(i).node, deltas.get(i).key,deltas.get(i).version);
        }
        return deltaSet;
    }

    /**
     * Sorts by increasing version number
     */
    private class DeltaComparator implements Comparator<Delta> {
        @Override
        public int compare(Delta delta, Delta t1) {
            return (int) (delta.version - t1.version);
        }
    }

    private class Delta {
        int node, key;
        long version;

        public Delta(int node, int key, long version) {
            this.node = node;
            this.key = key;
            this.version = version;
        }
    }

    public Object clone() {
        PreciseReconciliation sc = null;
        try {
            sc = (PreciseReconciliation) super.clone();
            sc.db = new Database(N,K);
        } catch (CloneNotSupportedException ex) {}
        return sc;
    }
}
