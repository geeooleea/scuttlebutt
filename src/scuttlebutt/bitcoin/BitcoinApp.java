package scuttlebutt.bitcoin;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import scuttlebutt.Database;
import scuttlebutt.DbContainer;

public class BitcoinApp implements EDProtocol {

    private static final String PAR_PROT = "protocol";

    private int pid;

    public BitcoinApp(String prefix) {
        pid = Configuration.getPid(prefix+"."+PAR_PROT);
    }

    @Override
    public void processEvent(Node node, int i, Object event) {
        Database db = ((DbContainer)node.getProtocol(pid)).getDb();
        db.update((int) node.getID(), CommonState.r.nextInt(Database.getK()));
    }

    @Override
    public Object clone() {
        try {
            BitcoinApp app = (BitcoinApp) super.clone();
            app.pid = pid;
            return app;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }
}