package bgu.spl.app.passiveObjects;

/**
 * An object which represents information about a single type of shoe in the store.
 */

public class ShoeStorageInfo {
	
	/**
	 * String - the type of the shoe
	 */
	private final String fShoeType;
	/**
	 * int - the number of fShoeType units in the storage
	 */
	private int fAmountOnStorage;
	/**
	 * int - the number of fShoeType units in the storage that are on discount price
	 */
	private int fDiscountedAmount;
	

	public ShoeStorageInfo(String shoeType, int amountOnStorage, int discountedAmount){
		this.fShoeType=shoeType;
		this.fAmountOnStorage=amountOnStorage;
		this.fDiscountedAmount=discountedAmount;
	}
	

	public String getShoeType(){
		return this.fShoeType;
	}
	
	public int getAmountOnStorage (){
		return this.fAmountOnStorage;
	}
	
	public int getDiscountedAmount (){
		return this.fDiscountedAmount;
	}
	
	public void setAmountOnStorage (int newValue){
		this.fAmountOnStorage=newValue;
	}
	
	public void setDiscountedAmount(int newValue){
		this.fDiscountedAmount=newValue;
	}
	
} 
