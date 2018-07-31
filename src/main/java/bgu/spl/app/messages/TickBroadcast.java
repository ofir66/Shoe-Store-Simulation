package bgu.spl.app.messages;

import bgu.spl.mics.Broadcast;

/**
 * a broadcast messages that is sent at every passed clock tick
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
	
	/**
	 * the sender name
	 * @param currentTick the current tick at the clock
	 * @param duration the duration of the program (termination time)
	 */
	
	public TickBroadcast(String senderId, int currentTick, int duration){
		this.fSenderId=senderId;
		this.fCurrentTick=currentTick;
		this.fDuration=duration;
	}
	
	/**
	 * 
	 * @return the sender name
	 */
    public String getSenderId() {
        return fSenderId;
    }
    
    /**
     * 
     * @return the current tick at the clock
     */
    public int getCurrentTick(){
    	return this.fCurrentTick;
    }
    
    /**
     * 
     * @return the duration of the program (termination time)
     */
    public int getDuration(){
    	return this.fDuration;
    }
}
