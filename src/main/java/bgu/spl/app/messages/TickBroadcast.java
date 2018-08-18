package bgu.spl.app.messages;

import bgu.spl.mics.Broadcast;

/**
 * a broadcast message that is sent at every clock tick
 *
 */
		
public class TickBroadcast implements Broadcast{
	
	/**
	 * String- the sender name
	 */
	private String fSenderId;
	/**
	 * int- the current tick at the clock
	 */
	private int fCurrentTick; 
	/**
	 * int- the duration of the program (termination time)
	 */
	private int fDuration;
	
	
	public TickBroadcast(String senderId, int currentTick, int duration){
		this.fSenderId=senderId;
		this.fCurrentTick=currentTick;
		this.fDuration=duration;
	}
	
	
    public String getSenderId() {
        return fSenderId;
    }
    
    public int getCurrentTick(){
    	return this.fCurrentTick;
    }
    
    public int getDuration(){
    	return this.fDuration;
    }
}
