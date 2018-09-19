package bgu.spl.app.passiveObjects;

/**
 * An object which describes a schedule of a single discount that the manager will add to a specific shoe at a specific tick.
*/

public class DiscountSchedule {
	
  /**
  *  the type of shoe to add discount to
  */
  private final String fShoeType;
  /**
  *   the tick number to add the discount at.
  */
  private final int fTick;
  /**
  *   the amount of @fShoeType items to put on discount
  */
  private final int fAmount;


  public DiscountSchedule(String shoeType, int tick, int amount){
    this.fShoeType=shoeType;
    this.fTick=tick;
    this.fAmount=amount;
  }


  public final String getShoeType(){
    return this.fShoeType;
  }

  public final int getTick(){
    return this.fTick;
  }

  public final int getAmount(){
    return this.fAmount;
  }
}
