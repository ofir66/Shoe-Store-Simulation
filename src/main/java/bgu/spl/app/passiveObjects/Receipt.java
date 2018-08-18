package bgu.spl.app.passiveObjects;

/**
 * An object representing a receipt for shoe purchasing by a client or shoe manufacturing by a factory
 */

public class Receipt {
	
	/**
	 * string - the name of the service which issued the receipt
	 */
	private final String fSeller;
	/**
	 * string - the name of the service which gets this receipt (the client name or “store”)
	 */
	private final String fCustomer;
	/**
	 * string - the shoe type bought
	 */
	private final String fShoeType;
	/**
	 * boolean - indicates if the shoe was sold at a discounted price
	 */
	private final boolean fDiscount;
	/**
	 * int - tick in which this receipt was issued 
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
	
	
	public Receipt(String seller, String customer, String shoeType, boolean discount, int issuedTick, int requestTick, int amountSold){
		this.fSeller=seller;
		this.fCustomer=customer;
		this.fShoeType=shoeType;
		this.fDiscount=discount;
		this.fIssuedTick=issuedTick;
		this.fRequestTick=requestTick;
		this.fAmountSold=amountSold;
	}
	
	
	public final String getSeller(){
		return this.fSeller;
	}
	
	public final String getCustomer(){
		return this.fCustomer;
	}
	
	public final String getShoeType(){
		return this.fShoeType;
	}
	
	public boolean getDiscount(){
		return this.fDiscount;
	}
	
	public int getIssuedTick(){
		return this.fIssuedTick;
	}
	
	public int getRequestTick(){
		return this.fRequestTick;
	}
	
	public int getAmountSold(){
		return this.fAmountSold;
	}
	
}
