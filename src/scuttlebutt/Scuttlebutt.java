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
    protected int MTU = Integer.MAX_VALUE;

    /**
     * 0 for scuttle-depth, 1 for scuttle-breadth. String values for
     * configuration file are "depth" and "breadth".
     */
    private static int order;

    // Database is NxK
    private static int N;
    private static int K;

    private static final int CYCLE = Configuration.getInt("global.cycle");

    /**
     * @param prefix
     */
    public Scuttlebutt(String prefix) {
        super(prefix);
        lbID = Configuration.getPid(prefix + "." + PAR_PROT, -1);
        System.err.println("scuttlebutt.Scuttlebutt protocol says:");
        ////// Not used /////
        if (lbID == -1) {
            System.err.println("---> No load balance protocol defined.");
        } else {
            System.err.println("---> Using custom load balance protocol.");
        }
        /////////////////////
        MTU = Configuration.getInt(prefix + "." + PAR_MTU);
        String orderStr = Configuration.getString(prefix + "." + PAR_ORD);
        if (orderStr.equals("depth")) {
            order = 0;
            System.err.println("---> Using scuttle-depth ordering");
        } else if (orderStr.equals("breadth")) {
            order = 1;
            System.err.println("---> Using scuttle-breadth ordering");
        }

        N = Network.size();
        K = Configuration.getInt(prefix + "." + PAR_K);
        db = new Database(N,K,true);
        ScuttlebuttObserver.setK(K);
    }

    /**
     * Start gossiping with a randomly selected peer in the overlay network.
     * @param node
     * @param pid
     */
    @Override
    public void nextCycle(Node node, int pid) {
        Linkable linkable
                = (Linkable) node.getProtocol(FastConfig.getLinkable(pid));
        // Obtain peer to initiate gossiping
        Node peer = linkable.getNeighbor(CommonState.r.nextInt(linkable.degree()));
        // Send first digest message
        ((Transport) node.getProtocol(FastConfig.getTransport(pid))).
                send(node, peer, new Message(node, db.getDigest(), null, Message.ACTION.DIGEST), pid);
    }

    /**
     * Answers incoming {@link Message} instances based on their "ACTION" field.
     * Message action DIGEST: Sends a DIGEST_RESPONSE and subsequently a DELTA_SET Message.
     * Message action DIGEST_RESPONSE: Sends DELTA_SET.
     * Message action DELTA_SET: Reconciles its database with the differences in the message payload.
     *
     * @param node
     * @param pid
     * @param o is processed only if o is instace of {@link Message}.
     */
    @Override
    public void processEvent(Node node, int pid, Object o) {
        if (o instanceof Message) {
            Message m = (Message) o;
            switch (m.action) {
                case DIGEST: {
                    ((Transport) node.getProtocol(FastConfig.getTransport(pid))).
                            send(node, m.sender, new Message(node, db.getDigest(),
                                    getDifference((long[]) m.digest), Message.ACTION.DIGEST_RESPONSE), pid);
                    break;
                }
                case DIGEST_RESPONSE: {
                    db.reconcile(m.deltaSet);
                    DeltaSet diff = getDifference((long[]) m.digest);
                    ((Transport) node.getProtocol(FastConfig.getTransport(pid))).
                            send(node, m.sender, new Message(node, null, diff, Message.ACTION.DELTA_SET), pid);
                    break;
                }
                case DELTA_SET:
                    db.reconcile(m.deltaSet);
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
        int MTU = CommonState.getTime() >= 15l*CYCLE ? this.MTU : Integer.MAX_VALUE;
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
     * Retrieves all updates that are more recent than the ones in the digest.
     * @param digest
     * @return
     */
    private PriorityQueue<Delta>[] getDeltas(long[] digest) {
        PriorityQueue<Delta> deltas[] = new PriorityQueue[N];

        // Obtain all deltas that are more recent than the peer's maximum version in the digest
        for (int node = 0; node < N; node++) {
            deltas[node] = new PriorityQueue<>();
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
     * Sorts by decreasing priority queue size
     */
    private class DeltaQueueComparator implements Comparator<PriorityQueue<Delta>> {
        @Override
        public int compare(PriorityQueue<Delta> deltas, PriorityQueue<Delta> t1) {
            return (t1.size() - deltas.size());
        }
    }

    private class Delta implements Comparable<Delta> {
        int node, key;
        long version;
        public Delta(int node, int key, long version) {
            this.node = node;
            this.key = key;
            this.version = version;
        }

        /**
         * Sorts by increasing version number
         * @param delta
         * @return
         */
        @Override
        public int compareTo(Delta delta) {
            return (int)(this.version-delta.version);
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
