package bgu.spl.app.services;
 
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import bgu.spl.app.messages.ManufacturingOrderRequest;
import bgu.spl.app.messages.TickBroadcast;
import bgu.spl.app.passiveObjects.Receipt;
import bgu.spl.mics.MicroService;

/**
 * 
 * This micro-service describes a shoe factory that manufacture shoes for the store. This micro-service
handles the {@link ManufacturingOrderRequest} and it takes it exactly 1 tick to manufacture a single shoe
 *
 */
public class ShoeFactoryService extends MicroService{
    
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
     * ConcurrentLinkedQueue of {@link ManufacturingOrderRequest ManufacturingOrderRequests}, which is the list 
     * of ManufacturingOrderRequest that this factory needs to handle.
     */
    private final ConcurrentLinkedQueue<ManufacturingOrderRequest> fHandleList;
    /**
     * ConcurrentHashMap of shoes (String), which represents a ManufacturingOrderRequest, each mapped
     * to the number of instances of it created so far by the factory.
     * will be mapped to null when the ManufacturingOrderRequest relates to it ends.
     */
    private final ConcurrentHashMap<String,Integer> fCompletedList;
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
     * @param name name of the factory
     * @param latchObject an object for indicating when the {@link TimeService} starts sending ticks.
     * @param latchObjectForEnd an object for indicating when the {@link bgu.spl.app.passiveObjects.ShoeStoreRunner ShoeStoreRunner} should terminate.
     */
     
    public ShoeFactoryService(String name, CountDownLatch latchObject, CountDownLatch latchObjectForEnd){
        super(name);
        this.fHandleList= new ConcurrentLinkedQueue<ManufacturingOrderRequest>();
        this.fCompletedList=new ConcurrentHashMap<String,Integer>();
        this.fLatchObject=latchObject;
        this.fLatchObjectForEnd=latchObjectForEnd;
    }
     
    /**
     * via his initialize, the factory will will subscribe to {@link ManufacturingOrderRequest} and create shoes if
     * He has {@link ManufacturingOrderRequest ManufacturingOrderRequests} in his handle list
     */
     
    protected void initialize(){
    	FileHandler handler;
		
		try {
			handler = new FileHandler("Log/ShoeFactoryService"+getName()+".txt");
			handler.setFormatter(new SimpleFormatter());
			LOGGER.addHandler(handler);
		} catch (SecurityException e1) {
			LOGGER.severe(e1.getMessage());
		} catch (IOException e1) {
			LOGGER.severe(e1.getMessage());
		}
		LOGGER.info(this.getName() +" started");
        this.subscribeBroadcast(TickBroadcast.class, tickBroadCast -> { // this is how the shoe factory handles TickBroadcast message
        this.fCurrentTick=tickBroadCast.getCurrentTick();
        this.fDuration=tickBroadCast.getDuration();
        if (this.fCurrentTick>this.fDuration){
            this.terminate();
            LOGGER.info(this.getName()+ " terminates");
            this.fLatchObjectForEnd.countDown();
        }    
        else{
            this.createShoe(); // if tick<duration, factory will create a new shoe (if he got tasks to do)
        }
        });
        // subscribing ManufacturingOrderRequest, and defining how to handle it
        this.subscribeRequest(ManufacturingOrderRequest.class, req -> {
            this.fHandleList.add(req); // it will be added to the handle list of the factory
            LOGGER.info("tick "+ this.fCurrentTick+ ": "+this.getName()+ " has received manufacturing order request for "+ req.getAmountNeeded()+ " instances of "+ req.getShoeType());
        });
        this.fLatchObject.countDown();
    }
     
    //Auxiliary method. create shoe at current tick
    private void createShoe(){
        ManufacturingOrderRequest request;
		String shoeType;
		
		if (this.fHandleList.size()>0){ // (if the handle list is empty, we won't create anything)
            request= this.fHandleList.peek(); // we will first look at our requested shoe to create
            shoeType= request.getShoeType();
            if (this.fCompletedList.get(shoeType)==null){ // if we just received the request 
                this.fCompletedList.put(shoeType, 1); // then we will define this shoe in our list. that means that we received the request
                LOGGER.info("tick "+ this.fCurrentTick+ ": "+getName() + " has created one "+ request.getShoeType());
            }
            else if (this.fCompletedList.get(shoeType)!=null && request.getAmountNeeded()-this.fCompletedList.get(shoeType)==0){ // if we created the amount of all this shoe type needed
                handleCompletedRequest(request, shoeType);    
            }
            else{ // if we handled this shoe before, but haven't finished the request that relates to it
                this.fCompletedList.replace(shoeType, this.fCompletedList.get(shoeType)+1); // we will create one more instance of that shoe
                LOGGER.info("tick "+ this.fCurrentTick+ ": "+getName() + " has created one "+ request.getShoeType());
            }
        }
    }

	private void handleCompletedRequest(ManufacturingOrderRequest request, String shoeType) {
		ManufacturingOrderRequest newRequest;
		String newShoeType;
		Receipt receipt;
		
		this.fHandleList.poll(); // we will poll out the request, because we are done with it
		receipt= new Receipt(this.getName(), "store", shoeType, false, this.fCurrentTick, request.getRequestedTick(), request.getAmountNeeded());
		this.fCompletedList.remove(shoeType); // and also initializing our shoe completed instances- because the request relates to it was completed, and we now need it initialized for later manufactoring requests for it  
		LOGGER.info("tick "+ this.fCurrentTick+ ": "+this.getName()+ " has completed manufacturing order request for "+ request.getAmountNeeded()+ " instances of "+ request.getShoeType());
		this.complete(request, receipt);
		if (this.fHandleList.size()>0){ // after completing, we will want to handle a new order (if the handle list is empty, we won't create anything)
		    newRequest= this.fHandleList.peek(); // we will first look at our requested shoe to create
		    newShoeType= newRequest.getShoeType();
		    this.fCompletedList.put(newShoeType, 1); // we will define this shoe in our list. that means that we received the request
		    LOGGER.info("tick "+ this.fCurrentTick+ ": "+getName() + " has created one "+ newShoeType);
		}
	}
         
     
}