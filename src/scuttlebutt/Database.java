package scuttlebutt;

import peersim.core.CommonState;

/**
 * Class that implements the database replicated onto every node.
 */
public class Database {

    private static int N;
    private static int K;
    private long version[][];
    private long digest[];
    private long n = 0;
    private boolean scuttlebutt;
    protected long self;

    /**
     * Creates a database given its size. The resulting database is a table of size N*K.
     *
     * @param N Number of nodes in the network
     * @param K Number of keys for every node
     * @param scuttlebutt States if a the database is meant for scuttlebutt or precise reconciliation.
     *                    If false, then a synchronized clock is used to timestamp updates.
     */
    public Database(int N, int K, boolean scuttlebutt) {
        this.N = N;
        this.K = K;
        this.scuttlebutt = scuttlebutt;
        digest = new long[N];
        version = new long[N][K];
        // 0 is a valid timestamp, so not updated keys are -1
        for (int i=0; i<N; i++) {
            digest[i] = -1;
            for (int j=0; j<K; j++) {
                version[i][j] = -1;
            }
        }
    }

    /**
     * Returns the current version number of a mapping.
     *
     * @param node Node coordinate of the mapping
     * @param key   Key coordinate of the mapping
     * @return  Version number/timestamp associated with this (node,key) mapping
     */
    public long getVersion(int node, int key) {
        return version[node][key];
    }

    /**
     * Retrieves the number of keys for each state in the database.
     * @return The number of keys
     */
    public int getK() {
        return K;
    }

    /**
     * Returns the current maximum version number for every node's state in this database
     *
     * @return
     */
    public long[] getDigest() {
        return digest;
    }

    /**
     * Updates a generic mapping.
     *
     * @param node
     * @param key
     * @param time
     */
    public void update(int node, int key, long time) {
        if (node != self && time > version[node][key]) PropagationObserver.increment(node, key);
        version[node][key] = Long.max(version[node][key], time);
        digest[node] = Long.max(digest[node], time);
    }

    /**
     * Updates the mapping for the node holding the database, creating a new version number.
     *
     * @param node
     * @param key
     */
    public void update(int node, int key) {
        if (scuttlebutt)
            update(node, key, ++n);
        else
            update(node, key, CommonState.getTime());
    }

    /**
     * Reconciles differences between databases at different nodes enclosed in the delta set
     *
     * @param deltaSet
     */
    public void reconcile(DeltaSet deltaSet) {
        while (deltaSet.next()) {
            ScuttlebuttObserver.reconciledCount++;
            update(deltaSet.node(), deltaSet.key(), deltaSet.version());
        }
    }

    /**
     * Return all version numbers in the database, including those for mappings that don't exist yet.
     * Is used as a digest for precise reconciliation.
     *
     * @return
     */
    public long[][] getVersions() {
        return version;
    }
}

