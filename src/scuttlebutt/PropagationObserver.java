package scuttlebutt;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class PropagationObserver implements Control {
    public static final int N = 92;
    private static final String PAR_FILE = "file";

    private static int delayCount[] = new int[N];
    private static long tot = 0;

    String file;

    public PropagationObserver(String prefix) {
        file = Configuration.getString(prefix+"."+PAR_FILE);
    }

    public static void increment(int node, int key) {
        int delay = ScuttlebuttObserver.getDelay(node,key);
        if (delay > 0) {// Ignore if the update comes from the owner of the database
            delayCount[delay]++;
            tot++;
        }
    }

    @Override
    public boolean execute() {
        if (CommonState.getTime() > 0) {
            // System.err.println("EXECUTING PROPAGATION OBSERVER");
            FileWriter fileWriter = null;
            try {
                fileWriter = new FileWriter(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("time, delay, cumulative");
            double sum = 0;
            for (int i = 0; i < N; i++) {
                sum += (double)delayCount[i]/tot;
                printWriter.println(i + ", " + (double)delayCount[i]/tot + ", " + sum);
            }
            printWriter.close();
            try {
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
