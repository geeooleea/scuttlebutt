package scuttlebutt;

import peersim.core.Node;

/**
 *
 * @author Giulia Carocari
 */
public class Message {
    public static enum ACTION {
        DIGEST,
        DIGEST_RESPONSE,
        DELTA_SET
    }

    protected final ACTION action;
    protected final Node sender;
    protected Object payload;

    public Message(Node sender, Object payload, ACTION a) {
        this.sender = sender;
        this.payload = payload;
        this.action = a;
    }


}
