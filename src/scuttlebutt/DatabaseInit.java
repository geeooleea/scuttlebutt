/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scuttlebutt;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

/**
 * Reads from the configuration file and sets a new database.
 *
 * @author Giulia Carocari
 */
public class DatabaseInit implements Control {

	private static final String PAR_PROT = "protocol";

	private static final String PAR_KEY = "keys";

	// ID of the protocol to initialize
	private final int pid;
	
	private static final int N = Network.size();

	private int k;

	// Load the protocol and number of keys from configuration
	public DatabaseInit(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		k = Configuration.getInt(prefix + "." +  PAR_KEY);
	}

	@Override
	public boolean execute() {

		Database prototype = new Database(k);
		Application.setK(k);
		for (int i=0; i<N; i++) {
			((Scuttlebutt)Network.get(i).getProtocol(pid)).setDatabase((Database)prototype.clone());
		}
		return true;
	}

}
