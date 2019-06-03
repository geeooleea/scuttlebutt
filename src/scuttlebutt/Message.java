package scuttlebutt;

import peersim.core.Node;

/**
 *
 * @author Giulia Carocari
 */
public class Message {
    public enum ACTION {
        DIGEST,
        DIGEST_RESPONSE, // Also contains delta set for first digest message
        DELTA_SET
    }

    // These are protected to simplify read access in reconciliation classes
    protected final ACTION action;
    protected final Node sender;
    protected DeltaSet deltaSet;
    protected Object digest;

    /**
     * Create a new gossip message.
     *
     * @param sender
     * @param digest
     * @param deltaSet
     * @param a
     */
    public Message(Node sender, Object digest, DeltaSet deltaSet, ACTION a) {
        this.sender = sender;
        this.digest = digest;
        this.deltaSet = deltaSet;
        this.action = a;
    }


}
