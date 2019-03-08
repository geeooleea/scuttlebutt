/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scuttlebutt;

/**
 * Implementation of the delta set used by two peers to reconcile their
 * databases. How the delta set is created is protocol-dependent.
 *
 * @author Giulia Carocari
 */
public class DeltaSet {

	private int nodes[];
	private int keys[];
	private int values[];
	private int timestamps[];

	/**
	 * Number of entries for a given node. Maybe to be used in
	 * scuttlebutt#scuttledepth
	 */
	private int entryNum[];

	/**
	 * Number of entries in the deltaSet.
	 */
	private int N;

	/**
	 * Keep total size of values in constant time
	 */
	private long size = 0;

	private int i = -1;
	
	/**
	 *
	 * @param n Number of nodes
	 * @param k Number of keys
	 */
	public DeltaSet(int n, int k) {
		// Allocate enough space to fit all possible entries.
		nodes = new int[n*k];
		keys = new int[n*k];
		values = new int[n*k];
		timestamps = new int[n*k];
	}

	/**
	 * Get the number of entries currently in the delta set.
	 *
	 * @return
	 */
	public int entryNumber() {
		return N;
	}

	/**
	 * Get the total size of the entries.
	 * @return 
	 */
	public long size() {
		return size;
	}

	/**
	 * 
	 * @param node
	 * @param key
	 * @param value
	 * @param timestamp
	 */
	public void put(int node, int key, int value, int timestamp) {
		size += value;
		nodes[N] = node;
		keys[N] = key;
		values[N] = value;
		timestamps[N] = timestamp;
		N++;
	}

	boolean next() {
		i++;
		if (i < N) {
			return true;
		} else {
			i=0; // Reset for next iteration
			return false;
		}
	}

	int nextNode() {
		return nodes[i];
	}

	int nextKey() {
		return keys[i];
	}

	int nextValue() {
		return values[i];
	}

	int nextTimestamp() {
		return timestamps[i];
	}

}
