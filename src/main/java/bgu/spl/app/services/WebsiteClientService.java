package bgu.spl.app.services;
 
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.app.messages.NewDiscountBroadcast;
import bgu.spl.app.messages.PurchaseOrderRequest;
import bgu.spl.app.messages.TickBroadcast;
import bgu.spl.app.passiveObjects.PurchaseSchedule;
import bgu.spl.mics.MicroService;

/**
 * 
 * This micro-service describes one client connected to the web-site, and tries to purchase shoes.
 *
 */
public class WebsiteClientService extends MicroService{
	
	/**
	 * Logger- a logger for printing commands
	 */
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	/**
	 * contains purchases that the client needs to make
	 */
    private final List<PurchaseSchedule> fPurchaseSchedule; 
    /**
     * the client's wish list contains name of shoe types that the client.
     * <p>
     * will buy only when there is a discount on them. 
     */
    private final Set<String> fWishList;
    /**
     * A list of items that the client has requested from his wish list
     */
    private final ConcurrentLinkedQueue<String> fRequestedFromWishList;
	/**
	 * int- the current tick in the clock
	*/
    private int fCurrentTick;
    /**
     * int- the duration of the program
     */
    private int fDuration;
    /**
     * CountDownLatch- an object for indicating when the {@link TimeService} starts sending ticks.
     * <p>
     * will be received at this micro-service constructor with the number of services not including the timer.
     * <p>
     * will count down at the end of initialize method.
     */
    private CountDownLatch fLatchObject;
    /**
     * CountDownLatch- an object for indicating when the {@link bgu.spl.app.passiveObjects.ShoeStoreRunner ShoeStoreRunner} should terminate.
     * <p>
     * will be received at this micro-service constructor with the number of services including the TimeService.
     * <p>
     * will count down at termination.
     */
    private CountDownLatch fLatchObjectForEnd;
    
    /**
     * 
     * @param name the name of the service
     * @param purchaseSchedule the list of purchases the client need to make
     * @param wishList the client's wish list (items to buy at discount)
     * @param latchObject an object for indicating when the {@link TimeService} starts sending ticks.
     * @param latchObjectForEnd an object for indicating when the {@link bgu.spl.app.passiveObjects.ShoeStoreRunner ShoeStoreRunner} should terminate.
     */
    public WebsiteClientService(String name, List<PurchaseSchedule> purchaseSchedule, Set<String> wishList, CountDownLatch latchObject, CountDownLatch latchObjectForEnd){
        super(name);
        this.fPurchaseSchedule=purchaseSchedule;
        this.fWishList=wishList;
        this.fLatchObject=latchObject;
        this.fLatchObjectForEnd=latchObjectForEnd;
        this.fRequestedFromWishList= new ConcurrentLinkedQueue<String>();
    }
     
    /**
     * via his initialize, the client subscribe to tickBroadcast and newDiscountBroadcast messages.
     * He is also trying to buy items from his purchase list
     */
    protected void initialize(){
    	FileHandler handler;
		
		try {
			handler = new FileHandler("Log/WebsiteClientService"+getName()+".txt");
			handler.setFormatter(new SimpleFormatter());
			LOGGER.addHandler(handler);
		} catch (SecurityException e1) {
			LOGGER.severe(e1.getMessage());
		} catch (IOException e1) {
			LOGGER.severe(e1.getMessage());
		}
		
        LOGGER.info(this.getName() +" started");
        // subscribing to TickBroadcast messages
        this.subscribeBroadcast(TickBroadcast.class, tickBroadCast -> { // this is how the client handles TickBroadcast message
            this.fCurrentTick=tickBroadCast.getCurrentTick(); // it saves the current tick
            this.fDuration=tickBroadCast.getDuration(); // and duration
            if (this.fCurrentTick>this.fDuration){
                this.terminate();
                LOGGER.info(this.getName()+ " terminates");
                this.fLatchObjectForEnd.countDown();
            }    
            else{
                this.buyPurchaseScheduleItems();  // and if the tick<duration, then trying to buy items on PurchaseSchedule list (if he has items on that tick)
            }   
        });
        // subscribing the client to NewDiscountBroadcast (at callBack: will try to buy items on wish list, if get's a discount on some item there)
        this.handleNewDiscountBroadcast();
        //pay attention! we don't need to iterate now on the wish list. this is because the client will need to check it only when he receives a broadcast message for discount (and we handle this case)
        this.fLatchObject.countDown();
    }   
     
