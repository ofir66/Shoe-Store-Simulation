package bgu.spl.app.messages;

import bgu.spl.mics.Request;

/**
 * A request that is sent by the selling service to the store manager so that he
will know that he needs to order new shoes from a factory.
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
  * int- the needed amount of the shoe to restock
  */
  private int fAmountNeeded;
  /**
  * PurchaseOrderRequest- the {@link PurchaseOrderRequest} which caused the sender of this restock message to send it
  */
  private final PurchaseOrderRequest fPurchaseOrderRequest;


  public RestockRequest(int id, String shoeNeeded, int amountNeeded, PurchaseOrderRequest purchaseOrderRequest) {
    this.fId = id;
    this.fShoeNeeded=shoeNeeded;
    this.fAmountNeeded=amountNeeded;
    this.fPurchaseOrderRequest=purchaseOrderRequest;
  }


  public int getId() {
    return this.fId;
  }

  public String getShoeNeeded(){
    return this.fShoeNeeded;
  }

  public int getAmountNeeded(){
    return this.fAmountNeeded;
  }

  public PurchaseOrderRequest getPurchaseOrderRequest(){
    return this.fPurchaseOrderRequest;
  }
    
}

