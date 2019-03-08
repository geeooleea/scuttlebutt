/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scuttlebutt;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.NextCycleEvent;
import peersim.transport.Transport;

import java.util.Arrays;

/**
 *
 * @author Giulia Carocari
 */
public class Scuttlebutt extends NextCycleEvent implements EDProtocol, CDProtocol {

	private Database db;

	private static final String PAR_PROT = "loadbalance";

	private static final String PAR_ORD = "order";

	private static final int N = Network.size();

	private static int K;

	protected static void setK(int k) { K = k; }

	// If pid == -1 then no load balancing protocol is used
	private static int lbID = -1;

	/**
	 * Maximum size of a scuttlebutt message. If no value is specified then a
	 * message can contain at most Integer.MAX_VALUE bytes.
	 */
	protected static int MTU;

	/**
	 * 0 for scuttle-depth (default), 1 for scuttle-breadth. Values for
	 * configuration file are "depth" and "breadth"
	 */
	private static int order;

	private static final int DIGEST_SIZE = 8; // bytes

	private static final int DELTA_HEADER = 12; // bytes

	public Scuttlebutt(String prefix) {
		super(prefix);
		lbID = Configuration.getPid(prefix + "." + PAR_PROT, -1);
		System.err.println("Scuttlebutt protocol says:");
		if (lbID == -1) {
			System.err.println("---> No load balance protocol defined.");
		} else {
			System.err.println("---> Using custom load balance protocol.");
		}
		// Use default if value is other than "breadth"
		order = Configuration.getString(prefix + "." + PAR_ORD, "depth").equals("breadth") ? 1 : 0;
		System.err.println("---> Using scuttle-" + (order == 1 ? "breadth" : "depth") + " ordering");
	}

	void setDatabase(Database database) {
		this.db = database;
	}

	/**
	 * Initiates gossiping with a random peer in the overlay network.
	 *
	 * @param node
	 * @param pid
	 */
	@Override
	public void nextCycle(Node node, int pid) {
		Linkable linkable
				= (Linkable) node.getProtocol(FastConfig.getLinkable(pid));
		Node peer = linkable.getNeighbor(
				CommonState.r.nextInt(linkable.degree()));
		((Transport) node.getProtocol(FastConfig.getTransport(pid))).
				send(node, peer, new Message(node, getDigest(), Message.ACTION.DIGEST), pid);
	}

	@Override
	public void processEvent(Node node, int i, Object o) {
		if (o instanceof Message) {
			Message m = (Message) o;
			switch (m.action) {
				case DIGEST:
					((Transport) node.getProtocol(FastConfig.getTransport(i))).
							send(node,
									m.getSender(),
									new Message(node, getDigest(), Message.ACTION.DIGEST_RESPONSE),
									i);
				// NO BREAK HERE
				case DIGEST_RESPONSE:
					DeltaSet diff;
					if (order == 1) {
						diff = scuttleBreadth((int[]) m.getPayload());
					} else {
						diff = scuttleDepth((int[]) m.getPayload());
					}
					((Transport) node.getProtocol(FastConfig.getTransport(i))).
							send(node,
									m.getSender(),
									new Message(node, diff, Message.ACTION.DELTA_SET),
									i);
					break;
				case DELTA_SET:
					db.reconcile((DeltaSet) m.getPayload());
					break;
			}
		}
	}

	/**
	 * Simplify representation leaving digest as array and setting the values
	 * that did not fit in to -1.
	 *
	 * @return a scuttlebutt digest of the current state of the database
	 */
	private int[] getDigest() {
		return db.getDigest();
	}

	int[] getState(Node n) {
		return db.getState((int) n.getID());
	}

	void updateSelf(int node, int key, int value) {
		db.updateSelf(node, key, value);
	}

