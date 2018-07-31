package bgu.spl.app.messages;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * RestockOrder is a Message which contains all the sellers that need to be updated when a ManufacturingOrderRequest 
 * of {@code fShoeType} is completed.
 * <p>
 * if a seller send a restock request for a shoe, and the manager sees that he didn't ordered enough shoes of this type, he will create new 
 * RestockOrder with new id, and a new queue ({@code fRestockRequestsList}) which the seller will be inserted to, so
 * he will be notified when the ManufacturingOrderRequest that relates to this shoe is completed.
 * <p>
 *  if a seller send a restock request for  {@code fShoeType}, and the manager sees
 * that enough shoes of this type was ordered- this seller will be added to the queue {@code fRestockRequestsList}
 * (in this case, the manager will find the RestockOrder corresponding to the RestockRequest asked via the current id 
 * at his class and the type of shoe wanted by the seller).  
 *
 */
public class RestockOrder {
	
	/**
	 * String- type of shoe to order
	 */
	private String fShoeType;
	/**
	 * int- id of this Message (the id is meant to differentiate between two or more different ManufacturingOrderRequest of a same shoe type)
	 */
	private int fId; 
	/**
	 * ConcurrentLinkedQueue<RestockRequest> - a list of sellers need to be notified when a ManufacturingOrderRequest relates to this shoe is completed
	 */
	private ConcurrentLinkedQueue<RestockRequest> fRestockRequestsList;
	
	/**
	 * 
	 * @param shoeType type of shoe to order
	 * @param id id of this Message
	 * @param restockRequestList a list of sellers need to be notified when a ManufacturingOrderRequest relates to this shoe is completed
	 */
	public RestockOrder(String shoeType, int id, ConcurrentLinkedQueue<RestockRequest> restockRequestList){
		this.fShoeType=shoeType;
		this.fId=id;
		this.fRestockRequestsList=restockRequestList;
	}
	
	/**
	 * 
	 * @return type of shoe to order
	 */
	public String getShoeType(){
		return this.fShoeType;
	}
	
	/**
	 * 
	 * @return id of this Message
	 */
	public int getId(){
		return this.fId;
	}
	
	/**
	 * 
	 * @return a list of sellers need to be notified when a ManufacturingOrderRequest relates to this shoe is completed
	 */
	public ConcurrentLinkedQueue<RestockRequest> getRestockRequestsList(){
		return this.fRestockRequestsList;
	}

}
