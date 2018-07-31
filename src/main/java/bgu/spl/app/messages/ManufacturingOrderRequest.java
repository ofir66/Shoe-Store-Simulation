package bgu.spl.app.messages;

import bgu.spl.app.passiveObjects.Receipt;
import bgu.spl.mics.Request;

/**
 * A request that is sent when the the store manager want that a
shoe factory will manufacture a shoe for the store. Its response type expected to be a Receipt.
On the case the manufacture was not completed successfully null should be returned as the
request result.
 */

public class ManufacturingOrderRequest implements Request<Receipt>{
	
	/**
	 * String- name of the sender
	 */
	private String fSenderName;
	/**
	 * String- the type of the shoe
	 */
	private String fShoeType;
	/**
	 * int- the amount needed of this shoe to manufacture
	 */
	private int fAmountNeeded;
	/**
	 * int- the tick in which the manufacturing request was sent
	 */
	private int fRequestedTick;
	
    /**
     * @param senderName name of the sender.
     * @param amountNeeded the amount needed of this shoe to manufacture.
     * @param  requestedTick the tick in which the manufacturing request was sent.
     */
	
	public ManufacturingOrderRequest(String senderName, String shoeType, int amountNeeded, int requestedTick) {
        this.fSenderName = senderName;
        this.fShoeType=shoeType;
        this.fAmountNeeded=amountNeeded;
        this.fRequestedTick=requestedTick;
    }
	
	
    /**
     * @return the name of the service - the service name is given to it in the
     *         construction time and is used mainly for debugging purposes.
     */
	
    public String getSenderName() {
        return this.fSenderName;
    }
    
    /**
     * @return the name of the service - the service name is given to it in the
     *         construction time and is used mainly for debugging purposes.
     */
    public String getShoeType(){
    	return this.fShoeType;
    }
    
    public int getAmountNeeded(){
    	return this.fAmountNeeded;
    }
    
    public int getRequestedTick(){
    	return this.fRequestedTick;
    }
}
