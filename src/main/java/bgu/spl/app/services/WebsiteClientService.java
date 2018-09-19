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
 * This micro-service describes one client connected to a website and tries to purchase shoes.
 *
 */
public class WebsiteClientService extends MicroService{
	
  /**
  * Logger- a logger for printing commands
  */
  private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  /**
  * list of purchases that the client needs to do
  */
  private final List<PurchaseSchedule> fPurchaseSchedule; 
  /**
  * list of shoes that the client will buy only at discount. 
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
  * It happens when all services besides of the TimeService finish their initialization.
  */
  private CountDownLatch fLatchObject;
  /**
  * CountDownLatch- an object for indicating when the {@link bgu.spl.app.passiveObjects.ShoeStoreRunner ShoeStoreRunner} should terminate.
  * It happens when all services terminate.
  */
  private CountDownLatch fLatchObjectForEnd;


  public WebsiteClientService(String name, List<PurchaseSchedule> purchaseSchedule, Set<String> wishList, CountDownLatch latchObject, CountDownLatch latchObjectForEnd){
    super(name);
    this.fPurchaseSchedule=purchaseSchedule;
    this.fWishList=wishList;
    this.fLatchObject=latchObject;
    this.fLatchObjectForEnd=latchObjectForEnd;
    this.fRequestedFromWishList= new ConcurrentLinkedQueue<String>();
  }

  /**
  * Subscribes the client to tickBroadcast and newDiscountBroadcast messages.
  * As a callback to tickBroadcast, the client will try to buy shoes from his purchase list if the list contains shoes to buy in the relevant tick
  */
  protected void initialize(){
    FileHandler handler;

    try {
      handler = new FileHandler("Log/WebsiteClientService"+getName()+".txt");
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
        this.buyPurchaseScheduleItems();
      }   
    });
    this.subscribeNewDiscountBroadcast();

    this.fLatchObject.countDown();
  }   

  // Tries to buy items from the PurchaseSchedule at the current tick
  private void buyPurchaseScheduleItems(){
    ConcurrentLinkedQueue<PurchaseSchedule> itemsToPurchaseAtCurrentTick= findPurchasesAtCurrentTick(); // find all the items to purchase in this current tick
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
    this.fPurchaseSchedule.remove(purchaseSchedule);
    this.fWishList.remove(wantedShoe);
    LOGGER.info("tick "+ this.fCurrentTick+ ": Client "+this.getName()+" has bought successfully "+ wantedShoe);
    if (this.fPurchaseSchedule.isEmpty() && this.fWishList.isEmpty()){
      LOGGER.info("tick "+ this.fCurrentTick+ ": Client "+this.getName()+" has finished his shopping. now terminates");
      this.terminate();
      this.fLatchObjectForEnd.countDown();
    }
  }

  private ConcurrentLinkedQueue<PurchaseSchedule> findPurchasesAtCurrentTick(){
    ConcurrentLinkedQueue<PurchaseSchedule> ans=new ConcurrentLinkedQueue<PurchaseSchedule>();
    for (int i=0; i<this.fPurchaseSchedule.size(); ++i){
      if (this.fPurchaseSchedule.get(i).getTick()==this.fCurrentTick)
        ans.add(this.fPurchaseSchedule.get(i));
    }
    return ans;
  }

  // subscribes the client to NewDiscountBroadcast (if a discount is announced to a shoe in his wish list - he will try to buy it) 
  private void subscribeNewDiscountBroadcast(){
    this.subscribeBroadcast(NewDiscountBroadcast.class, discountBroadcast -> {
      String discountedShoe=discountBroadcast.getshoeType();

      if (this.fWishList.contains(discountedShoe) && !this.fRequestedFromWishList.contains(discountedShoe) && discountBroadcast.getAomunt()>0){
        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest(this.getName(), discountedShoe, true, this.fCurrentTick, 1); 
        this.fRequestedFromWishList.add(discountedShoe);
        LOGGER.info("tick "+ this.fCurrentTick+ ": "+"Client "+this.getName()+" will try to buy this item from his wish list: " +discountedShoe);

        boolean success=this.sendRequest(purchaseOrderRequest, Receipt -> { // Receipt is what the client expects to get for his purchaseOrderRequest
          if (Receipt!=null){ // if the item was successfully purchased
            handlePurchasedAtDiscountItem(discountedShoe);
          }
          else{
            LOGGER.info("tick "+ this.fCurrentTick+ ": "+"purchase of: "+ discountedShoe+ " by "+this.getName()+" was not accepted");
            this.fRequestedFromWishList.remove(discountedShoe); // if request fails, remove the shoe from requested list because the client may have a chance to buy it at discount in the future
          }
        });
        if (success)
          LOGGER.info("tick "+ this.fCurrentTick+ ": "+"Client "+this.getName()+" sent a request for: " +discountedShoe+"  and wait for its completion");
        else{
          LOGGER.info("tick "+ this.fCurrentTick+ ": "+"Client "+this.getName()+" sent a request for: " +discountedShoe+"  but there was no one to handle it");
          this.fRequestedFromWishList.remove(discountedShoe);
        }     
      }
    });
  }

  private void handlePurchasedAtDiscountItem(String discountedShoe) {
    this.fWishList.remove(discountedShoe);
    LOGGER.info("tick "+ this.fCurrentTick+ ": Client "+this.getName()+" has bought successfully "+ discountedShoe);
    if (this.fPurchaseSchedule.isEmpty() && this.fWishList.isEmpty()){
      LOGGER.info("tick "+ this.fCurrentTick+ ": "+"Client "+this.getName()+" has finished his shopping. now terminates");
    this.terminate();
    this.fLatchObjectForEnd.countDown();
    }
  }
     
 }