/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scuttlebutt;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 *
 * @author Giulia Carocari
 */
public class Application implements Control {

	private static final String PAR_PROT = "protocol";

	private static final String PAR_MAX_SIZE = "maxsize";

	private static final String PAR_MIN_SIZE = "minsize";

	private final int pid;

	private static final int N = Network.size();

	private static int K;

	private boolean exec = false;

	public static void setK(int K) {
		Application.K = K;
	}

	private static int maxs;
	private static int mins;
	public Application(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		maxs = Configuration.getInt(prefix + "." + PAR_MAX_SIZE);
		mins = Configuration.getInt(prefix + "." + PAR_MIN_SIZE, 0);
	}

	@Override
	public boolean execute() {
		long time = CommonState.getTime();
		long endtime = CommonState.getEndTime();

		if (time < endtime / 3) {
			exec = !exec;
		} else if (time < 2*endtime/3) {
			exec = true;
		} else {
			exec = false;
		}
		
		if (exec) {
			for (int i = 0; i < N; i++) {
				Node node = Network.get(i);
				((Scuttlebutt) node.getProtocol(pid)).updateSelf((int) node.getID(), CommonState.r.nextInt(K),
					mins + 1 + CommonState.r.nextInt(maxs-1));
			}
		}
		
		return false;
	}

}
