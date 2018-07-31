package bgu.spl.app.passiveObjects;
 
 
 

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
 
 
/**
 * The store object holds a collection of ShoeStorageInfo: One for each shoe type the store offers. In
	addition, it contains a list of receipts issued to and by the store.
 */
 
public class Store {
    
	/**
	 * Logger - a logger for printing commands
	 */
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	/**
	 * A ConcurrentLinkedQueue of {@link ShoeStorageInfo}. represents the list of shoes in the store
	 */
    private final ConcurrentLinkedQueue<ShoeStorageInfo> fShoesList;
	/**
	 * A ConcurrentLinkedQueue of {@link Receipt}. represents the list of shoes in the store
	 */
    private final ConcurrentLinkedQueue<Receipt> fReceiptsList;
	/**
	 * An object represents a lock, which locks the methods add, addDiscount and take. this lock is for preventing this case:
	 * <p>
	 * A manager might add/add discount to some shoe, and this adding might mean that afterwards the seller need to sell this shoe
	 * at regular price/discounted price, when he couldn't do this before the adding.
	 * However, a possible scenario here is that the add/addDiscount method (called by manager) will be finished only after the seller
	 * sold the shoe (with take method)  
	 */
    private final Object fLockAddAndTake; 
    
	/**
	 * An enum that represents result of {@link Store#take(String, boolean) take} method
	 */
    
    public enum BuyResult { 
        NOT_IN_STOCK, NOT_ON_DISCOUNT, REGULAR_PRICE, DISCOUNTED_PRICE
    }
     
     
    private static class StoreHolder {
          private static Store instance = new Store();
    }
                 
    private Store(){
        this.fShoesList= new ConcurrentLinkedQueue<ShoeStorageInfo>();
        this.fReceiptsList= new ConcurrentLinkedQueue<Receipt>();
        this.fLockAddAndTake=new Object();
    }
    
	/**
	 * @return the instance of our Store
	 */
     
    public static Store getInstance() {
         return StoreHolder.instance;
    }
     
     
	  /**
	   * This method will be called in order to initialize the store storage before starting an execution
		 (by the ShoeStoreRunner). The method will add the items in the given array to
		 the store.
	   * @param storage the shoes to load the Store with
	  */
    
    public void load(ShoeStorageInfo [] storage){
        this.fShoesList.clear();
        for (int i=0; i<storage.length; ++i)
            this.fShoesList.add(storage[i]);
    }
     
	  /**
	   * This method will attempt to take a single showType from the store. It receives the shoeType to
		take and a boolean - onlyDiscount which indicates that the caller wish to take the item only if it is
		in discount. Its result is an enum which his value represent the result of the attempt to take a shoe from the store.
	   * @param shoeType the shoe we wish to take
	   * @param onlyDiscount indicates if we want to take the shoe only at discount
	   * @return NOT_IN_STOCK if there were no shoe of this type in stock, NOT_ON_DISCOUNT if {@code onlyDiscount} was true and there are no
		discounted shoes with the requested type, REGULAR_PRICE if the item was successfully taken, DISCOUNTED_PRICE if the item was successfully taken in a discounted price
	  */
    
    public BuyResult take(String shoeType, boolean onlyDiscount){
        ShoeStorageInfo wantedShoe;
		
		synchronized(this.fLockAddAndTake){
	    	wantedShoe=this.findShoe(shoeType); // first we will find the shoe in our shoes list
			
	        if (wantedShoe==null) // if wasn't found
	            return BuyResult.NOT_IN_STOCK;
	        else if (wantedShoe.getAmountOnStorage()==0 && !onlyDiscount) // else, we can assume it was found. if it amount=0 and wasn't asked at discount
	            return BuyResult.NOT_IN_STOCK;
	        else if (wantedShoe.getAmountOnStorage()==0 && onlyDiscount) // if it amount=0 and was asked only at discount
	            return BuyResult.NOT_ON_DISCOUNT;
	        else if (wantedShoe.getDiscountedAmount()>0){ // else, we can assume it amount>0. if it discountAmount>0
	            wantedShoe.setAmountOnStorage((wantedShoe.getAmountOnStorage()-1));
	            wantedShoe.setDiscountedAmount(wantedShoe.getDiscountedAmount()-1);
	            return BuyResult.DISCOUNTED_PRICE;
	        }   
	        else if (wantedShoe.getDiscountedAmount()==0 && onlyDiscount) // if it discountAmount=0 and was asked only at discount
	            return BuyResult.NOT_ON_DISCOUNT;
	        else if (wantedShoe.getDiscountedAmount()==0 && !onlyDiscount){ //if it discountAmount=0 and wasn't asked on discount
	            wantedShoe.setAmountOnStorage(wantedShoe.getAmountOnStorage()-1);
	            if (wantedShoe.getDiscountedAmount()>wantedShoe.getAmountOnStorage()){  // that can happen if a restock is being ordered for a customer, and in the time between the restock order to the completion of the restock, the manager put a discount on the shoe (according to forum: the customer will still buy the shoe in regular price in case like that)
	            	wantedShoe.setDiscountedAmount(wantedShoe.getAmountOnStorage());
	            }
	            return BuyResult.REGULAR_PRICE;
	        }
			
	        return BuyResult.NOT_IN_STOCK;
        }
    }
    
    
	  /**
	   * adds the given amount to the ShoeStorageInfo of the given {@code shoeType}.
	   * @param shoeType the shoe to add to the store
	   * @param amount the amount of {@code shoeType} to add to the store
	  */ 
    public void add(String shoeType, int amount){ 
    	ShoeStorageInfo foundShoe;
		
		synchronized(this.fLockAddAndTake){ // ass described in fLockAddAndTake field
	    	foundShoe=this.findShoe(shoeType);
	        if (foundShoe==null)
	            this.fShoesList.add(new ShoeStorageInfo(shoeType,amount,0));
	        else
	            foundShoe.setAmountOnStorage(foundShoe.getAmountOnStorage()+amount);
    	}
    }
     
