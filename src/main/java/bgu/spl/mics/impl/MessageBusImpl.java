package bgu.spl.mics.impl;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import bgu.spl.mics.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
 
/** 
 * This class describes an implementation for a {@link bgu.spl.mics.MessageBus MessageBus} used for communication between micro-services.
 * 
 * 
*/

public class MessageBusImpl implements MessageBus {

  /**
  * A ConcurrentHashMap of Micro-services, when each one have a queue of Messages.
  */
  private final ConcurrentHashMap<MicroService, ConcurrentLinkedQueue<Message>> fMicroServices; 
  /**
  * A ConcurrentHashMap of types of Request messages, each mapped with a ConcurrentLinkedQueue of Micro-services which subscribed to it.
  */
  private final ConcurrentHashMap<Class<? extends Request>, ConcurrentLinkedQueue<MicroService>> fSubscribedRequestList; 
  /**
  * A ConcurrentHashMap of types of Broadcast messages, each mapped with a ConcurrentLinkedQueue of Micro-services which subscribed to it.
  */
  private final ConcurrentHashMap<Class<? extends Broadcast>, ConcurrentLinkedQueue<MicroService>> fSubscribedBroadcastList; 
  /**
  * A ConcurrentHashMap of types of Request messages, each mapped with an index which indicates who is the next micro-service to handle a Request, in the Request subscribed list.
  */
  private final ConcurrentHashMap<Class<? extends Request>, AtomicInteger> fRoundRobin; 
  /**
  * A ConcurrentHashMap of messages, each mapped with a MicroService which sent it.
  */
  private final ConcurrentHashMap<Message, MicroService> fmessagesRequests;
  /**
  * A ConcurrentHasMap of MicroServices, each mapped with an AtomicBoolean with value true - if registered; false - otherwise.
  */
  private final ConcurrentHashMap<MicroService, AtomicBoolean> fRegisterList;
  /**
  * An Object represents a lock in order to synchronize the method SubscribeRequest.
  */
  private final Object fLockSubscribeRequest;
  /**
  * same description as of the field fLockSubscribeRequest, just for SubscribeBroadcast method.
  */
  private final Object fLockSubscribeBroadcast;
  /**
  * An Object represents a lock in order to synchronize the methods isRegistered, unregister and nextInRoundRobinFashion.
  */
  private final Object fLockUnregister;

  /**
  * Singleton implementation
  */
  private static class MessageBusImplHolder {
    private static MessageBusImpl instance = new MessageBusImpl();
  }

  private MessageBusImpl(){
    fMicroServices=new ConcurrentHashMap<MicroService, ConcurrentLinkedQueue<Message>>();
    fSubscribedRequestList= new ConcurrentHashMap<Class<? extends Request>, ConcurrentLinkedQueue<MicroService>>();
    fSubscribedBroadcastList= new ConcurrentHashMap<Class<? extends Broadcast>, ConcurrentLinkedQueue<MicroService>>();
    fRoundRobin= new ConcurrentHashMap<Class<? extends Request>, AtomicInteger>();
    fmessagesRequests= new ConcurrentHashMap<Message, MicroService>();
    fRegisterList= new ConcurrentHashMap<MicroService, AtomicBoolean>();
    fLockSubscribeRequest=new Object();
    fLockSubscribeBroadcast= new Object();
    fLockUnregister=new Object();
  }

  public static MessageBusImpl getInstance() {
    return MessageBusImplHolder.instance;
  }

  public void register(MicroService m){
    if (!this.isRegistered(m)){
      fMicroServices.put(m,new ConcurrentLinkedQueue<Message>());
      this.fRegisterList.put(m, new AtomicBoolean(true));
    }    
  }

  /**
  * Checks if a MicroService is registered to the MessageBusImpl.
  * 
  * @param m the MicroService to check if registered to this MessageBusImpl.
  * @return true if m is registered to MessageBusImpl and false otherwise.
  */
  private boolean isRegistered(MicroService m){
    synchronized(this.fLockUnregister){ 
      if (this.fRegisterList.get(m)==null)
        return false;
      else
        return this.fRegisterList.get(m).get();
    }
  }