    // Auxiliary method. will try to buy items on the PurchaseSchedule, at the current tick
    private void buyPurchaseScheduleItems(){
        // trying to buy items on PurchaseSchedule list.
                ConcurrentLinkedQueue<PurchaseSchedule> itemsToPurchaseAtCurrentTick= findPurchasesAtCurrentTick(); // will find all the items to put in this current tick
                Iterator<PurchaseSchedule> i= itemsToPurchaseAtCurrentTick.iterator();
				PurchaseOrderRequest purchaseOrderRequest;
				boolean success;
				
                while (i.hasNext()){
                    PurchaseSchedule temp=i.next();
                    purchaseOrderRequest= new PurchaseOrderRequest(this.getName(), temp.getShoeType(), false, this.fCurrentTick, 1);
					String wantedShoe= purchaseOrderRequest.getShoeRequested();
					
                    LOGGER.info("tick "+ this.fCurrentTick+ ": "+"Client "+this.getName()+" will try to buy this item from his purchase list: " +temp.getShoeType());
                    success=this.sendRequest(purchaseOrderRequest, Receipt -> {
                        if (Receipt!=null){ // if the item was successfully purchased
                            handlePurchasedItem(temp, wantedShoe);
                        }
                        else
                            LOGGER.info("tick "+ this.fCurrentTick+ ": purchase of: "+ wantedShoe+ " by "+this.getName()+" was not accepted");
                    });
                    if (success)
                        LOGGER.info("tick "+ this.fCurrentTick+ ": Client "+this.getName()+" sent a request for: " +wantedShoe+" and wait for its completion");
                    else
                        LOGGER.info("tick "+ this.fCurrentTick+ ": Client "+this.getName()+" sent a request for: " +wantedShoe+"  but there was no one to handle it");
                }
    }

	private void handlePurchasedItem(PurchaseSchedule purchaseSchedule, String wantedShoe) {
		this.fPurchaseSchedule.remove(purchaseSchedule); // we remove it from the client purchase list
		this.fWishList.remove(wantedShoe); // and also remove it from the wish list, if the shoe exist there
		LOGGER.info("tick "+ this.fCurrentTick+ ": Client "+this.getName()+" has bought successfully "+ wantedShoe);
		if (this.fPurchaseSchedule.isEmpty() && this.fWishList.isEmpty()){ // and if both of his shopping list are now empty
		    LOGGER.info("tick "+ this.fCurrentTick+ ": Client "+this.getName()+" has finished his shopping. now terminates");
		    this.terminate(); // then the client has finished his shopping
		    this.fLatchObjectForEnd.countDown();
		}
	}
     
    // Auxiliary method. 
    private ConcurrentLinkedQueue<PurchaseSchedule> findPurchasesAtCurrentTick(){
        ConcurrentLinkedQueue<PurchaseSchedule> ans=new ConcurrentLinkedQueue<PurchaseSchedule>();
        for (int i=0; i<this.fPurchaseSchedule.size(); ++i){ // search in the list, and find items that correspond to the current tick
            if (this.fPurchaseSchedule.get(i).getTick()==this.fCurrentTick)
                ans.add(this.fPurchaseSchedule.get(i));
        }
        return ans;
    }
     
    // Auxiliary method. subscribing to newDiscountBroadcast and defining how to handle it on callBack
    private void handleNewDiscountBroadcast(){
    	// subscribing the client to NewDiscountBroadcast (will try to buy items on wish list, if get's a discount on some item there)
        this.subscribeBroadcast(NewDiscountBroadcast.class, discountBroadcast -> { //this is how the client handle a NewDiscountBroadcast message in his queue
            String discountedShoe=discountBroadcast.getshoeType();
            
            if (this.fWishList.contains(discountedShoe) && !this.fRequestedFromWishList.contains(discountedShoe) && discountBroadcast.getAomunt()>0){ // if the message declared on a shoe at discount, it appears on this client wish list and the client didn't asked that before
                PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest(this.getName(), discountedShoe, true, this.fCurrentTick, 1); 
                // the client will try to buy this item immediately
                this.fRequestedFromWishList.add(discountedShoe);
                LOGGER.info("tick "+ this.fCurrentTick+ ": "+"Client "+this.getName()+" will try to buy this item from his wish list: " +discountedShoe);
                
                boolean success=this.sendRequest(purchaseOrderRequest, Receipt -> { // this is what the client expects to get
                    if (Receipt!=null){ // if the item was successfully purchased
                        handlePurchasedAtDiscountItem(discountedShoe);
                    }
                    else{
                        LOGGER.info("tick "+ this.fCurrentTick+ ": "+"purchase of: "+ discountedShoe+ " by "+this.getName()+" was not accepted");
                        this.fRequestedFromWishList.remove(discountedShoe); // if request fails, we will remove the shoe from requested list, because the client might have a chance to buy it in discount at future
                    }
                });
                if (success)
                    LOGGER.info("tick "+ this.fCurrentTick+ ": "+"Client "+this.getName()+" sent a request for: " +discountedShoe+"  and wait for its completion");
                else{
                    LOGGER.info("tick "+ this.fCurrentTick+ ": "+"Client "+this.getName()+" sent a request for: " +discountedShoe+"  but there was no one to handle it");
                    this.fRequestedFromWishList.remove(discountedShoe); // if request fails, we will remove the shoe from requested list, because the client might have a chance to buy it in discount at future
                }     
            }
        });
    }

	private void handlePurchasedAtDiscountItem(String discountedShoe) {
		this.fWishList.remove(discountedShoe); // we remove it from the client wish list
		LOGGER.info("tick "+ this.fCurrentTick+ ": Client "+this.getName()+" has bought successfully "+ discountedShoe);
		if (this.fPurchaseSchedule.isEmpty() && this.fWishList.isEmpty()){ // and if both of it's shopping list are now empty
		    LOGGER.info("tick "+ this.fCurrentTick+ ": "+"Client "+this.getName()+" has finished his shopping. now terminates");
		    this.terminate(); // then the client has finished his shopping
		    this.fLatchObjectForEnd.countDown();
		}
	}
     
 }