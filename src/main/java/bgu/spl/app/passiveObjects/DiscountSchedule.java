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
	 *   the tick number to send the add the discount at.
	 */
	private final int fTick;
	/**
	 *   the amount of items to put on discount (i.e., if the amount is 3 than after selling
			3 items the discount should be over).
	 */
	private final int fAmount;
	
    /**
     * @param shoeType the type of shoe to add discount to
     * @param tick the tick number to send the add the discount at.
     * @param amount the amount of items to put on discount
     */
	
	public DiscountSchedule(String shoeType, int tick, int amount){
		this.fShoeType=shoeType;
		this.fTick=tick;
		this.fAmount=amount;
	}
	
    /**
     * @return the shoe type of this DiscountSchedule. the shoe type is given to it in construction time 
     */
	
	public final String getShoeType(){
		return this.fShoeType;
	}
	
    /**
     * @return the tick of this DiscountSchedule. the tick is given to it in construction time
     */
	public final int getTick(){
		return this.fTick;
	}
	
    /**
     * @return the shoe type which the manager wants to add discount to
     */
	public final int getAmount(){
		return this.fAmount;
	}
}