	/**
	 *
	 * @param digest
	 * @return
	 */
	private DeltaSet scuttleDepth(int[] digest) {
		int values[][] = new int[N][K];
		int timestamps[][] = new int[N][K];
		int pos[] = new int[N];
		int node[] = new int[N];
		int priority[] = new int[N];

		init(pos, node, priority);

		// Obtain all deltas that are more recent than the peer's maximum timestamp in the digest
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < K; j++) {
				int time = db.getTimestamp(i, j);
				if (digest[i] >= 0 && digest[i] < time) {
					values[i][j] = db.getValue(i, j);
					timestamps[i][j] = time;
					increase(pos, node, priority, i);
				}
			}
		}
		DeltaSet deltaSet = new DeltaSet(N, K);

		for (int i = 0; i < N; i++) {
			int n = deleteMax(node, pos, priority, N - i);
			for (int j = 0; j < K; j++) {
				int maxTimestamp = 0, maxKey = 0;
				for (int k = 0; k < K; k++) {
					if (values[n][k] > 0 && timestamps[n][k] > maxTimestamp) {
						maxTimestamp = timestamps[n][k];
						maxKey = k;
					}
				}
				if (DELTA_HEADER * (deltaSet.entryNumber() + 1) + deltaSet.size() + values[n][maxKey] <= MTU) {
					deltaSet.put(n, maxKey, values[n][maxKey], maxTimestamp);
				}
				if (DELTA_HEADER * deltaSet.entryNumber() + deltaSet.size() == MTU) {
					return deltaSet;
				}
				// No longer considering this entry, independently of insertion
				values[n][maxKey] = 0;
				timestamps[n][maxKey] = 0;
			}
		}
		return deltaSet;
	}

	private static int p(int i) {
		return (i - 1)/2;
	}

	private static int l(int i) {
		return (2 * i + 1);
	}

	private static int r(int i) {
		return (2 * i + 2);
	}

	private void init(int[] pos, int node[], int priority[]) {
		for (int i = 0; i < N; i++) {
			pos[i] = i;
			node[i] = i;
			priority[i] = 0;
		}
	}

	/**
	 * Increases the priority of the node by 1.
	 *
	 * @param pos
	 * @param priority
	 * @param node
	 */
	private static void increase(int[] pos, int node[], int[] priority, int n) {
		int i = pos[n];
		priority[i]++;
		while (i > 0 && priority[i] > priority[p(pos[i])]) {	
			swap(priority, i, p(i));
			swap(pos, i, p(i));
			swap(node, i, p(i));
			i = p(i);
		}
	}

	/**
	 * @param node
	 * @param pos
	 * @param priority
	 * @param i
	 * @param dim
	 */
	private static void maxHeapRestore(int node[], int[] pos, int[] priority, int i, int dim) {
		int maxindex;
		do {
			maxindex = i;
			if (l(i) <= dim && priority[l(i)] > priority[maxindex]) {
				maxindex = l(i);
			}
			if (r(i) <= dim && priority[r(i)] > priority[maxindex]) {
				maxindex = r(i);
			}
			if (maxindex != i) {
				swap(priority, i, maxindex);
				swap(pos, i, maxindex);
				swap(node, i, maxindex);
			}
		} while (maxindex != i);
	}

	private static int deleteMax(int node[], int pos[], int heap[], int dim) {
		swap(heap, 0, dim - 1);
		swap(pos, 0, dim - 1);
		swap(node, 0, dim - 1);
		dim--;
		maxHeapRestore(node, pos, heap, 0, dim - 1);
		return node[dim];
	}

	private static void swap(int[] arr, int i, int j) {
		arr[i] = (arr[i] + arr[j]) - (arr[j] = arr[i]);
	}

	private DeltaSet scuttleBreadth(int[] digest) {
		int values[][] = new int[N][K];
		int timestamps[][] = new int[N][K];

		// Obtain all deltas that are more recent than the peer's maximum timestamp in the digest
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < K; j++) {
				int time = db.getTimestamp(i, j);
				if (digest[i] >= 0 && digest[i] < time) {
					values[i][j] = db.getValue(i, j);
					timestamps[i][j] = time;
				} else {
					timestamps[i][j] = -1;
					values[i][j] = -1;
				}
			}
		}
		int size = 0;
		DeltaSet deltaSet = new DeltaSet(N, K);
		boolean moreDeltas = true;

		while (moreDeltas) {
			moreDeltas = false;

			int ranked[] = new int[N];
			for (int i = 0; i < N; i++) ranked[i] = -1; // Initialize to -1, 0 might be a valid key

			for (int i = 0; i < N; i++) {
				int min = Integer.MAX_VALUE;
				for (int j = 0; j < K; j++) { // Find current min timestamp of node i
					if (values[i][j] > 0 && timestamps[i][j] < min) {
						min = timestamps[i][j];
						ranked[i] = j;
						moreDeltas = true;
					}
				}
			}
			// System.err.println(Arrays.toString(ranked));
			for (int i = 0; i < N; i++) { // Random access iteration with Network.get()
				int node = (int) Network.get(i).getID();
				if (ranked[node] >= 0) {
					if (values[node][ranked[node]] > 0 && size + 1 <= MTU) {
						deltaSet.put(node, ranked[node], values[node][ranked[node]], timestamps[node][ranked[node]]);
						size++;
					}
					values[node][ranked[node]] = 0; // Exclude this entry from being considered
					timestamps[node][ranked[node]] = 0;
				}
			}
			// if (size == MTU) return deltaSet;
		}
		// System.err.println(deltaSet.entryNumber());
		return deltaSet;
	}

	@Override
	public Object clone() {
		Scuttlebutt clone = null;
		try {
			clone = (Scuttlebutt) super.clone();
			// Database setting happens after cloning
			if (db != null) {
				clone.setDatabase((Database) db.clone());
			}
		} catch (CloneNotSupportedException ex) {}
		return clone;
	}

}
