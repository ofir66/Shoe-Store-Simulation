package bgu.spl.app.services;
 
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import bgu.spl.app.messages.ManufacturingOrderRequest;
import bgu.spl.app.messages.NewDiscountBroadcast;
import bgu.spl.app.messages.RestockOrder;
import bgu.spl.app.messages.RestockRequest;
import bgu.spl.app.messages.TickBroadcast;
import bgu.spl.app.passiveObjects.DiscountSchedule;
import bgu.spl.app.passiveObjects.Store;
import bgu.spl.mics.MicroService;

/**
* 
* The micro-service that manages the store. Can add discount to shoes and
* handle {@link RestockRequest} when needed.
*
*/
public class ManagementService extends MicroService{
    
  /**
  * Logger- a logger for printing commands
  */
  private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  /**
  * List<DiscountSchedule>- a list of shoes the manager would like to put a discount on at certain tick
  */
  private final List<DiscountSchedule> fDiscountItemsList;
  /**
  * int- the current tick in the clock
  */
  private int fCurrentTick;
  /**
  * int- the duration of the program
  */
  private int fDuration;
  /**
  * Store- the {@link Store} that the manager manages
  */
  private final Store fStore=Store.getInstance();
  /**
  * ConcurrentHashMap of shoes (String), when each shoe is mapped to the amount of sellers requested it so far.
  */
  private final ConcurrentHashMap<String,Integer> fRequestedOrders;
  /**
  * ConcurrentHashMap of shoes (String), when each shoe is mapped to the amount of orders requested by the manager so far.
  */
  private final ConcurrentHashMap<String,Integer> fSentOrders;
  /**
  * ConcurrentHashMap of {@link RestockOrder RestockOrders}, each mapped to the number of sellers that need to be notified when an order
  * relates to the shoe in the (@link RestockOrder} is completed  
  */
  private final ConcurrentHashMap<RestockOrder, Integer> fRequestedSellers; 
  /**
  * int- id for the last (@link RestockOrder} that was created by the manager
  */
  private int fIdForLastRestockOrder;
  /**
  * CountDownLatch- an object for indicating when the {@link TimeService} starts sending ticks.
  * It happens when all services besides of the TimeService finish their initialization.
  */
  private CountDownLatch fLatchObject;
  /**
  * CountDownLatch- an object for indicating when the {@link bgu.spl.app.passiveObjects.ShoeStoreRunner ShoeStoreRunner} should terminate.
  * It happens when all services terminate.
  */
  private CountDownLatch fLatchObjectForEnd;


  public ManagementService(List<DiscountSchedule> discountItemsList, CountDownLatch latchObject, CountDownLatch latchObjectForEnd){
    super("manager");
    this.fDiscountItemsList=discountItemsList;
    this.fRequestedOrders= new ConcurrentHashMap<String,Integer>();
    this.fSentOrders= new ConcurrentHashMap<String,Integer>();
    this.fRequestedSellers= new ConcurrentHashMap<RestockOrder, Integer> ();
    this.fIdForLastRestockOrder=0;
    this.fLatchObject=latchObject;
    this.fLatchObjectForEnd=latchObjectForEnd;
  }
  
  /**
  * Subscribes the manager to tickBroadcast and RestockRequest messages.
  * As a callback to tickBroadcast, the manager will notify about discounts at the relevant ticks	 
  */
  @Override
  protected void initialize() {
    FileHandler handler;

    try {
      handler = new FileHandler("Log/ManagementService.txt");
      handler.setFormatter(new SimpleFormatter());
      LOGGER.addHandler(handler);
    } 
    catch (SecurityException e1) {
      LOGGER.severe(e1.getMessage());
    } 
    catch (IOException e1) {
      LOGGER.severe(e1.getMessage());
    }

    LOGGER.info(this.getName() +" started");

    this.subscribeBroadcast(TickBroadcast.class, tickBroadCast -> {
      this.fCurrentTick=tickBroadCast.getCurrentTick();
      this.fDuration=tickBroadCast.getDuration();
      if (this.fCurrentTick>this.fDuration){
        this.terminate();
        LOGGER.info(this.getName()+ " terminates");
        this.fLatchObjectForEnd.countDown();
      }    
      else{
        this.notifyDiscountAtTick();
      }
    });
    this.subscribeRestockRequest();

    this.fLatchObject.countDown();
  }


