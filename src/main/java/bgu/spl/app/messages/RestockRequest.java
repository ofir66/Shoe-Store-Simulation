package bgu.spl.app.messages;

import bgu.spl.mics.Request;

/**
 * A request that is sent by the selling service to the store manager so that he
will know that he need to order new shoes from a factory. Its response type expected to be a
boolean where the result: true means that the order is complete and the shoe is reserved for
the selling service and the result: false means that the shoe cannot be ordered (because there
were no factories available)
 */
public class RestockRequest implements Request<Boolean>{
	
	/**
	 * int- the id of the sender of this message
	 */
	private int fId; 
	/**
	 * String- the shoe needed by the sender for restock
	 */
	private String fShoeNeeded;
	/**
	 * int- the amount needed of the shoe to restock
	 */
	private int fAmountNeeded;
	/**
	 * PurchaseOrderRequest- the {@link PurchaseOrderRequest} that caused the sender of this message to send it
	 */
	private final PurchaseOrderRequest fPurchaseOrderRequest;
	
	/**
	 * 
	 * @param id the id of the sender of this message
	 * @param shoeNeeded the shoe needed by the sender for restock
	 * @param amountNeeded the amount needed of the shoe to restock
	 * @param purchaseOrderRequest the {@link PurchaseOrderRequest} that caused the sender of this message to send it
	 */
	
	public RestockRequest(int id, String shoeNeeded, int amountNeeded, PurchaseOrderRequest purchaseOrderRequest) {
        this.fId = id;
        this.fShoeNeeded=shoeNeeded;
        this.fAmountNeeded=amountNeeded;
        this.fPurchaseOrderRequest=purchaseOrderRequest;
    }
	
	
	/**
	 * 
	 * @return the id of the sender of this message
	 */
	
    public int getId() {
        return this.fId;
    }
    /**
     * 
     * @return the shoe needed by the sender for restock
     */
    public String getShoeNeeded(){
    	return this.fShoeNeeded;
    }
    
    /**
     * 
     * @return the amount needed of the shoe to restock
     */
    public int getAmountNeeded(){
    	return this.fAmountNeeded;
    }
    
    /**
     * 
     * @return the {@link PurchaseOrderRequest} that caused the sender of this message to send it
     */
    public PurchaseOrderRequest getPurchaseOrderRequest(){
    	return this.fPurchaseOrderRequest;
    }
    
}

