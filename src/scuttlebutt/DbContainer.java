package scuttlebutt;

/**
 * Abstract container class to allow Application classes to access a node's database knowing the
 * underlying reconciliation mechanism.
 */
public abstract class DbContainer {
    protected Database db;

    public DbContainer(String s) {
    }

    public Database getDb() {
        return db;
    }
}
