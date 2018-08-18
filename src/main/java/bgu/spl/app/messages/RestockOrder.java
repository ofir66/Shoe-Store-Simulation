package bgu.spl.app.messages;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * RestockOrder is message for ordering a shoe from a factory.
 *
 */
public class RestockOrder {
	
	/**
	 * String- type of shoe to order
	 */
	private String fShoeType;
	/**
	 * int- id of this message
	 */
	private int fId; 
	/**
	 * ConcurrentLinkedQueue<RestockRequest> - list of sellers which asked for restock for this shoe
	 */
	private ConcurrentLinkedQueue<RestockRequest> fRestockRequestsList;
	

	public RestockOrder(String shoeType, int id, ConcurrentLinkedQueue<RestockRequest> restockRequestList){
		this.fShoeType=shoeType;
		this.fId=id;
		this.fRestockRequestsList=restockRequestList;
	}
	

	public String getShoeType(){
		return this.fShoeType;
	}
	
	public int getId(){
		return this.fId;
	}
	
	public ConcurrentLinkedQueue<RestockRequest> getRestockRequestsList(){
		return this.fRestockRequestsList;
	}

}
