package bgu.spl.app.passiveObjects;

/**
 * An object which describes a schedule of a single client-purchase at a specific tick.
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
	
    /**
     * @param shoeType the type of shoe which a client wish to purchase
     * @param tick the number to send the PurchaseOrderRequest at. 
     */
	public PurchaseSchedule(String shoeType, int tick){
		this.fShoeType=shoeType;
		this.fTick=tick;
	}
	
	
    /**
     * @return the shoeType that the client wish to purchase
     */
	
	public final String getShoeType(){
		return this.fShoeType;
	}
	
    /**
     * @return the tick which the client want to purchase the shoe type defined in this PurchaseSchedule
     */
	
	public final int getTick(){
		return this.fTick;
	}
	
}
