package scuttlebutt.bitcoin;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.edsim.EDSimulator;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

public class BitcoinInit implements Control {
    private static final String PAR_PROT = "protocol";
    private static final String PAR_FILE = "file";

    private int pid;
    private String filename;

    public BitcoinInit(String prefix) {
        pid = Configuration.getPid(prefix+"."+PAR_PROT);
        filename = Configuration.getString(prefix+"."+PAR_FILE);
    }

    @Override
    public boolean execute() {
        try {
            FileReader fr = new FileReader(filename);
            LineNumberReader lnr = new LineNumberReader(fr);
            String line; int delay;
            while ((line = lnr.readLine()) != null) {
                try {
                    delay = Integer.parseInt(line);
                    if (delay >= 0)
                        EDSimulator.add(delay*10000,null, Network.get(CommonState.r.nextInt(Network.size())),pid);
                    else
                        throw new RuntimeException(line + " is not a positive delay");
                } catch (NumberFormatException ex) {
                    throw new RuntimeException(line + " is not a positive delay");
                }
            }
            lnr.close();
        } catch(IOException e) {
            throw new RuntimeException("Unable to read file: " + e);
        }
        return false;
    }
}
