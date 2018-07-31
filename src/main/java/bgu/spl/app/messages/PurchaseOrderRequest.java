package bgu.spl.app.messages;

import bgu.spl.app.passiveObjects.Receipt;
import bgu.spl.mics.Request;

/**
 * a request that is sent when the a store client wish to buy a shoe.
Its response type expected to be a Receipt.  On the case the purchase was not completed
successfully null should be returned as the request result
 */
public class PurchaseOrderRequest implements Request<Receipt>{
	
	/**
	 * String- name of the sender of the request
	 */
	private String fSenderName; 
	/**
	 * String- name of the shoe requeted in this request
	 */
	private String fShoeRequested;
	/**
	 * boolean- indicates if the sender wants the shoe in the message only on discount
	 */
	private boolean fDiscount;
	/**
	 * int- the tick in which the customer requested the shoe
	 */
	private int fRequestTick;
	/**
	 * the amount wanted by the sender
	 */
	private int fAmountWanted;
	
    /**
     * @param senderName name of the sender.
     * @param shoeRequested the shoe that the sender wants to buy
     * @param discount indicates if the sender wants the shoe in the message only on discount
     * @param requestTick the tick in which the customer requested the shoe
     * @param the amount wanted by the sender
     */
	
	public PurchaseOrderRequest(String senderName, String shoeRequested, boolean discount, int requestTick, int amountWanted) {
        this.fSenderName = senderName;
        this.fShoeRequested=shoeRequested;
        this.fDiscount=discount;
        this.fRequestTick=requestTick;
        this.fAmountWanted=amountWanted;
    }
	
	
    /**
     * @return the sender of this message
     */
	
    public String getSenderName() {
        return fSenderName;
    }
    
    /**
     * @return the shoe requested by the sender
     */
    public String getShoeRequested() {
        return this.fShoeRequested;
    }
    
    /**
     * @return true if the customer wants tu buy this shoe only at discount, false otherwise
     */
    public boolean getDiscount(){
    	return this.fDiscount;
    }
    
    /**
     * @return the tick in which the customer requested the shoe
     */
    
    public int getRequestTick(){
    	return this.fRequestTick;
    }
    
    /**
     * @return the amount wanted by the sender 
     */
    public int getAmountWanted(){
    	return this.fAmountWanted;
    }
}
