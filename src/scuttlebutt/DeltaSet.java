package scuttlebutt;

/**
 * Class that holds the deltas exchanged among peers
 */
public class DeltaSet {
    private int nodes[];
    private int keys[];
    private long versions[];

    protected int size;
    int i = -1;
    private int capacity;

    /**
     * Creates a delta set.
     *
     * @param capacity initial size of the delta set
     */
    public DeltaSet(int capacity) {
        this.capacity = capacity;
        nodes = new int[capacity];
        keys = new int[capacity];
        versions = new long[capacity];
    }

    /**
     * Inserts delta into delta set
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

    public boolean next() {
        i++;
        if (i<size) return true;
        else {
            i = -1;
            return false;
        }
    }

    public int node() {
        return nodes[i];
    }

    public int key() {
        return keys[i];
    }

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
