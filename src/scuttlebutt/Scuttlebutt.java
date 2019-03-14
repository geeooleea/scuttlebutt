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
		// Obtain peer to initiate gossiping
		Node peer = linkable.getNeighbor(
				CommonState.r.nextInt(linkable.degree()));
		// Send first digest message
		((Transport) node.getProtocol(FastConfig.getTransport(pid))).
				send(node, peer, new Message(node, getDigest(), Message.ACTION.DIGEST), pid);
	}

	@Override
	public void processEvent(Node node, int i, Object o) {
		if (o instanceof Message) {
			Message m = (Message) o;
			switch (m.action) {
				case DIGEST: {
					((Transport) node.getProtocol(FastConfig.getTransport(i))).
							send(node,
									m.getSender(),
									new Message(node, getDigest(), Message.ACTION.DIGEST_RESPONSE),
									i);
				}
				// NO BREAK HERE, after a node gets a digest it immediately sends the corresponding delta set
				case DIGEST_RESPONSE: {
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
				}
				case DELTA_SET: {
					db.reconcile((DeltaSet) m.getPayload());
					break;
				}
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

	void updateSelf(int key, int value) {
		db.updateSelf(key, value);
	}

	private DeltaSet scuttleDepth(int[] digest) {
		// Obtain deltas for all nodes in the correct order
		PriorityQueue<Delta> deltas[] = getDeltas(digest);

		PriorityQueue<PriorityQueue<Delta>> depthSorted = new PriorityQueue<>(N,new ScuttleDepthComparator());

		// Consider using the random access iterator to nodes
		for (PriorityQueue<Delta> pq : deltas) depthSorted.add(pq);

		DeltaSet deltaSet = new DeltaSet(N,K);
		while (!depthSorted.isEmpty() && deltaSet.entryNumber() < MTU) {
			PriorityQueue<Delta> pq = depthSorted.poll();
			while (!pq.isEmpty() && deltaSet.entryNumber() < MTU) {
				Delta d = pq.poll();
				if (d.timestamp > digest[d.node])
					deltaSet.put(d.node,d.key,d.value,d.timestamp);
				else {
					System.err.println("Just avoided putting obsolete update into digest");
				}
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
					deltaSet.put(d.node,d.key,d.value,d.timestamp);
				}
			}
		}
		return deltaSet;
	}

	private class Delta {
		int node, key, timestamp, value;
		public Delta(int node, int key, int value, int timestamp) {
			this.node = node;
			this.key = key;
			this.timestamp = timestamp;
			this.value = value;
		}
	}

	/**
	 * Implementation of the definition of scuttlebutt delta set in paragraph 3.2
	 *
	 * @param digest
	 * @return
	 */
	private PriorityQueue<Delta>[] getDeltas(int[] digest) {
		PriorityQueue<Delta> deltas[] = new PriorityQueue[N];
		DeltaComparator cmp = new DeltaComparator();

		// Obtain all deltas that are more recent than the peer's maximum timestamp in the digest
		for (int node = 0; node < N; node++) {
			deltas[node] = new PriorityQueue<>(cmp);
			for (int key = 0; key < K; key++) {
				int time = db.getTimestamp(node, key);
				if (time > digest[node]) { // Current update at this node for (node,key) is fresher than any other at the peer
					deltas[node].add(new Delta(node,key,db.getValue(node,key),time));
				}
			}
		}
		return deltas;
	}

	class DeltaComparator implements Comparator<Delta>{
		@Override
		public int compare(Delta delta, Delta t1) {
			if (delta.timestamp < t1.timestamp) return -1;
			else if (delta.timestamp == t1.timestamp) return 0; // Never happens
			else return 1;
		}
	}

	class ScuttleDepthComparator implements Comparator<PriorityQueue<Delta>>{
		@Override
		public int compare(PriorityQueue<Delta> deltas, PriorityQueue<Delta> t1) {
			if (deltas.size() < t1.size()) return -1;
			else if (deltas.size() == t1.size()) return 0;
			else return 1;
		}
	}

	@Override
	public Object clone() {
		Scuttlebutt clone = null;
		try {
			clone = (Scuttlebutt) super.clone();
			// NOTE: database is set after protocol initialization
		} catch (CloneNotSupportedException ex) {}
		return clone;
	}

}
