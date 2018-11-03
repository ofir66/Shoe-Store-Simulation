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
* This service is responsible for counting the clock ticks passed since the beginning of its execution.
Also responsible for notifying other micro services about the clock ticks passed using the TickBroadcast.
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
  * Timer- an auxiliary object for notifying about clock ticks
  */
  private final Timer fTimer;
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


  public TimeService(int speed, int duration, CountDownLatch latchObject, CountDownLatch latchObjectForEnd){
    super("timerService");
    this.fSpeed=speed;
    this.fDuration=duration;
    this.fTimer= new Timer();
    this.fLatchObject=latchObject;
    this.fLatchObjectForEnd=latchObjectForEnd;
  }

  /**
  * Waits for all other services to finish their initialization, and afterwards updates those services about passed ticks.
  */

  protected void initialize(){
    try {
      FileHandler handler = new FileHandler("Log/TimeService.txt");
      handler.setFormatter(new SimpleFormatter());
      LOGGER.addHandler(handler);
    } 
    catch (SecurityException e1) {
      LOGGER.severe(e1.getMessage());
    } 
    catch (IOException e1) {
      LOGGER.severe(e1.getMessage());
    }
    LOGGER.info("timerService started");
    try{
      this.fLatchObject.await();
    }
    catch(InterruptedException e){
      e.printStackTrace();
    }

    scheduleTimerTask(); 

    // subscribe to the message which indicates that all other services terminated, and now the TimeService can terminate as well
    this.subscribeBroadcast(TimeServiceClock.class, timeServiceClock -> {}); 
  }

  private void scheduleTimerTask() {
    // when current time <= duration => sends tick broadcast to services
    // when current time == duration+1 => it is the last tick that will be sent, and when the services get it - they will immediately terminate. 
    //									  the TimeService will terminate right after them.
    this.fTimer.schedule(new TimerTask(){ 
      public void run(){
        fCurrentTime++;
        if (fCurrentTime>fDuration+1){
          terminate();
          LOGGER.info(getName()+ " terminates");
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
  }
     
}