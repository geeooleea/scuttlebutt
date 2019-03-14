/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scuttlebutt;

import java.util.logging.Level;
import java.util.logging.Logger;
import peersim.core.Network;

/**
 *
 * @author Giulia Carocari
 */
public class Database {
	private int[][] values;
	private int[][] timestamps;
	private int[] maxTime;
	
	private static final int N = Network.size();
	private final int K;
	private int currTime = 0;
	private int self = -1;
	
	/**
	 * 
	 * @param k Number of keys for each state in the database
	 */
	public Database(int k) {
		this.K = k;
		values = new int[N][K];
		timestamps = new int[N][K];
		maxTime = new int[N];
	}

	/**
	 * Sets the node that owns this database.
	 * Only needed at database initialization time.
	 *
	 * @param self
	 */
	public void setSelf(int self) {
		this.self = self;
	}

	/**
	 * Update state only if the update is fresher than the one currently available
	 * 
	 * @param node
	 * @param key
	 * @param value
	 * @param time 
	 */
	public void updateState(int node, int key, int value, int time) {
		if (time > timestamps[node][key]) {
			values[node][key] = value;
			timestamps[node][key] = time;
			maxTime[node] = Integer.max(maxTime[node], time);
			ScuttlebuttObserver.signalUpdate(self,node,key);
		} else { // Signal protocol error
			System.err.println("Found obsolete update in deltaSet at node " + self + " for node " + node);
		}
	}
	
	/**
	 *
	 * @param key
	 * @param value
	 */
	public void updateSelf(int  key, int value) {
		currTime++;
		updateState(self, key, value, currTime);
	}
	
	public int[] getDigest() {
		return maxTime;
	}
	
	public int getValue(int node, int key) {
		return values[node][key];
	}
	
	public int getTimestamp(int node, int key) {
		return timestamps[node][key];
	}
	
	int[] getState(int id) {
		return timestamps[id];
	}

	/**
	 * Updates this databases reconciling the differences received from another
	 * peer. Independent of reconciliation mechanism, simply update (p,k,v,n)
	 * tuples.
	 *
	 * @param deltaSet
	 */
	public void reconcile(DeltaSet deltaSet) {
		while (deltaSet.next()) {
			updateState(deltaSet.getNode(), deltaSet.getKey(), deltaSet.getValue(), deltaSet.getTimestamp());
		}
	}
}