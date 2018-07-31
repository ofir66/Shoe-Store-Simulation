package bgu.spl.app.passiveObjects;

/**
 * An object which represents information about a single type of shoe in the store (e.g., red-sneakers,
blue-sandals, etc.).
 */

public class ShoeStorageInfo {
	
	/**
	 * String - the type of the shoe (e.g., red-sneakers, blue-sandals, etc.) that this storage
		info regards.
	 */
	private final String fShoeType;
	/**
	 * int - the number of shoes of shoeType currently on the storage
	 */
	private int fAmountOnStorage;
	/**
	 * int - amount of shoes in this storage that can be sale in a discounted price.
	(i.e., if amountOnStorage is 3 and discountedAmount is 1 it means that 1 out of the 3 shoes
	have a discount, after selling this one shoe the discount will end)
	 */
	private int fDiscountedAmount;
	
    /**
     * @param shoeType  the type of the shoe that this storage
		info regards.
     * @param amountOnStorage the number of shoes of shoeType currently on the storage
     * @param  discountedAmount amount of shoes in this storage that can be sale in a discounted price.
     */
	public ShoeStorageInfo(String shoeType, int amountOnStorage, int discountedAmount){
		this.fShoeType=shoeType;
		this.fAmountOnStorage=amountOnStorage;
		this.fDiscountedAmount=discountedAmount;
	}
	
	   /**
  * @return the the type of the shoe that this storage
		info regards.
  */
	
	public String getShoeType(){
		return this.fShoeType;
	}
	
	   /**
  * @return the number of shoes of shoeType currently on the storage
  */
	
	public int getAmountOnStorage (){
		return this.fAmountOnStorage;
	}
	
	   /**
  * @return the amount of shoes in this storage that can be sale in a discounted price.
  */
	
	public int getDiscountedAmount (){
		return this.fDiscountedAmount;
	}
	   /**
  * sets the amount of shoes in this storage to {@code newValue}
  * @param newValue the value to change the amount of shoes in this storage to
  */
	
	public void setAmountOnStorage (int newValue){
		this.fAmountOnStorage=newValue;
	}
	
	   /**
* sets the amount of shoes in this storage that can be sale in a discounted price. {@code newValue}
* @param newValue the value to change the amount of shoes in this storage that can be sale in a discounted price
*/
	
	public void setDiscountedAmount(int newValue){
		this.fDiscountedAmount=newValue;
	}
	
} 
