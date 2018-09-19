package bgu.spl.app.messages;

import bgu.spl.app.passiveObjects.Receipt;
import bgu.spl.mics.Request;

/**
* a request that is sent when the a store client wish to buy a shoe.
Its response type expected to be a Receipt.  
In the case the purchase was not completed successfully null will be returned as the request result.
*/
public class PurchaseOrderRequest implements Request<Receipt>{
	
  /**
  * String- name of the sender
  */
  private String fSenderName; 
  /**
  * String- name of the shoe requested
  */
  private String fShoeRequested;
  /**
  * boolean- indicates if the sender wants the shoe in the message only at discount
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


  public PurchaseOrderRequest(String senderName, String shoeRequested, boolean discount, int requestTick, int amountWanted) {
    this.fSenderName = senderName;
    this.fShoeRequested=shoeRequested;
    this.fDiscount=discount;
    this.fRequestTick=requestTick;
    this.fAmountWanted=amountWanted;
  }


  public String getSenderName() {
    return fSenderName;
  }

  public String getShoeRequested() {
    return this.fShoeRequested;
  }

  public boolean getDiscount(){
    return this.fDiscount;
  }

  public int getRequestTick(){
    return this.fRequestTick;
  }

  public int getAmountWanted(){
    return this.fAmountWanted;
  }
}