  public void unregister(MicroService m){
    synchronized(this.fLockUnregister){
      if(this.fRegisterList.get(m)!=null && this.fRegisterList.get(m).get()==true){
        this.fRegisterList.get(m).getAndSet(false); // define m as unregistered
        this.fMicroServices.remove(m);
        deleteReferences(m);
      }
    }    
  }


  /**
  * Deletes all references to a given MicroService
  * 
  * @param m the MicroService to delete its references
  */
  private void deleteReferences(MicroService m){
    for (Class<? extends Request> mes: this.fSubscribedRequestList.keySet()){
      ConcurrentLinkedQueue<MicroService> subscribedReqList=this.fSubscribedRequestList.get(mes);
      if (subscribedReqList!=null && subscribedReqList.size()>0 && subscribedReqList.contains(m)) {
        removeServiceFromSubReq(m, mes, subscribedReqList);
      }
    }

    for (Class<? extends Broadcast> mes: this.fSubscribedBroadcastList.keySet()){
      ConcurrentLinkedQueue<MicroService> subscribedBroadList=this.fSubscribedBroadcastList.get(mes); 
      if (subscribedBroadList!=null)
        subscribedBroadList.remove(m);
    }
  }

  private void removeServiceFromSubReq(MicroService m, Class<? extends Request> mes, ConcurrentLinkedQueue<MicroService> subscribedList) {
    if (this.fRoundRobin.get(mes).intValue()>0){ // if  the next to handle isn't the first element at the subscribe list
      AtomicInteger next=this.fRoundRobin.get(mes); // the index of the next MicroService to handle requests
      
      if (m==this.findElementAt(next.intValue(), subscribedList)){ // if m is the next to handle the request
        if (next.intValue()==subscribedList.size()-1 || subscribedList.size()==1){// if m is the last in the subscribe list or the only one in it
          next.set(0); // the first element in the subscribe list will now be the next to handle the request
          subscribedList.remove(m);
        }
        else{ // if there is a MicroService after m
          MicroService nextToHandle= this.findElementAt(next.intValue()+1, subscribedList);
          subscribedList.remove(m);
          next.set(this.indexOfElementInList(nextToHandle, subscribedList)); // set the index of "nextToHandle" in the updated list (after removing "m" from it) to be the index of "nextToHandle" in the old list
        }
      }
      else if (this.indexOfElementInList(m, subscribedList)>next.intValue()) // if the next to handle the request has an index which is smaller than "m" index
        subscribedList.remove(m);
      else { // if the next to handle the request has an index which is greater than "m" index
        next.set(next.intValue()-1);
        subscribedList.remove(m);
      }
    }
    else // if the next to handle the request is the first element at it
      subscribedList.remove(m);
  }


  /**
  * Finds the j element in a subscribedList.
  * 
  * @param j index in the subscribedList of the wanted MicroService
  * @param subscribedList list of MicroServices which subscribed to the same Message
  * @return the j-th element in the subscribedList if exists and null otherwise.
  */
  private MicroService findElementAt(int j, ConcurrentLinkedQueue<MicroService> subscribedList){
    Iterator<MicroService> k= subscribedList.iterator();

    if (!k.hasNext())
      return null;
    for (int i=0; i<j; ++i){
      if (!k.hasNext()) // when there are less then j elements in the subscribedList
        return null;
      k.next();
    }
    if (!k.hasNext()) // if the subscribedList has only j-1 elements
      return null;

    return k.next();
  }

  /**
  * Finds the index of a MicroService in a subscribedList.
  * 
  * @param m the MicroService to find its index in the subscribedList.
  * @param subscribedList the subscribe list to search m index.
  * @return the index of m in subscribedList. If subscribedList doesn't contain m, return -1.
  */
  private int indexOfElementInList(MicroService m, ConcurrentLinkedQueue<MicroService> subscribedList){
    int index=-1;
    Iterator<MicroService> i= subscribedList.iterator();

    while (i.hasNext()){
      MicroService next;
      
      index++;
      next=i.next();
      if (m==next)
        break;
    }

    return index;
  }

