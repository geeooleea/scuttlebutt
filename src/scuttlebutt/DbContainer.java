package scuttlebutt;

import peersim.edsim.NextCycleEvent;

public abstract class DbContainer extends NextCycleEvent {
    protected Database db;

    public DbContainer(String s) {
        super(s);
    }

    public Database getDb() {
        return db;
    }
}
