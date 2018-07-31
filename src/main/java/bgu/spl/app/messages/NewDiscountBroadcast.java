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
	 * String- type of shoe to put discount on
	 */
	private String fShoeType;
	/**
	 * int- the amount of shoe type to put discount on
	 */
	private int fAomunt;
	
    /**
     * @param senderId name of the sender.
     * @param shoeType the shoe to put discount on
     * @param  amount the amount of shoeType to put discount on
     */
	public NewDiscountBroadcast(String senderId, String shoeType, int amount){
		this.fSenderId=senderId;
		this.fShoeType=shoeType;
		this.fAomunt=amount;
	}
	
	   /**
     * @return the sender of this message
     */
    public String getSenderId() {
        return fSenderId;
    }
    
    /**
     * @return the shoe that this discount message relates to
     */
    public String getshoeType(){
    	return this.fShoeType;
    }
    
    /**
     * @return the amount of shoes in this message the sender wants to put discount on
     */
    public int getAomunt(){
    	return this.fAomunt;
    }
    
}
	

