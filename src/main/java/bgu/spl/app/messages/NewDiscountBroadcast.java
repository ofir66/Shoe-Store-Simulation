package bgu.spl.app.messages;

import bgu.spl.mics.Broadcast;

/**
 * a broadcast message that is sent when the manager of the store
decides to have a sale on a specific shoe
 */
public class NewDiscountBroadcast implements Broadcast{
	
	/**
	 * String- name of the sender
	 */
	private String fSenderId;
	/**
	 * String- type of shoe for discount 
	 */
	private String fShoeType;
	/**
	 * int- the amount of fShoeType for discount
	 */
	private int fAomunt;
	

	public NewDiscountBroadcast(String senderId, String shoeType, int amount){
		this.fSenderId=senderId;
		this.fShoeType=shoeType;
		this.fAomunt=amount;
	}
	
	
    public String getSenderId() {
        return fSenderId;
    }
    
    public String getshoeType(){
    	return this.fShoeType;
    }
    
    public int getAomunt(){
    	return this.fAomunt;
    }
    
}
	