	  /**
	   * adds the given amount of {@code shoeType} to the corresponding ShoeStorageInfo’s discountedAmount field.
	   * @param shoeType the shoe to add discount amount of it to the store
	   * @param amount the amount of {@code shoeType} to add discount on to the store
	  */ 
    public void addDiscount(String shoeType , int amount){
    	ShoeStorageInfo foundShoe;
		
		synchronized(this.fLockAddAndTake){ // ass described in fLockAddAndTake field
	    	foundShoe=this.findShoe(shoeType);
	        if (foundShoe!=null){
	            if (foundShoe.getAmountOnStorage()<amount+foundShoe.getDiscountedAmount()) // case we want more discounts that overall quantity
	                foundShoe.setDiscountedAmount(foundShoe.getAmountOnStorage()); // the discounts number will be the same as the quantity
	            else
	                foundShoe.setDiscountedAmount(foundShoe.getDiscountedAmount()+amount);
	        }
	        else
	            this.fShoesList.add(new ShoeStorageInfo(shoeType,0,0));
    	}
    }
     
	  /**
	   * Save the given {@code receipt} in the store.
	   * @param receipt the receipt to file
	  */ 
    public void file(Receipt receipt){
        this.fReceiptsList.add(receipt);
    }
     
    //Auxiliary method.  if shoe found => return it. else, return null
     
    private ShoeStorageInfo findShoe(String shoeType){
        Iterator<ShoeStorageInfo> i= this.fShoesList.iterator();
        ShoeStorageInfo foundShoe=null;
        ShoeStorageInfo temp=null;
		
        while (i.hasNext() && foundShoe==null){
            temp=i.next();
            if (temp.getShoeType().compareTo(shoeType)==0)
                foundShoe=temp;
        }
		
        return foundShoe;
    }
     
	  /**
	   * The method prints stock in the store, and the receipts that were filed to the store
	  */ 
    public void print(){
    	FileHandler handler;
		
		try {
			handler = new FileHandler("Log/Store.txt"); // create a log file to store the receipts
			handler.setFormatter(new SimpleFormatter());
			LOGGER.addHandler(handler);
		} catch (SecurityException e1) {
			LOGGER.severe(e1.getLocalizedMessage());
		} catch (IOException e1) {
			LOGGER.severe(e1.getLocalizedMessage());
		}

		printShoesStock();
		printReceipts();
    }

	private void printShoesStock(){
		Iterator<ShoeStorageInfo> shoesListIterator = this.fShoesList.iterator();
		ShoeStorageInfo tempShoesList;
		int i=1;
		
        if (!shoesListIterator.hasNext())
        	LOGGER.info("There is no shoe stock!");
        else{
        	LOGGER.info("Printing shoes stock:");
	        while (shoesListIterator.hasNext()){
	            tempShoesList=shoesListIterator.next();
	            LOGGER.info("         "+i+". "+tempShoesList.getShoeType()+": "+tempShoesList.getAmountOnStorage()+
							" items on storage, "+tempShoesList.getDiscountedAmount()+ " of them has a discount");
		        i++;
	        }
        }
	}
	
	private void printReceipts(){
        Iterator<Receipt> receiptListIterator = this.fReceiptsList.iterator();
		int i=1;
		
        if (!receiptListIterator.hasNext())
        	LOGGER.info("There are no receipts to print!");
        else{
        	LOGGER.info("Printing receipts:");
		    while (receiptListIterator.hasNext()){
		       Receipt tempReceiptList=receiptListIterator.next();
		       LOGGER.info("    Receipt " + i + ":\n" +
							"              Seller: " + tempReceiptList.getSeller() + "\n" +
							"              Customer: "+ tempReceiptList.getCustomer() + "\n" +
							"              Discount: "+ tempReceiptList.getDiscount() + "\n" +
							"              Shoe: "+ tempReceiptList.getShoeType() + "\n" +
							"              RequestTick: "+ tempReceiptList.getRequestTick() + "\n" +
							"              IssuedTick: "+ tempReceiptList.getIssuedTick() + "\n" +
							"              AmountSold: "+ tempReceiptList.getAmountSold());
		       i++;
		    }
        }	
	}
	
}