package scuttlebutt;

/**
 * Class that holds the deltas exchanged among peers
 */
public class DeltaSet {
    private int nodes[];
    private int keys[];
    private long versions[];

    protected int size;
    private int i = -1;
    private int capacity;

    /**
     * Creates a delta set.
     *
     * @param capacity initial capacity of the delta set
     */
    public DeltaSet(int capacity) {
        this.capacity = capacity;
        nodes = new int[capacity];
        keys = new int[capacity];
        versions = new long[capacity];
    }

    /**
     * Inserts delta into delta set. If new size exceeds capacity, then the capacity is doubled.
     * @param node
     * @param key
     * @param version
     */
    public void add(int node, int key, long version) {
        if (size == capacity) {
            doubleSize();
        }
        nodes[size] = node;
        keys[size] = key;
        versions[size] = version;
        size++;
    }

    /**
     * States if there are any more deltas in the delta set to iterate over
     * @return True if using {@link #node()), {@link #version()} and {@link #key()} will return a delta
     * that has not been processed in this iteration of the deltaset.
     */
    public boolean next() {
        i++;
        if (i<size) return true;
        else {
            i = -1;
            return false;
        }
    }

    /**
     * Retrieves the node of the next delta in the current iteration
     * @return
     */
    public int node() {
        return nodes[i];
    }

    /**
     * Retrieves the key of the next delta in the current iteration
     * @return
     */
    public int key() {
        return keys[i];
    }

    /**
     * Retrieves the version number of the next delta in the current iteration
     * @return
     */
    public long version() {
        return versions[i];
    }

    private void doubleSize() {
        capacity *= 2;
        int n[] = new int[capacity];
        int k[] = new int[capacity];
        long v[] = new long[capacity];

        for (int i=0; i<size; i++) {
            n[i] = nodes[i];
            k[i] = keys[i];
            v[i] = versions[i];
        }
        nodes = n;
        keys = k;
        versions = v;
    }
}