  public Message awaitMessage(MicroService m) throws InterruptedException{
    ConcurrentLinkedQueue<Message> q;

    if(!isRegistered(m))
      throw new IllegalStateException(m.getName()+" can't wait for messages if it is not registered");

    q=this.fMicroServices.get(m);
    while (q.isEmpty()){
      synchronized(this){
        this.wait(); 
      }
    }   

    return q.poll();
  }


  // Returns the Request subscribed list for the given message type input. If the input isn't in any list -
  // it will be added to a new subscribedList for this type.
  private ConcurrentLinkedQueue<MicroService> subscribeListOfRequestType(Class<? extends Request> type){
    boolean isFound=false;
    ConcurrentLinkedQueue<MicroService> subscribedList=null;

    for (Class<? extends Request> mes: this.fSubscribedRequestList.keySet()){ 
      if (mes.getName().compareTo(type.getName())==0){ // message was found
        isFound=true;
        subscribedList=this.fSubscribedRequestList.get(mes);  
        if (subscribedList==null)
          subscribedList=new ConcurrentLinkedQueue<MicroService>();
        break;
      }
    }

    if (!isFound){ // didn't find the message
      subscribedList=new ConcurrentLinkedQueue<MicroService>(); // create new list for this type of message
      this.fSubscribedRequestList.put(type, subscribedList);
      this.fRoundRobin.put(type, new AtomicInteger()); // also add it to fRoundRobin list- for later usage.
    }
    if (subscribedList==null)
      throw new RuntimeException("illegal situation at subscribeListOfRequestType method");

    return subscribedList;
  }

  // Same as subscribeListOfRequestType, just for Broadcast subscribed list
  private ConcurrentLinkedQueue<MicroService> subscribeListOfBroadcastType(Class<? extends Broadcast> type){
    boolean isFound=false;
    ConcurrentLinkedQueue<MicroService> subscribedList=null;

    for (Class<? extends Broadcast> mes: this.fSubscribedBroadcastList.keySet()){  
      if (mes.getName().compareTo(type.getName())==0){ 
        isFound=true;
        subscribedList=this.fSubscribedBroadcastList.get(mes);  
        if (subscribedList==null)
          subscribedList=new ConcurrentLinkedQueue<MicroService>();     
      }
    }

    if (!isFound){ 
      subscribedList=new ConcurrentLinkedQueue<MicroService>(); 
      this.fSubscribedBroadcastList.put(type, subscribedList);
    }
    if (subscribedList==null)
      throw new RuntimeException("addMessageIfAbsent has returned null- illegal.");

    return subscribedList;
  }

  /**
  * subscribes {@code m} to receive {@link Request}s of type {@code type}.
  * <p>
  * @param type the type to subscribe to
  * @param m    the subscribing MicroService
  */
  public void subscribeRequest(Class<? extends Request<?>> type, MicroService m){ 
    synchronized(this.fLockSubscribeRequest){
      if (isRegistered(m)){
        ConcurrentLinkedQueue<MicroService> subscribedList=this.subscribeListOfRequestType(type); // find the subscribe list for @type
        subscribedList.add(m);
      }
      else
        throw new IllegalStateException(m.getName()+ " tried to subscribe a message, without registering first");
    }
  }    


  /**
  * subscribes {@code m} to receive {@link Broadcast}s of type {@code type}.
  * <p>
  * @param type the type to subscribe to
  * @param m    the subscribing micro-service
  */