  // Notifies about a discount for all the shoes that are in the DiscountItemsList at the current tick
  private void notifyDiscountAtTick(){
    LinkedBlockingQueue<DiscountSchedule> itemsToNotyifyDiscountAtCurrentTick=findDiscountsAtCurrentTick();
    Iterator<DiscountSchedule> i= itemsToNotyifyDiscountAtCurrentTick.iterator();
    DiscountSchedule temp;

    while (i.hasNext()){
      temp=i.next();
      LOGGER.info("tick "+ this.fCurrentTick+ ": "+this.getName()+" will now publish a discount on "+temp.getAmount()+ " "+ temp.getShoeType());
      this.fStore.addDiscount(temp.getShoeType(), temp.getAmount());
      NewDiscountBroadcast newDiscountBroadcast= new NewDiscountBroadcast(this.getName(), temp.getShoeType(), temp.getAmount()); 
      this.sendBroadcast(newDiscountBroadcast);
    }
  }

  // Returns items in the DiscountItemsList at the current tick
  private LinkedBlockingQueue<DiscountSchedule> findDiscountsAtCurrentTick(){
    LinkedBlockingQueue<DiscountSchedule> ans=new LinkedBlockingQueue<DiscountSchedule>();

    for (int i=0; i<this.fDiscountItemsList.size(); ++i){
      if (this.fDiscountItemsList.get(i).getTick()==this.fCurrentTick)
      ans.add(this.fDiscountItemsList.get(i));
    }

    return ans;
  }

  private void subscribeRestockRequest(){
    this.subscribeRequest(RestockRequest.class, restockRequest ->{
      String requestedShoe=restockRequest.getShoeNeeded();
      int amountNeeded;

      if (this.fStore.take(requestedShoe, false).name().compareTo("REGULAR_PRICE")==0){ // if the requested shoe is in the store
        this.complete(restockRequest, true); // manager can complete this request immediately 
      }
      else{
        this.fRequestedOrders.put(requestedShoe, this.fRequestedOrders.getOrDefault(requestedShoe, 0)+restockRequest.getAmountNeeded());
        amountNeeded= this.fRequestedOrders.getOrDefault(requestedShoe, 0)-this.fSentOrders.getOrDefault(requestedShoe, 0); // find the number of shoes that the manager needs to order
        if (amountNeeded>0){
          orderShoes(restockRequest, requestedShoe);
        }
        else { // ordered enough units of this shoe for someone else
          addRequestedSeller(restockRequest.getShoeNeeded(), restockRequest);
        }
      }    
    });
  }

