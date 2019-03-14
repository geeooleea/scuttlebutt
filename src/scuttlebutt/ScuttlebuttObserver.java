/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scuttlebutt;

import java.util.ArrayList;
import java.util.List;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 *
 * @author Giulia Carocari
 */
public class ScuttlebuttObserver implements Control {

	private static final String PAR_PROT = "protocol";

	private final int pid;

	private static final int N = Network.size();

	private static int K;

	private static long times[][][];

	protected static void setK(int k) { K = k; }

	public ScuttlebuttObserver(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		times = new long[N][N][K];
	}

	@Override
	public boolean execute() {
		int count = 0; long maxStale = 0;
		boolean isStale[][] = new boolean[N][K];
		int currState[], compState[];
		long time = CommonState.getTime();
		for (int i = 0; i < N; i++) {
			Node current = Network.get(i);
			currState = ((Scuttlebutt) current.getProtocol(pid)).getState(current);
			for (int j = 0; j < N; j++) {
				if (i == j) {
					continue;
				}
				compState = ((Scuttlebutt) Network.get(j).getProtocol(pid)).getState(current);
				K = compState.length;
				for (int k = 0; k < K; k++) {
					if (currState[k] != compState[k] && !isStale[(int)current.getID()][k]) {
						maxStale = Long.max(time - times[j][i][k], maxStale);
						count++;
						// maxStale = Integer.max(maxStale, realTime(currState[k]) - realTime(compState[k]));
						isStale[(int) current.getID()][k] = true;
					}
				}
			}
		}

		System.out.println(maxStale + ", " + count);
		return false;
	}

	protected static void signalUpdate(int self, int node, int key) {
		times[self][node][key] = CommonState.getTime();
	}
}
