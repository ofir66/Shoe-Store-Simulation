package bgu.spl.app.passiveObjects;

/**
 * An object which describes a schedule of a single client purchase at a specific tick.
*/

public class PurchaseSchedule {
	
  /**
  * string - the type of shoe to purchase.
  */
  private final String fShoeType;
  /**
  * int - the tick number to send the PurchaseOrderRequest at.
  */
  private final int fTick;


  public PurchaseSchedule(String shoeType, int tick){
    this.fShoeType=shoeType;
    this.fTick=tick;
  }


  public final String getShoeType(){
    return this.fShoeType;
  }

  public final int getTick(){
    return this.fTick;
  }
	
}
