package scuttlebutt;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.NextCycleEvent;

public class Application extends NextCycleEvent implements CDProtocol {

    private static final int CYCLE = 10000;

    private static final String PAR_PROT = "protocol";

    private static int pid;

    public Application(String prefix) {
        super(prefix);
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
    }

    @Override
    public void nextCycle(Node node, int i) {
        if (CommonState.getTime() < 120 * CYCLE) {
            Database db = ((DbContainer) node.getProtocol(pid)).db;

            db.update((int) node.getID(), CommonState.r.nextInt(db.getK()));
            // Doubled update rate
            if (CommonState.getTime() >= 25 * CYCLE && CommonState.getTime() < 75 * CYCLE) {
                db.update((int) node.getID(), CommonState.r.nextInt(db.getK()));
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
