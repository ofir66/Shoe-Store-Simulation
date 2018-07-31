package bgu.spl.app.services;
 
 
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import bgu.spl.app.messages.PurchaseOrderRequest;
import bgu.spl.app.messages.RestockRequest;
import bgu.spl.app.messages.TickBroadcast;
import bgu.spl.app.passiveObjects.Receipt;
import bgu.spl.app.passiveObjects.Store;
import bgu.spl.mics.MicroService;

/**
 * 
 * A service which handles {@link PurchaseOrderRequest purchase order requests}. if the amount asked
 * in the request isn't in the stock, the seller will send a {@link RestockRequest}
 *
 */

public class SellingService extends MicroService{
 
    
	/**
	 * Logger- a logger for printing commands
	 */
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	   /**
     * int- the current tick in the clock
     */
    private int fCurrentTick;
    /**
     * int- the duration of the program
     */
    private int fDuration;
    /**
     * int- the id of this seller (each seller has different id)
     */
    private int fId;
    /**
     * Store- the {@link Store} that seller sells items or send restock requests
     */
    private final Store fStore=Store.getInstance();
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
     * @param id the id of the service
     * @param latchObject an object for indicating when the {@link TimeService} starts sending ticks.
     * @param latchObjectForEnd an object for indicating when the {@link bgu.spl.app.passiveObjects.ShoeStoreRunner ShoeStoreRunner} should terminate.
     */
     
    public SellingService(String name, int id, CountDownLatch latchObject, CountDownLatch latchObjectForEnd){
        super(name); 
        this.fId=id;
        this.fLatchObject=latchObject;
        this.fLatchObjectForEnd=latchObjectForEnd;
    }
     
   /**
    * via his initialize, the seller will subscribe to TickBroadcast and PurchaseOrderRequests messages. 
    * He will also send restock requests if needed 
    */
     
    @Override
    protected void initialize() {
    	try {
			FileHandler handler = new FileHandler("Log/SellingService"+fId+".txt");
			handler.setFormatter(new SimpleFormatter());
			LOGGER.addHandler(handler);
		} catch (SecurityException e1) {
			LOGGER.severe(e1.getMessage());
		} catch (IOException e1) {
			LOGGER.severe(e1.getMessage());
		}
		LOGGER.info(this.getName() +" started");
        //the seller subscribe to TickBroadcast, for knowing the current tick
        this.subscribeBroadcast(TickBroadcast.class, tickBroadCast -> { // that's how the seller handles TickBroadcast message
            this.fCurrentTick=tickBroadCast.getCurrentTick();
            this.fDuration=tickBroadCast.getDuration();
            if (this.fCurrentTick>this.fDuration){
                this.terminate();
                LOGGER.info(this.getName()+ " terminates");
                this.fLatchObjectForEnd.countDown();
            }    
        });
        // now the seller subscribe to PurchaseOrderRequest, for handling customers purchases
        this.handlePurchaseOrderRequest();
        // now we will subscribe to "NewDiscountBroadcast", for knowing if the manager gave a discount on some shoe
        this.fLatchObject.countDown();
    }
     
    //Auxiliary method. will subscribe the service to PurchaseOrderRequest messages, and define how to handle them on callBack
    private void handlePurchaseOrderRequest(){
        this.subscribeRequest(PurchaseOrderRequest.class, req -> { // now we will define how selling-service handle this request
            LOGGER.info("tick "+ this.fCurrentTick+ ": "+this.getName()+ " will try to take care of "+ req.getSenderName()+ " request of "+ req.getShoeRequested());
            String wantedShoe=req.getShoeRequested(); // we will save the wanted shoe
            boolean discount=req.getDiscount(); // check if the customer wanted to buy it only at discount
            String takeResult=this.fStore.take(wantedShoe, discount).name(); // and try to find it in the store
            if (takeResult.compareTo("REGULAR_PRICE")==0){ // if we found this shoe, and the customer didn't care about discount
                Receipt receipt= new Receipt(this.getName(), req.getSenderName(), req.getShoeRequested(), false, this.fCurrentTick, req.getRequestTick(),req.getAmountWanted());
                this.fStore.file(receipt);
                complete(req,receipt);
            }
            if (takeResult.compareTo("DISCOUNTED_PRICE")==0){ // if we found this shoe on discounted price (regardless to the customer wish for discount)
                Receipt receipt= new Receipt(this.getName(), req.getSenderName(), req.getShoeRequested(), true, this.fCurrentTick, req.getRequestTick(),req.getAmountWanted());
                // note: if a client bought a shoe with discount, we will mention it by two prints:
                // that he bought it with discount, and successfully bought it (in complete method)
                LOGGER.info("tick "+ this.fCurrentTick+ ": Client "+req.getSenderName()+ " will buy one "+ req.getShoeRequested()+ " with discount");
                this.fStore.file(receipt);
                complete(req,receipt);
            }
            if (takeResult.compareTo("NOT_IN_STOCK")==0){ // if we didn't find this shoe, and the customer didn't care about discount, we will send a restock request to the manager- and then complete the request
                LOGGER.info("tick "+ this.fCurrentTick+ ": "+"No shoes from kind: "+wantedShoe+" left in stock for "+ req.getSenderName()+ ". calling for restock. you will receive a receipt only if the restock request succeed");
                RestockRequest restockRequest= new RestockRequest(this.fId, wantedShoe, req.getAmountWanted(), req);
                boolean success=this.sendRequest(restockRequest, v -> { // we will now define that is the wanted result of our restock request
                        if (!v) // if the manager returned "false" to our restock request
                            complete(req,null); // we will return the result "null" tp the customer
                        else{ // if the restock succeed
                            Receipt receipt= new Receipt(this.getName(), req.getSenderName(), req.getShoeRequested(), false, this.fCurrentTick, req.getRequestTick(),req.getAmountWanted());
                            this.fStore.file(receipt);
                            complete(req,receipt);
                        }
                });
                if (!success){
                	LOGGER.info("tick "+ this.fCurrentTick+ ": there is no one to handle "+this.getName()+" request of type: "+restockRequest.getClass().getSimpleName());
                    complete(req,null);
                }
            }
            if (takeResult.compareTo("NOT_ON_DISCOUNT")==0){
                complete(req,null);
            }
        }); 
    }
     
     
}