  private void orderShoes(RestockRequest restockRequest, String requestedShoe){
    ConcurrentLinkedQueue<RestockRequest> restockRequestsList;
    RestockOrder restockOrder;
    ManufacturingOrderRequest manufacturingOrderRequest;
    boolean success;

    this.fIdForLastRestockOrder++;
    restockRequestsList= new ConcurrentLinkedQueue<RestockRequest>();
    restockRequestsList.add(restockRequest);
    restockOrder= new RestockOrder(requestedShoe, this.fIdForLastRestockOrder, restockRequestsList);
    this.fRequestedSellers.put(restockOrder, restockRequest.getAmountNeeded()); // create a new order for this seller
    manufacturingOrderRequest= new ManufacturingOrderRequest("manager",requestedShoe,(this.fCurrentTick%5)+1, this.fCurrentTick);
    LOGGER.info("tick "+ this.fCurrentTick+ ": manager will send a ManufacturingOrderRequest for: " + ((this.fCurrentTick%5)+1) + " items of " + restockRequest.getShoeNeeded());
    this.fSentOrders.put(requestedShoe, this.fSentOrders.getOrDefault(requestedShoe, 0)+manufacturingOrderRequest.getAmountNeeded()); // update the sent order list
    success=this.sendRequest(manufacturingOrderRequest, receipt -> {
      if (receipt!=null){ // if ManufacturingOrderRequest succeed
        this.fStore.add(requestedShoe, receipt.getAmountSold()-this.fRequestedSellers.getOrDefault(restockOrder, 0)); // add the amount needed to store, minus all the sellers that relates to this order                    
        this.fRequestedSellers.remove(restockOrder);
        this.fSentOrders.put(requestedShoe, this.fSentOrders.get(requestedShoe)-receipt.getAmountSold()); // update the sent orders list (because the order was completed)
        if (this.fRequestedOrders.getOrDefault(requestedShoe, 0)-receipt.getAmountSold()<0)
          this.fRequestedOrders.put(requestedShoe, 0);
        else
          this.fRequestedOrders.put(requestedShoe, this.fRequestedOrders.getOrDefault(requestedShoe, 0)-receipt.getAmountSold());
        this.fStore.file(receipt);     
        LOGGER.info("tick "+ this.fCurrentTick+ ": "+receipt.getAmountSold()+ " items of: "+ restockRequest.getShoeNeeded()+ " were added to the store");
        this.updateSellers(restockOrder.getRestockRequestsList(), true);
      }
      else{
        this.restockFails(requestedShoe, restockOrder, manufacturingOrderRequest);
      }	                 
    });
    if (!success){
      LOGGER.info("tick "+ this.fCurrentTick+ ": there is no one to handle "+this.getName()+" request of type: "+ manufacturingOrderRequest.getClass().getSimpleName());
      this.restockFails(requestedShoe, restockOrder, manufacturingOrderRequest);
    }
  }

  // Updates fRequestedSellers for relevant restockRequest with one more seller
  private void addRequestedSeller(String wantedShoe, RestockRequest restockRequest){
    for (RestockOrder mes: this.fRequestedSellers.keySet()){
      if (wantedShoe.compareTo(mes.getShoeType())==0 && this.fIdForLastRestockOrder==mes.getId()){
        mes.getRestockRequestsList().add(restockRequest);
        this.fRequestedSellers.put(mes, this.fRequestedSellers.getOrDefault(mes, 0)+1);	
      }
    }
  }

  // Completes all restock requests with "false"
  private void restockFails(String requestedShoe, RestockOrder restockOrder, ManufacturingOrderRequest manufacturingOrderRequest){
    this.fSentOrders.put(requestedShoe, this.fSentOrders.get(requestedShoe)-manufacturingOrderRequest.getAmountNeeded());
    if (this.fRequestedOrders.getOrDefault(requestedShoe, 0)-manufacturingOrderRequest.getAmountNeeded()<0) 
      this.fRequestedOrders.put(requestedShoe, 0);
    else
      this.fRequestedOrders.put(requestedShoe, this.fRequestedOrders.getOrDefault(requestedShoe, 0)-manufacturingOrderRequest.getAmountNeeded());
    this.fRequestedSellers.remove(restockOrder);
    this.updateSellers(restockOrder.getRestockRequestsList(), false);
  }

  // Updates relevant sellers with the result of some ManufacturingOrderRequest
  private void updateSellers(ConcurrentLinkedQueue<RestockRequest> q, boolean result){
    RestockRequest req;

    if (q==null || q.size()==0)
      LOGGER.warning("if manager sent a manufacturing order, someone must has requested a restock");
    while (!q.isEmpty()){
      req=q.poll();
      this.complete(req, result);
    }
  }
     
}