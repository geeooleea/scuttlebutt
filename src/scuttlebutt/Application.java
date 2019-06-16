package scuttlebutt;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;

public class Application implements CDProtocol {

    private static final String PAR_PROT = "protocol";

    private static int pid;

    private static final int CYCLE = Configuration.getInt("global.cycle");
    private static final int DOUBLE_RATE_TIME = Configuration.getInt("global.updateDouble");
    private static final int RESTORE_RATE_TIME = Configuration.getInt("global.updateRestore");

    public Application(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
    }

    @Override
    public void nextCycle(Node node, int i) {
        long t = CommonState.getTime();
        if (t < 120 * CYCLE) {
            Database db = ((DbContainer) node.getProtocol(pid)).db;
            int k = CommonState.r.nextInt(db.getK());
            db.update((int) node.getID(), k);
            ScuttlebuttObserver.signalUpdate((int) node.getID(),k); // To compute maximum staleness
            // Doubled update rate
            if (t >= DOUBLE_RATE_TIME * CYCLE  && t < RESTORE_RATE_TIME * CYCLE) {
                k = CommonState.r.nextInt(db.getK());
                db.update((int) node.getID(), k);
                ScuttlebuttObserver.signalUpdate((int) node.getID(),k);
            }
        }
    }

    @Override
    public Object clone() {
        Application app = null;
        try {
            app = (Application) super.clone();
        } catch (CloneNotSupportedException ex) {}
        return app;
    }
}
