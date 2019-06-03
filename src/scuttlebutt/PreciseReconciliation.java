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

public class PreciseReconciliation extends DbContainer implements CDProtocol, EDProtocol {
    private static final String PAR_ORD = "order";
    private static final String PAR_K = "keys";
    private static final String PAR_MTU = "MTU";
    private static final String PAR_PERC = "percent";

    /**
     * Maximum size of a reconciliation message. If no value is specified then a
     * message can contain at most Integer.MAX_VALUE bytes.
     */
    protected static int MTU = Integer.MAX_VALUE;

    /**
     * 0 for precise oldest, 1 for precise newest, 2 for mixed behaviour.
     * String values for configuration file are "oldest", "newest" and "mixed".
     */
    private static int order;

    /**
     * Defines percentage of precise-newest and -oldest behaviour.
     * 0 means entirely precise-oldest, 100 means entirely precise-newest
     */
    private static int perc;

    private static int N;
    private static int K;

    private static final int CYCLE = Configuration.getInt("global.cycle");

    /**
     * Retrieves information from configuration file and creates the database for the prototype
     * @param prefix
     */
    public PreciseReconciliation(String prefix) {
        super(prefix);
        MTU = Configuration.getInt(prefix + "." + PAR_MTU, Integer.MAX_VALUE);
        String orderStr = Configuration.getString(prefix + "." + PAR_ORD);
        if (orderStr.equals("oldest")) {
            order = 0;
            System.err.println("---> Using precise-oldest ordering");
        } else if (orderStr.equals("newest")) {
            order = 1;
            System.err.println("---> Using precise-newest ordering");
        } else if (orderStr.equals("mixed")) {
            order = 2;
            perc = Configuration.getInt(prefix + "." + PAR_PERC);
            if (perc > 100 || perc < 0) {
                throw new IllegalArgumentException(prefix + "." + PAR_PERC + " must be between 0 and 100");
            }
            System.err.println("---> Using mixed ordering, " + perc + " percent precise newest");
        }
        N = Network.size();
        K = Configuration.getInt(prefix + "." + PAR_K);
        db = new Database(N,K,false);
        ScuttlebuttObserver.setK(K);
    }

    /**
     * Selects a random peer from the overlay and sends it a digest (versions without values) of its database.
     * NOTE: in this case the digest corresponds with the entire information we are storing in the database.
     * @param node
     * @param pid
     */
    @Override
    public void nextCycle(Node node, int pid) {
        Linkable linkable
                = (Linkable) node.getProtocol(FastConfig.getLinkable(pid));
        Node peer = linkable.getNeighbor(CommonState.r.nextInt(linkable.degree()));
        // Send first digest message
        ((Transport) node.getProtocol(FastConfig.getTransport(pid))).
                send(node, peer, new Message(node, db.getVersions(), null, Message.ACTION.DIGEST), pid);
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
            if (m.action == Message.ACTION.DIGEST) {
                ((Transport) node.getProtocol(FastConfig.getTransport(pid))).
                        send(node, m.sender, new Message(node, db.getVersions(),
                                getDifference((long[][]) m.digest), Message.ACTION.DIGEST_RESPONSE), pid);
            }
            if (m.action == Message.ACTION.DIGEST_RESPONSE) {
                db.reconcile(m.deltaSet);
                DeltaSet diff = getDifference((long[][]) m.digest);
                ((Transport) node.getProtocol(FastConfig.getTransport(pid))).
                        send(node, m.sender, new Message(node, null, diff, Message.ACTION.DELTA_SET), pid);
            }
            if (m.action == Message.ACTION.DELTA_SET)
                db.reconcile(m.deltaSet);
        }
    }

    /**
     * Creates the delta set for precise reconciliation.
     * Retrieves deltas that are newer than those in the digest, sorts them accordingly to reconciliation ordering and
     * inserts them into a {@link DeltaSet}, until either all of them have been processed or the size of the delta set
     * equals the (current) MTU.
     *
     * @param digest
     * @return
     */
    private DeltaSet getDifference(long[][] digest) {
        int MTU = CommonState.getTime() >= 15l*CYCLE ? this.MTU : Integer.MAX_VALUE;
        ArrayList<Delta> deltas = new ArrayList<>();

        // Retrieve fresh deltas from DB
        for (int i=0; i<N; i++) {
            for (int j=0; j<K; j++) {
                if (digest[i][j] < db.getVersion(i,j)) {
                    deltas.add(new Delta(i,j,db.getVersion(i,j)));
                }
            }
        }

        Collections.sort(deltas);
        if (order == 0) {
            Collections.reverse(deltas);
        }
        DeltaSet deltaSet = new DeltaSet(Math.min(100,MTU)); // Size is dynamic
        if (order < 2) {
            for (int i=0; i<MTU && i<deltas.size(); i++) {
                deltaSet.add(deltas.get(i).node, deltas.get(i).key,deltas.get(i).version);
            }
        } else if (order == 2) {
            int precNew = (int)((double)MTU*perc/100d); // Percentage of MTU used by precise oldest,
            // If MTU is bigger than deltas.size() then all updates are selected
            for (int i=0; i < Math.min(precNew,deltas.size()); i++) {
                deltaSet.add(deltas.get(i).node, deltas.get(i).key,deltas.get(i).version);
            }
            int s = deltaSet.size;
            for (int i=0; i<Math.min(MTU-s,deltas.size()-s); i++) {
                Delta d = deltas.get(deltas.size()-i-1); // Access last elements
                deltaSet.add(d.node, d.key, d.version);
            }
        }

        return deltaSet;
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
         * Sort by higher version number first
         * @param delta
         * @return
         */
        @Override
        public int compareTo(Delta delta) {
            return (int)(delta.version-this.version);
        }
    }

    /**
     * Creates a new database instance for the clone.
     * @return
     */
    public Object clone() {
        PreciseReconciliation prec = null;
        try {
            prec = (PreciseReconciliation) super.clone();
            prec.db = new Database(N,K,false);
        } catch (CloneNotSupportedException ex) {}
        return prec;
    }
}
