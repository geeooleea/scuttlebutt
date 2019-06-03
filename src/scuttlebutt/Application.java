package scuttlebutt;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;

public class Application implements CDProtocol {

    private static final String PAR_PROT = "protocol";

    private static int pid;

    public Application(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
    }

    @Override
    public void nextCycle(Node node, int i) {
        if (CommonState.getTime() < 120 * Configuration.getInt("global.cycle")) {
            Database db = ((DbContainer) node.getProtocol(pid)).db;
            int k = CommonState.r.nextInt(db.getK());
            db.update((int) node.getID(), k);
            ScuttlebuttObserver.signalUpdate((int) node.getID(),k); // To compute maximum staleness
            // Doubled update rate
            if (CommonState.getTime() >= 25 * Configuration.getInt("global.cycle") &&
                    CommonState.getTime() < 75 * Configuration.getInt("global.cycle")) {
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
            app.pid = this.pid;
        } catch (CloneNotSupportedException ex) {}
        return app;
    }
}
