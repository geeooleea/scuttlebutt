/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
	
	public final ACTION action;
	private final Node sender;
	private Object payload;
	
	public Node getSender() {
		return sender;
	}

	public Object getPayload() {
		return payload;
	}

	public void setPayload(Object payload) {
		this.payload = payload;
	}
	
	public Message(Node sender, Object payload, ACTION a) {
		this.sender = sender;
		this.payload = payload;
		this.action = a;
	}
	
	
}