  public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m){
    synchronized(this.fLockSubscribeBroadcast){
      if (isRegistered(m)){
        ConcurrentLinkedQueue<MicroService> subscribedList=this.subscribeListOfBroadcastType(type);
        subscribedList.add(m);
      }
      else
        throw new IllegalStateException(m.getName()+ " tried to subscribe a message, without registering first");
    }
  }

  /**
  * add the {@link Broadcast} {@code b} to the message queues of all the
  * micro-services subscribed to {@code b.getClass()}.
  * <p>
  * @param b the message to add to the queues.
  */

  public void sendBroadcast(Broadcast b){
    Class<? extends Broadcast> bClassRepresentation=b.getClass();
    ConcurrentLinkedQueue<MicroService> subscribedList=subscribeListOfBroadcastType(bClassRepresentation);
    Iterator<MicroService> i = subscribedList.iterator();

    while(i.hasNext()){ // iterate over the micro-services that subscribed to "b"
      ConcurrentLinkedQueue<Message> q;
      MicroService m= i.next();
      
      if (!this.isRegistered(m))
        throw new IllegalStateException("A micro-services that is subscribed to a message must be registered");

      q=this.fMicroServices.get(m);
      synchronized(this){
        q.add(b);
        this.notifyAll(); // notify the MicroServices for the new message that was sent
      }
    } 
  }

  /**
  * add the {@link Request} {@code r} to the message queue of one of the
  * micro-services subscribed to {@code r.getClass()} in a round-robin
  * fashion.
  * <p>
  * @param r         the request to add to the queue.
  * @param requester the {@link MicroService} sending {@code r}.
  * @return true if there was at least one micro-service subscribed to
  *         {@code r.getClass()} and false otherwise.
  */
  public boolean sendRequest(Request<?> r, MicroService requester){
    Class<? extends Request> rClassRepresentation;
    ConcurrentLinkedQueue<MicroService> subscribedList;

    if (!this.isRegistered(requester))
      throw new IllegalStateException(requester.getName()+ " tried to send a request, but he is not registered");

    this.fmessagesRequests.put(r, requester);
    rClassRepresentation=r.getClass();
    subscribedList=subscribeListOfRequestType(rClassRepresentation); // find the micro-services that were subscribed to Request messages
    if(subscribedList.size()==0){ // if no one subscribed
      return false;
    }   
    else{ // if someone subscribed, find the next MicroService to handle the message, in a round-robin fashion
      ConcurrentLinkedQueue<Message> q;
      MicroService m=nextInRoundRobinFashion(subscribedList, rClassRepresentation);
      
      if (!this.isRegistered(m))
        throw new IllegalStateException("Illegal state- The next to handle a request must be registered");

      q=this.fMicroServices.get(m);
      synchronized(this){
        q.add(r);
        this.notifyAll(); // notify the MicroServices for the new message that was sent
      }   
      return true;
    }
  }

  // Finds the next MicroService to handle a request message in round-robin fashion. 
  // After finding that MicroService, updates the index of the following MicroService in the subscribe list (in round robin fashion) that needs to handle a request message 
  private MicroService nextInRoundRobinFashion(ConcurrentLinkedQueue<MicroService> subscribedList, Class <? extends Request> type){
    synchronized(this.fLockUnregister){
      AtomicInteger index= this.fRoundRobin.get(type); // find the index of the next MicroService in the subscribeList to handle the message (round-robin fashion)
      Iterator<MicroService> j= subscribedList.iterator(); // iterate over the subscribe list to find the MicroService with this index
      for (int i=0; i<index.intValue(); ++i) // get here the [index-1] element. the [index] element will be returned at the last line of the method
        j.next();
      index.updateAndGet(value-> value+1>subscribedList.size()-1? 0: value+1); // update the index in round-robin fashion, for the next Request messages

      return j.next(); 
    }
  }


  /**
  * Notifying the MessageBus that the request {@code r} is completed and its
  * result was {@code result}.
  * When this method is called, the message-bus will implicitly add the
  * special {@link RequestCompleted} message to the queue
  * of the requesting micro-service, the RequestCompleted message will also
  * contain the result of the request ({@code result}).
  * <p>
  * @param <T>    the type of the result expected by the completed request
  * @param r      the completed request
  * @param result the result of the completed request
  */
  public  <T> void  complete(Request<T> r, T result){
    Message completedMessage=new RequestCompleted<T>(r,result);
    MicroService microServiceRequested=this.fmessagesRequests.get(r); // find the MicroService requested r
    ConcurrentLinkedQueue<Message> q;

    if (microServiceRequested==null)
      throw new IllegalStateException("illegal state- someone must has sent a request message if we now try to complete it");
    if (!this.isRegistered(microServiceRequested))
      throw new IllegalStateException("a micro-service will not unregister itself if it has pending requests");

    q=this.fMicroServices.get(microServiceRequested); // find the MicroService fMicroServices list, and then add the completed message to its queue
    synchronized(this){
      q.add(completedMessage);
      this.notifyAll(); // notify the MicroServices for the new message that was sent
    }
  }
    
}