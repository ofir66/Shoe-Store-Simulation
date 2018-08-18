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
	 * A ConcurrentLinkedQueue of {@link ShoeStorageInfo}. Represents the list of shoes in the store
	 */
    private final ConcurrentLinkedQueue<ShoeStorageInfo> fShoesList;
	/**
	 * A ConcurrentLinkedQueue of {@link Receipt}. Represents the list of receipts in the store
	 */
    private final ConcurrentLinkedQueue<Receipt> fReceiptsList;
	/**
	 * A lock which is used to synchronize the methods add, addDiscount and take.
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
     
    public static Store getInstance() {
         return StoreHolder.instance;
    }
     
     
	  /**
	   * Initializes the store storage
	   * @param storage the shoes to load the store with
	  */
    
    public void load(ShoeStorageInfo [] storage){
        this.fShoesList.clear();
        for (int i=0; i<storage.length; ++i)
            this.fShoesList.add(storage[i]);
    }
     
	  /**
	   * Attempts to take a single shoe from the store.
	   * @param shoeType the wanted
	   * @param onlyDiscount indicates if the shoe is wanted only at discount
	   * @return NOT_IN_STOCK if there were no shoe of this type in stock, NOT_ON_DISCOUNT if {@code onlyDiscount} was true and there are no
		discounted shoes with the requested type, REGULAR_PRICE if the item was successfully taken, DISCOUNTED_PRICE if the item was successfully taken in a discounted price
	  */
    
    public BuyResult take(String shoeType, boolean onlyDiscount){
        ShoeStorageInfo wantedShoe;
		
		synchronized(this.fLockAddAndTake){
	    	wantedShoe=this.findShoe(shoeType);
			
	        if (wantedShoe==null)
	            return BuyResult.NOT_IN_STOCK;
	        else if (wantedShoe.getAmountOnStorage()==0 && !onlyDiscount)
	            return BuyResult.NOT_IN_STOCK;
	        else if (wantedShoe.getAmountOnStorage()==0 && onlyDiscount)
	            return BuyResult.NOT_ON_DISCOUNT;
	        else if (wantedShoe.getDiscountedAmount()>0){
	            wantedShoe.setAmountOnStorage((wantedShoe.getAmountOnStorage()-1));
	            wantedShoe.setDiscountedAmount(wantedShoe.getDiscountedAmount()-1);
	            return BuyResult.DISCOUNTED_PRICE;
	        }   
	        else if (wantedShoe.getDiscountedAmount()==0 && onlyDiscount)
	            return BuyResult.NOT_ON_DISCOUNT;
	        else if (wantedShoe.getDiscountedAmount()==0 && !onlyDiscount){
	            wantedShoe.setAmountOnStorage(wantedShoe.getAmountOnStorage()-1);
	            if (wantedShoe.getDiscountedAmount()>wantedShoe.getAmountOnStorage()){
	            	wantedShoe.setDiscountedAmount(wantedShoe.getAmountOnStorage());
	            }
	            return BuyResult.REGULAR_PRICE;
	        }
			
	        return BuyResult.NOT_IN_STOCK;
        }
    }
    
    
	  /**
	   * adds some shoe type units to the store (no discount)
	   * @param shoeType the shoe to add to the store
	   * @param amount the amount of {@code shoeType} to add to the store
	  */ 
    public void add(String shoeType, int amount){ 
    	ShoeStorageInfo foundShoe;
		
		synchronized(this.fLockAddAndTake){
	    	foundShoe=this.findShoe(shoeType);
	        if (foundShoe==null)
	            this.fShoesList.add(new ShoeStorageInfo(shoeType,amount,0));
	        else
	            foundShoe.setAmountOnStorage(foundShoe.getAmountOnStorage()+amount);
    	}
    }
     
	  /**
	   * adds some shoe type units to the store with discount
	   * @param shoeType the shoe to add to the store
	   * @param amount the amount of {@code shoeType} to add to the store
	  */ 
    public void addDiscount(String shoeType , int amount){
    	ShoeStorageInfo foundShoe;
		
		synchronized(this.fLockAddAndTake){
	    	foundShoe=this.findShoe(shoeType);
	        if (foundShoe!=null){
	            if (foundShoe.getAmountOnStorage()<amount+foundShoe.getDiscountedAmount())
	                foundShoe.setDiscountedAmount(foundShoe.getAmountOnStorage());
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
     
	  /**
	   * Auxiliary method.  
	   * If shoe found => return it. Otherwise, return null
	  */     
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
	   * Prints the stock in the store, and the receipts that were filed to the store
	  */ 
    public void print(){
    	FileHandler handler;
		
		try {
			handler = new FileHandler("Log/Store.txt");
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