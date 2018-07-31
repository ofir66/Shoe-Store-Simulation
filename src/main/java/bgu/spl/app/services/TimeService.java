package bgu.spl.app.services;
 
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import bgu.spl.app.messages.TickBroadcast;
import bgu.spl.app.messages.TimeServiceClock;
import bgu.spl.mics.MicroService;

/**
 * 
 * The service which is responsible
for counting how much clock ticks passed since the beginning of its execution and notifying every
other micro service (thats interested) about it using the TickBroadcast.
 *
 */

public class TimeService extends MicroService{
    
	/**
	 * Logger- a logger for printing commands
	 */
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	/**
	 * int- the current tick in the clock
	*/
    private int fCurrentTime;
    /**
     * int- the number of milliseconds each clock tick takes 
     */
    private int fSpeed;
    /**
     * int- the duration of the program
     */
    private int fDuration;
    /**
     * Timer- an object that will help us notify about clock ticks
     */
    private final Timer fTimer;
    /**
     * CountDownLatch- an object for indicating when the {@link TimeService} starts sending ticks
     * <p>
     * will be received at this micro-service constructor with the number of services not including the timer
     * <p>
     * will wait at the beginning of his initialize method for this latch to go down to 0 (by count down of all other services)
     */
    private CountDownLatch fLatchObject;
    /**
     * CountDownLatch- an object for indicating when the {@link bgu.spl.app.passiveObjects.ShoeStoreRunner ShoeStoreRunner} should terminate
     * <p>
     * will be received at this micro-service constructor with the number of services including the TimeService
     *<p>
     * will count down at termination
     */
    private CountDownLatch fLatchObjectForEnd;
     
    /**
     * 
     * @param speed the number of milliseconds each clock tick takes 
     * @param duration the duration of the program
     * @param latchObject an object for indicating when the {@link TimeService} starts sending ticks.
     * @param latchObjectForEnd an object for indicating when the {@link bgu.spl.app.passiveObjects.ShoeStoreRunner ShoeStoreRunner} should terminate.
     */
     
    public TimeService(int speed, int duration, CountDownLatch latchObject, CountDownLatch latchObjectForEnd){
        super("timerService");
        this.fSpeed=speed;
        this.fDuration=duration;
        this.fTimer= new Timer();
        this.fLatchObject=latchObject;
        this.fLatchObjectForEnd=latchObjectForEnd;
    }
     
    /**
     * via his initialize, the TimeService first waits for all other services to finish their initialize.
     * Afterwards, it will notify all other services about the current tick, and when it is time to terminate.
     */
     
    protected void initialize(){
    	try {
			FileHandler handler = new FileHandler("Log/TimeService.txt");
			handler.setFormatter(new SimpleFormatter());
			LOGGER.addHandler(handler);
		} catch (SecurityException e1) {
			LOGGER.severe(e1.getMessage());
		} catch (IOException e1) {
			LOGGER.severe(e1.getMessage());
		}
		LOGGER.info("timerService started");
        try{
            this.fLatchObject.await(); // as described in the field documentation 
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
        // when current time<=duration => sends tick broadcast to services
        // when current time= duration+1 => it is the last tick that will be sent, and when the services get it, they will immediately terminate. 
        // the TimeService will terminate right after them, sending himself the garbage message TimeServiceClock for getting out of awaitMessage method at MicroService Class
        this.fTimer.schedule(new TimerTask(){ 
            public void run(){
                fCurrentTime++;
                if (fCurrentTime>fDuration+1){
                	terminate();
                    LOGGER.warning(getName()+ " terminates");
                    sendBroadcast(new TimeServiceClock());
                    fTimer.cancel();
                    this.cancel();
                    fLatchObjectForEnd.countDown();
                }
                else{
                    TickBroadcast tickBroadcast=new TickBroadcast(getName(), fCurrentTime, fDuration);
                    sendBroadcast(tickBroadcast);
                }
            }
        }, 0, this.fSpeed); 
        // subscribing to the message which indicates that all the services terminates, and now the TimeService can terminate as well
        this.subscribeBroadcast(TimeServiceClock.class, timeServiceClock -> {}); 
    }
     
}