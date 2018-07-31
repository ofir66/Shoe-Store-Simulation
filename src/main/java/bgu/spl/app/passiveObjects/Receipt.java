package bgu.spl.app.passiveObjects;

/**
 * An object representing a receipt that should be sent to a client after buying a shoe (when the client’s
PurchaseRequest completed).
 */

public class Receipt {
	
	/**
	 * string - the name of the service which issued the receipt
	 */
	private final String fSeller;
	/**
	 * string - string - the name of the service this receipt issued to (the client name or “store”)
	 */
	private final String fCustomer;
	/**
	 * string - string - the shoe type bought
	 */
	private final String fShoeType;
	/**
	 * boolean - indicating if the shoe was sold at a discounted price
	 */
	private final boolean fDiscount;
	/**
	 * int - tick in which this receipt was issued (upon completing the corresponding
		request).
	 */
	private final int fIssuedTick;
	/**
	 * int - tick in which the customer requested to buy the shoe.
	 */
	private final int fRequestTick;
	/**
	 * int - the amount of shoes sold
	 */
	private final int fAmountSold;
	
    /**
     * @param seller the name of the service which issued the receipt
     * @param customer the name of the service this receipt issued to
     * @param shoeType the shoe type bought
     * @param discount indicating if the shoe was sold at a discounted price
     * @param issuedTick tick in which this receipt was issued
     * @param requestTick tick in which the customer requested to buy the shoe.
     * @param amountSold the amount of shoes sold
     */
	
	public Receipt(String seller, String customer, String shoeType, boolean discount, int issuedTick, int requestTick, int amountSold){
		this.fSeller=seller;
		this.fCustomer=customer;
		this.fShoeType=shoeType;
		this.fDiscount=discount;
		this.fIssuedTick=issuedTick;
		this.fRequestTick=requestTick;
		this.fAmountSold=amountSold;
	}
	
	
    /**
     * @return the name of the service which issued the receipt
     */
	
	public final String getSeller(){
		return this.fSeller;
	}
	
	   /**
     * @return the name of the service this receipt issued to
     */
	
	public final String getCustomer(){
		return this.fCustomer;
	}
	
	   /**
     * @return the shoe type bought
     */
	
	public final String getShoeType(){
		return this.fShoeType;
	}
	
	   /**
     * @return true if the shoe this receipt relates is wat in discount. otherwise- false.
     */
	
	public boolean getDiscount(){
		return this.fDiscount;
	}
	
	   /**
     * @return the tick in which this receipt was issued
     */
	
	public int getIssuedTick(){
		return this.fIssuedTick;
	}
	
	   /**
     * @return the tick in which the customer requested to buy the shoe.
     */
	
	public int getRequestTick(){
		return this.fRequestTick;
	}
	   /**
     * @return the amount of shoes sold
     */
	
	public int getAmountSold(){
		return this.fAmountSold;
	}
	
}
