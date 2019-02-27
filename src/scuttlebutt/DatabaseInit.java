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

	private static final String PAR_FILE = "file";

	// Name of the file from the configuration file
	private final String fileName;

	// ID of the protocol to initialize
	private final int pid;
	
	private static final int N = Network.size();

	// Load the file and the protocol from configuration
	public DatabaseInit(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		fileName = Configuration.getString(prefix + "." + PAR_FILE);
	}

	@Override
	public boolean execute() {
		int k;
		Database prototype;
		try {
			FileReader fr = new FileReader(fileName);
			LineNumberReader lnr = new LineNumberReader(fr);
			String line = lnr.readLine();
			try {
				k = Integer.parseInt(line);
				Application.setK(k);
			} catch (NumberFormatException ex) {
				throw new RuntimeException("First line of database file is not a size.");
			}
			prototype = new Database(k);
			for (int i = 0; i < k; i++) {
				line = lnr.readLine();
				int val;
				try {
					val = Integer.parseInt(line);
				} catch (NumberFormatException ex) {
					throw new RuntimeException("Line " + i+2 + " is not an integer value");
				}
				for (int j=0; j<N; j++) {
					prototype.updateState(j, i, val, 0);
				}
			}
			
			for (int i=0; i<N; i++) {
				((Scuttlebutt)Network.get(i).getProtocol(pid)).setDatabase((Database)prototype.clone());
			}
		} catch (IOException ex) {
			throw new RuntimeException("Unable to read file: " + ex);
		}

		return true;
	}

}
