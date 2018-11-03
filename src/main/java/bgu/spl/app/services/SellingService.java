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
* A service which handles {@link PurchaseOrderRequest purchase order requests}. 
* If the amount asked in the PurchaseOrderRequest isn't in the stock, the seller will send a {@link RestockRequest}
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
  * int- the id of this seller
  */
  private int fId;
  /**
  * Store- the {@link Store} that the seller work at
  */
  private final Store fStore=Store.getInstance();
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


  public SellingService(String name, int id, CountDownLatch latchObject, CountDownLatch latchObjectForEnd){
    super(name); 
    this.fId=id;
    this.fLatchObject=latchObject;
    this.fLatchObjectForEnd=latchObjectForEnd;
  }

  /**
  * Subscribes the seller to TickBroadcast and PurchaseOrderRequests messages.
  */

  @Override
  protected void initialize() {
    try {
      FileHandler handler = new FileHandler("Log/SellingService"+fId+".txt");
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
    });
    this.subscribePurchaseOrderRequest();

    this.fLatchObject.countDown();
  }

  private void subscribePurchaseOrderRequest(){
    this.subscribeRequest(PurchaseOrderRequest.class, req -> {
      String wantedShoe;
      boolean discount;
      String takeResult;

      LOGGER.info("tick "+ this.fCurrentTick+ ": "+this.getName()+ " will try to take care of "+ req.getSenderName()+ " request of "+ req.getShoeRequested());
      wantedShoe=req.getShoeRequested();
      discount=req.getDiscount(); // check if the customer wanted to buy the shoe only at discount
      takeResult=this.fStore.take(wantedShoe, discount).name();

      if (takeResult.compareTo("REGULAR_PRICE")==0){
        handleRegularPriceRequest(req);
      }
      if (takeResult.compareTo("DISCOUNTED_PRICE")==0){
        handleDiscountPriceRequest(req);
      }
      if (takeResult.compareTo("NOT_IN_STOCK")==0){
        handleNotInStockRequest(req, wantedShoe);
      }
      if (takeResult.compareTo("NOT_ON_DISCOUNT")==0){
        complete(req,null);
      }
    }); 
  }

  private void handleNotInStockRequest(PurchaseOrderRequest req, String wantedShoe) {
    RestockRequest restockRequest;
    boolean success;

    LOGGER.info("tick "+ this.fCurrentTick+ ": "+"No shoes from kind: "+wantedShoe+" left in stock for "+ req.getSenderName()+ ". calling for restock. you will receive a receipt only if the restock request succeed");
    restockRequest= new RestockRequest(this.fId, wantedShoe, req.getAmountWanted(), req);
    success=this.sendRequest(restockRequest, v -> { // v is a boolean which indicates the restockRequest answer
      if (!v) // if the manager returned "false" to the restock request
        complete(req,null); // return the result "null" to the customer
      else{ // if the restock succeed
        handleRegularPriceRequest(req);
      }
    });
    if (!success){
      LOGGER.info("tick "+ this.fCurrentTick+ ": there is no one to handle "+this.getName()+" request of type: "+restockRequest.getClass().getSimpleName());
      complete(req,null);
    }
  }

  private void handleDiscountPriceRequest(PurchaseOrderRequest req) {
    Receipt receipt= new Receipt(this.getName(), req.getSenderName(), req.getShoeRequested(), true, this.fCurrentTick, req.getRequestTick(),req.getAmountWanted());

    LOGGER.info("tick "+ this.fCurrentTick+ ": Client "+req.getSenderName()+ " will buy one "+ req.getShoeRequested()+ " with discount");
    this.fStore.file(receipt);
    complete(req,receipt);
  }

  private void handleRegularPriceRequest(PurchaseOrderRequest req) {
    Receipt receipt= new Receipt(this.getName(), req.getSenderName(), req.getShoeRequested(), false, this.fCurrentTick, req.getRequestTick(),req.getAmountWanted());

    this.fStore.file(receipt);
    complete(req,receipt);
  }
     
     
}