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
 * This micro-service describes a shoe factory that manufacture shoes for the store.
Handles the {@link ManufacturingOrderRequest}.
The factory manufacture a single shoe in one tick.
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
     * ConcurrentLinkedQueue of {@link ManufacturingOrderRequest ManufacturingOrderRequests}, which is a list 
     * of manufacturing order requests that this factory needs to handle.
     */
    private final ConcurrentLinkedQueue<ManufacturingOrderRequest> fHandleList;
    /**
     * ConcurrentHashMap of shoes (String), which represents a ManufacturingOrderRequest, each mapped
     * to the number of instances of it created so far by the factory.
     * will be mapped to null when the ManufacturingOrderRequest relates to it was handled.
     */
    private final ConcurrentHashMap<String,Integer> fCompletedList;
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
     
     
    public ShoeFactoryService(String name, CountDownLatch latchObject, CountDownLatch latchObjectForEnd){
        super(name);
        this.fHandleList= new ConcurrentLinkedQueue<ManufacturingOrderRequest>();
        this.fCompletedList=new ConcurrentHashMap<String,Integer>();
        this.fLatchObject=latchObject;
        this.fLatchObjectForEnd=latchObjectForEnd;
    }
     
    /**
     * Subscribes to tickBroadcast and ManufacturingOrderRequest messages.
     * As a callback to tickBroadcast, the factory will manufacture shoes (if its handle list isn't empty )
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
		
        this.subscribeBroadcast(TickBroadcast.class, tickBroadCast -> {
        this.fCurrentTick=tickBroadCast.getCurrentTick();
        this.fDuration=tickBroadCast.getDuration();
        if (this.fCurrentTick>this.fDuration){
            this.terminate();
            LOGGER.info(this.getName()+ " terminates");
            this.fLatchObjectForEnd.countDown();
        }    
        else{
            this.createShoe();
        }
        });
        this.subscribeRequest(ManufacturingOrderRequest.class, req -> {
            this.fHandleList.add(req); // it will be added to the handle list of the factory
            LOGGER.info("tick "+ this.fCurrentTick+ ": "+this.getName()+ " has received manufacturing order request for "+ req.getAmountNeeded()+ " instances of "+ req.getShoeType());
        });
		
        this.fLatchObject.countDown();
    }
     
    // Creates a shoe at the current tick
    private void createShoe(){
        ManufacturingOrderRequest request;
		String shoeType;
		
		if (this.fHandleList.size()>0){
            request= this.fHandleList.peek();
            shoeType= request.getShoeType();
            if (this.fCompletedList.get(shoeType)==null){ // if the factory hasn't started yet to take care of this request
                this.fCompletedList.put(shoeType, 1);
                LOGGER.info("tick "+ this.fCurrentTick+ ": "+getName() + " has created one "+ request.getShoeType());
            }
            else if (this.fCompletedList.get(shoeType)!=null && request.getAmountNeeded()-this.fCompletedList.get(shoeType)==0){ // if manufactured all the shoe units needed in this request
                handleCompletedRequest(request, shoeType);    
            }
            else{ // // if the factory has not finished yet to manufacture all the shoe units needed in this request
                this.fCompletedList.replace(shoeType, this.fCompletedList.get(shoeType)+1); // create one more instance of this shoe
                LOGGER.info("tick "+ this.fCurrentTick+ ": "+getName() + " has created one "+ request.getShoeType());
            }
        }
    }

	private void handleCompletedRequest(ManufacturingOrderRequest request, String shoeType) {
		ManufacturingOrderRequest newRequest;
		String newShoeType;
		Receipt receipt;
		
		this.fHandleList.poll();
		receipt= new Receipt(this.getName(), "store", shoeType, false, this.fCurrentTick, request.getRequestedTick(), request.getAmountNeeded());
		this.complete(request, receipt);
		this.fCompletedList.remove(shoeType);
		LOGGER.info("tick "+ this.fCurrentTick+ ": "+this.getName()+ " has completed manufacturing order request for "+ request.getAmountNeeded()+ " instances of "+ request.getShoeType());
		if (this.fHandleList.size()>0){ // when the handle in the request is done, handle a new order (if the handle list is empty, no need to do anything)
		    newRequest= this.fHandleList.peek();
		    newShoeType= newRequest.getShoeType();
		    this.fCompletedList.put(newShoeType, 1);
		    LOGGER.info("tick "+ this.fCurrentTick+ ": "+getName() + " has created one "+ newShoeType);
		}
	}
         
     
}