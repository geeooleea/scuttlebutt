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
import java.util.Comparator;
import java.util.PriorityQueue;

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
	protected static int MTU = Integer.MAX_VALUE;

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

	private DeltaSet scuttleDepth(int[] digest) {
		PriorityQueue<Delta> deltas[] = getDeltas(digest);
		DeltaSet deltaSet = new DeltaSet(N,K);

		Arrays.sort(deltas, new ScuttleDepthComparator());
		for (int i=0; i<N && deltaSet.entryNumber() < MTU; i++) {
			while (!deltas[i].isEmpty() && deltaSet.entryNumber() < MTU) {
				Delta d = deltas[i].poll();
				deltaSet.put(d.node,d.key,d.value,d.timestamp);
			}
		}

		return deltaSet;
	}

	private DeltaSet scuttleBreadth(int[] digest) {
		PriorityQueue<Delta> deltas[] = getDeltas(digest);

		boolean empty = false;
		DeltaSet deltaSet = new DeltaSet(N,K);

		while (deltaSet.entryNumber() < MTU && !empty) {
			empty = true;
			for (int i=0; i<N; i++) {
				int node = (int) Network.get(i).getID(); // Random access to nodes
				if (!deltas[node].isEmpty()) {
					empty = false;
					Delta d = deltas[node].poll();
					deltaSet.put(node,d.key,d.value,d.timestamp);
				}
			}
		}
		return deltaSet;
	}

	class DeltaComparator implements Comparator<Delta>{
		@Override
		public int compare(Delta delta, Delta t1) {
			if (delta.timestamp < t1.timestamp) return -1;
			else if (delta.timestamp == t1.timestamp) return 0;
			else return 1;
		}
	}

	class ScuttleDepthComparator implements Comparator<PriorityQueue<Delta>>{
		@Override
		public int compare(PriorityQueue<Delta> deltas, PriorityQueue<Delta> t1) {
			if (deltas.size() > t1.size()) return -1;
			else if (deltas.size() == t1.size()) return 0;
			else return 1;
		}
	}

	private class Delta {
		int node, key, timestamp, value;
		public Delta(int node, int key, int timestamp, int value) {
			this.node = node;
			this.key = key;
			this.timestamp = timestamp;
			this.value = value;
		}
	}

	private PriorityQueue<Delta>[] getDeltas(int[] digest) {
		PriorityQueue<Delta> deltas[] = new PriorityQueue[N];
		DeltaComparator cmp = new DeltaComparator();

		// Obtain all deltas that are more recent than the peer's maximum timestamp in the digest
		for (int i = 0; i < N; i++) {
			deltas[i] = new PriorityQueue<>(K,cmp);
			for (int j = 0; j < K; j++) {
				int time = db.getTimestamp(i, j);
				if (digest[i] >= 0 && digest[i] < time) {
					deltas[i].add(new Delta(i,j,time,db.getValue(i,j)));
				}
			}
		}
		return deltas;
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
