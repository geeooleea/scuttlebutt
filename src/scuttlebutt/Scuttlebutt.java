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

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Implementation of the scuttlebutt protocol for reconciliation
 */
public class Scuttlebutt extends DbContainer implements CDProtocol, EDProtocol  {
    private static final String PAR_PROT = "loadbalance";
    private static final String PAR_ORD = "order";
    private static final String PAR_K = "keys";
    private static final String PAR_MTU = "MTU";

    // If pid == -1 then no load balancing protocol is used
    private static int lbID = -1;

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

    /**
     * @param prefix
     */
    public Scuttlebutt(String prefix) {
        super(prefix);
        lbID = Configuration.getPid(prefix + "." + PAR_PROT, -1);
        System.err.println("scuttlebutt.Scuttlebutt protocol says:");
        if (lbID == -1) {
            System.err.println("---> No load balance protocol defined.");
        } else {
            System.err.println("---> Using custom load balance protocol.");
        }
        MTU = Configuration.getInt(prefix + "." + PAR_MTU);
        // Use default if value is other than "breadth"
        order = Configuration.getString(prefix + "." + PAR_ORD, "depth").equals("breadth") ? 1 : 0;
        System.err.println("---> Using scuttle-" + (order == 1 ? "breadth" : "depth") + " ordering");
        N = Network.size();
        K = Configuration.getInt(prefix + "." + PAR_K);
        db = new Database(N,K,true);
        ScuttlebuttObserver.setK(K);
    }

    /**
     * Start gossiping with a peer in the overlay network
     * @param node
     * @param pid
     */
    @Override
    public void nextCycle(Node node, int pid) {
        Linkable linkable
                = (Linkable) node.getProtocol(FastConfig.getLinkable(pid));
        // Obtain peer to initiate gossiping
        Node peer = linkable.getNeighbor(
                CommonState.r.nextInt(linkable.degree()));
        // Send first digest message
        ((Transport) node.getProtocol(FastConfig.getTransport(pid))).
                send(node, peer, new Message(node, db.getDigest(), Message.ACTION.DIGEST), pid);
    }

    @Override
    public void processEvent(Node node, int pid, Object o) {
        if (o instanceof Message) {
            Message m = (Message) o;
            switch (m.action) {
                case DIGEST: {
                    ((Transport) node.getProtocol(FastConfig.getTransport(pid))).
                            send(node, m.sender, new Message(node, db.getDigest(), Message.ACTION.DIGEST_RESPONSE), pid);
                }
                case DIGEST_RESPONSE: {
                    DeltaSet diff = getDifference((long[]) m.payload);
                    ((Transport) node.getProtocol(FastConfig.getTransport(pid))).
                            send(node, m.sender, new Message(node, diff, Message.ACTION.DELTA_SET), pid);
                    break;
                }
                case DELTA_SET:
                    db.reconcile((DeltaSet) m.payload);
            }
        }
    }

    /**
     * Build the delta set that represents the differences between to peers give a digest
     * @param digest
     * @return
     */
    private DeltaSet getDifference(long[] digest) {
        // Set MTU after 15 seconds to allow the system to warm up and remove bias
        int MTU = CommonState.getTime() >= 15l*10000 ? this.MTU : Integer.MAX_VALUE;
        DeltaSet deltaSet = new DeltaSet(Integer.min(MTU,100));
        PriorityQueue<Delta> deltas[] = getDeltas(digest);

        int next[] = new int[N];
        int updates = 0;
        // Select a different random ordering for every gossip exchange
        for (int i=0; i<N; i++) {
            next[i] = (int) Network.get(i).getID();
            updates += deltas[i].size();
        }

        if (order == 1) { // Scuttle-breadth
            boolean empty = false;
            while (!empty && deltaSet.size < MTU) {
                empty = true;
                for (int i=0; i<N && deltaSet.size < MTU; i++) {
                    if (!deltas[next[i]].isEmpty()) {
                        empty = false;
                        Delta d = deltas[next[i]].poll();
                        deltaSet.add(d.node,d.key,d.version);
                    }
                }
            }
        } else { // Scuttle depth
            Arrays.sort(deltas, new DeltaQueueComparator());
            for (int i=0; i<N && deltaSet.size < MTU; i++) {
                while (!deltas[i].isEmpty() && deltaSet.size < MTU) {
                    Delta d = deltas[i].poll();
                    deltaSet.add(d.node,d.key,d.version);
                }
            }
        }

        if (updates > 0) {
            ScuttlebuttObserver.avgMessageRate = (ScuttlebuttObserver.avgMessageRate + ((double)deltaSet.size / updates)) / 2;
        }

        return deltaSet;
    }

    /**
     * Implementation of the definition of scuttlebutt delta set in paragraph 3.2
     *
     * @param digest
     * @return
     */
    private PriorityQueue<Delta>[] getDeltas(long[] digest) {
        PriorityQueue<Delta> deltas[] = new PriorityQueue[N];
        DeltaComparator cmp = new DeltaComparator();

        // Obtain all deltas that are more recent than the peer's maximum version in the digest
        for (int node = 0; node < N; node++) {
            deltas[node] = new PriorityQueue<>(cmp);
            for (int key = 0; key < K; key++) {
                long time = db.getVersion(node, key);
                if (time > digest[node]) { // Current update at this node for (node,key) is fresher than any other at the peer
                    deltas[node].add(new Delta(node,key,time));
                }
            }
        }
        return deltas;
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

    /**
     * Sorts by decreasing priority queue size
     */
    private class DeltaQueueComparator implements Comparator<PriorityQueue<Delta>> {
        @Override
        public int compare(PriorityQueue<Delta> deltas, PriorityQueue<Delta> t1) {
            return (t1.size() - deltas.size());
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

    @Override
    public Object clone() {
        Scuttlebutt sc = null;
        try {
            sc = (Scuttlebutt) super.clone();
            sc.db = new Database(N,K,true);
        } catch (CloneNotSupportedException ex) {}
        return sc;
    }
}
