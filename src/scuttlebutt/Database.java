package scuttlebutt;

/**
 * Class that implements the database replicated onto every node.
 */
public class Database {

    private static int N;
    private static int K;
    private long version[][];
    private long digest[];
    private long n = 0;
    private static long clock = 0;
    private boolean scuttlebutt;
    protected long self;

    /**
     * Creates a database given its size. The resulting database is a table of size N*K.
     *
     * @param N Number of nodes in the network
     * @param K Number of keys for every node
     */
    public Database(int N, int K, boolean scuttlebutt) {
        this.N = N;
        this.K = K;
        this.scuttlebutt = scuttlebutt;
        digest = new long[N];
        version = new long[N][K];
        for (int i=0; i<N; i++) {
            digest[i] = -1;
            for (int j=0; j<K; j++) {
                version[i][j] = -1;
            }
        }
    }

    /**
     * Returns the current version number of a mapping
     *
     * @param node
     * @param key
     * @return
     */
    public long getVersion(int node, int key) {
        return version[node][key];
    }

    public static int getK() {
        return K;
    }

    /**
     * Returns the current maximum version number for every node in this database
     *
     * @return
     */
    public long[] getDigest() {
        return digest;
    }

    /**
     * Updates a mapping
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
     * Updates the mapping for the node holding the database creating a new version number
     *
     * @param node
     * @param key
     */
    public void update(int node, int key) {
        if (scuttlebutt)
            update(node, key, ++n);
        else
            update(node, key, ++clock);
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

    public long[][] getVersions() {
        return version;
    }
}

