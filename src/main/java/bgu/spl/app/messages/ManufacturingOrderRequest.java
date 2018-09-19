package bgu.spl.app.messages;

import bgu.spl.app.passiveObjects.Receipt;
import bgu.spl.mics.Request;

/**
* A request that is sent when the the store manager decides that a
shoe factory will manufacture a shoe for the store. Its response type expected to be a Receipt.
If the manufacture was not completed successfully null will be returned as the
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


  public ManufacturingOrderRequest(String senderName, String shoeType, int amountNeeded, int requestedTick) {
    this.fSenderName = senderName;
    this.fShoeType=shoeType;
    this.fAmountNeeded=amountNeeded;
    this.fRequestedTick=requestedTick;
  }


  public String getSenderName() {
    return this.fSenderName;
  }

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
