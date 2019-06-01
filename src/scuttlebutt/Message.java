package scuttlebutt;

import peersim.core.Node;

/**
 *
 * @author Giulia Carocari
 */
public class Message {
    public enum ACTION {
        DIGEST,
        DIGEST_RESPONSE,
        DELTA_SET
    }

    // These are protected to simplify read access in reconciliation classes
    protected final ACTION action;
    protected final Node sender;
    protected Object payload;

    /**
     * Create a new gossip message.
     *
     * @param sender
     * @param payload
     * @param a
     */
    public Message(Node sender, Object payload, ACTION a) {
        this.sender = sender;
        this.payload = payload;
        this.action = a;
    }


}
