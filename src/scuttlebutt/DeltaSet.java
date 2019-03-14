/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scuttlebutt;

/**
 * Implementation of the delta set used by two peers to reconcile their
 * databases. How the delta set is created depends on the protocol configuration.
 * Allows for iterable-like access to deltas in the set.
 *
 * @author Giulia Carocari
 */
public class DeltaSet {

	private int nodes[];
	private int keys[];
	private int values[];
	private int timestamps[];

	/**
	 * Number of entries in the deltaSet.
	 */
	private int N;

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
	 * 
	 * @param node
	 * @param key
	 * @param value
	 * @param timestamp
	 */
	public void put(int node, int key, int value, int timestamp) {
		// size += value;
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

	int getNode() {
		return nodes[i];
	}

	int getKey() {
		return keys[i];
	}

	int getValue() {
		return values[i];
	}

	int getTimestamp() {
		return timestamps[i];
	}

}
