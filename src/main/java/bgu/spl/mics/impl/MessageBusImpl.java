package bgu.spl.mics.impl;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue; // *consider replacing it by ConcurrentLinkedQueue as oran shuster use. ConcurrentLinkedQueue is wrriten as thread-safe, when on ConcurrentLinkedQueue it doesn't wrriten. problem with ConcurrentLinkedQueue: doesn't have take method (but have peek and remove)
import bgu.spl.mics.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
 
/** 
 * This class describes an implemantation for a {@link bgu.spl.mics.MessageBus MessageBus} used for communication between micro-services.
 * 
 * @author Ofir Hauzer & Liraz Reichenstein
 * 
 */

public class MessageBusImpl implements MessageBus {

	/**
	 * A ConcurrentHashMap of Micro-services, each mapped with a queue of Messages.
	 */
    private final ConcurrentHashMap<MicroService, ConcurrentLinkedQueue<Message>> fMicroServices; 
    /**
     * A ConcurrentHashMap of types of A ConcurrentHashMap of types of Request Messages, each mapped with a ConcurrentLinkedQueue of Micro-services which subscribed to it. Messages, each mapped with a ConcurrentLinkedQueue of Micro-services which subscribed to it.
     */
    private final ConcurrentHashMap<Class<? extends Request>, ConcurrentLinkedQueue<MicroService>> fSubscribedRequestList; 
    /**
     * A ConcurrentHashMap of types of Broadcast Messages, each mapped with a ConcurrentLinkedQueue of Micro-services which subscribed to it.
     */
    private final ConcurrentHashMap<Class<? extends Broadcast>, ConcurrentLinkedQueue<MicroService>> fSubscribedBroadcastList; 
    /**
     * A ConcurrentHashMap of types of Request Messages, each mapped with an index which indicates who is the next to handle a message from this type, in the subscribed list of this message.
     */
    private final ConcurrentHashMap<Class<? extends Request>, AtomicInteger> fRoundRobin; 
    /**
     * A ConcurrentHashMap of Messages, each mapped with a MicroService which sent it.
     */
    private final ConcurrentHashMap<Message, MicroService> fmessagesRequests;
    /**
     * A ConcurrentHasMap of MicroServices, each mapped with an AtomicBoolean with value true - if registered; false - otherwise.
     */
    private final ConcurrentHashMap<MicroService, AtomicBoolean> fRegisterList;
    /**
     * An Object represents a lock in order to lock the method SubscribeRequest. It is needed in case there are two services (or more)
     * who try to subscribe to the same type of Request which was never subscribed before.
     * In case like that, we will want to define a new Micro-Service queue for this type of message, and add the micro-services that subscribed to it.
     * But those micro-services can reach the method SubscribeRequest at the same time and a new queue might be created for each of them, instead of one queue to all of them.
     * this lock is meant to prevent this case from happening.
     */
    private final Object fLockSubscribeRequest;
    /**
     * same description as of the field fLockSubscribeRequest, just for SubscribeBroadcast method.
     */
    private final Object fLockSubscribeBroadcast;
    /**
     * An Object represents a lock in order to lock the methods isRegistered (private method), unregister and nextInRoundRobinFashion (private method).
     * this lock is meant to prevent the following cases:<p>
     * 1. Calling isRegistered method for some micro-service, when this micro-service is at unregistering proccess. in this case. the method isRegistered
     * can return true, although this micro-service is unregistering.<p>
     * 2. Calling  nextInRoundRobinFashion method for finding the next micro-service to handle a request message, when some micro-service that subscribed to this
     * request is unregistering. in case like that- the  nextInRoundRobinFashion method might return the wrong micro-service to handle a request, since this method is not
     * updated with the unregistering method that might has changed the subscribed list of this certain request.
     */
    private final Object fLockUnregister;
     
     /**
      * This private static class is meant for the owner of the MessageBusImpl class to hold it as a Singleton.
      * If anyone want to access the MessageBusImpl he won't be able to do it directly, as we only want a sole
      * instance of it. That is why we will have a MessageBusImplHolder who'll contain this sole instance, in which
      * we will initialize our MessageBusImpl with it's private constructor. If anyone wants to gain access to the
      * MessageBusImpl, he would have to ask the MessageBusImpl for it's instance. This ofcourse would be able to
      * be done from inside the MessageBusImpl class ONLY. (as it's a private class)
      * 
      * @author Ofir Hauzer & Liraz Reichenstein
      *
      */
    private static class MessageBusImplHolder {
          private static MessageBusImpl instance = new MessageBusImpl();
    }
     /**
      * Private default constructor.
      * Simply initialize all of MessageBusImpl fields with their default constructors.            
      */
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
     
    /**
     * 
     * @return the instance of our MessageBusImpl
     */
    public static MessageBusImpl getInstance() {
         return MessageBusImplHolder.instance;
    }
     
    /**
     * allocates a message-queue for the {@link MicroService} {@code m}.
     * <p>
     * @param m the micro-service to create a queue for.
     */
     
    public void register(MicroService m){
        if (!this.isRegistered(m)){
            fMicroServices.put(m,new ConcurrentLinkedQueue<Message>());
            this.fRegisterList.put(m, new AtomicBoolean(true));
        }    
    }
     
    /**
     * Auxiliary method. Let us know if a MicroService is registered to the MessageBusImpl.
     * 
     * @param m the microservice we want to check if registered to this MessageBusImpl.
     * @return true if m is registered to MessageBusImpl - false otherwise.
     */
    private boolean isRegistered(MicroService m){
    	synchronized(this.fLockUnregister){ 
		    if (this.fRegisterList.get(m)==null)
		   		return false;
		   	else
		   		return this.fRegisterList.get(m).get();
    	}
    }
    
    /**
     * remove the message queue allocated to {@code m} via the call to
     * {@link #register(bgu.spl.mics.MicroService)} and clean all references
     * related to {@code m} in this message-bus. If {@code m} was not
     * registered, nothing should happen.
     * <p>
     * @param m the MicroService to unregister.
     */
     
    public void unregister(MicroService m){
    	synchronized(this.fLockUnregister){ // as described in fLockUnregister documentation
	        if(this.fRegisterList.get(m)!=null && this.fRegisterList.get(m).get()==true){
		        	this.fRegisterList.get(m).getAndSet(false); // define m as unregistered
		            this.fMicroServices.remove(m); // delete the micro-service queue
		            deleteReferences(m); // and then delete all of his references
	        }
    	}    
    }
 
     
    /**
     * Auxiliary method- deleting all references of a given MicroService
     * 
     * @param m the MicroService we would like to delete references of
     */
    private void deleteReferences(MicroService m){
    	ConcurrentLinkedQueue<MicroService> subscribedReqList;
    	ConcurrentLinkedQueue<MicroService> subscribedBroadList;
    	
    	for (Class<? extends Request> mes: this.fSubscribedRequestList.keySet()){ // for each type of request message in it list
    		subscribedReqList=this.fSubscribedRequestList.get(mes);  // we will look at it subscribe list
            if (subscribedReqList!=null && subscribedReqList.size()>0 && subscribedReqList.contains(m)) {
                removeServiceFromSubReq(m, mes, subscribedReqList);
            }
        }
       
        for (Class<? extends Broadcast> mes: this.fSubscribedBroadcastList.keySet()){ // for each type of broadcast message
        	subscribedBroadList=this.fSubscribedBroadcastList.get(mes);  // we will look at it subscribe list
            if (subscribedBroadList!=null)
            	subscribedBroadList.remove(m); // and if "m" is there- we will delete it
        }
    }

	private void removeServiceFromSubReq(MicroService m, Class<? extends Request> mes,
			ConcurrentLinkedQueue<MicroService> subscribedList) {
		
		AtomicInteger next=this.fRoundRobin.get(mes); // the index of the next MS to handle requests
		
		if (this.fRoundRobin.get(mes).intValue()>0){ // if  the next to handle isn't the first element at it
		    if (m==this.findElementAt(next.intValue(), subscribedList)){ // if m is the next to handle
		        if (next.intValue()==subscribedList.size()-1 || subscribedList.size()==1){// if m is the last in the list or the only one in the list
		            next.set(0); // the first element will now be the next to handle
		            subscribedList.remove(m); // and we will remove m
		        }
		        else{ // if there is a MS after m
		            MicroService nextToHandle= this.findElementAt(next.intValue()+1,
		            							subscribedList); // we will save the reference of him
		            subscribedList.remove(m); // remove m
		            next.set(this.indexOfElementInList(nextToHandle, subscribedList)); // and set the index of "nextToHandle" in the updated list (after deleting "m" from it) to be the index of "nextToHandle" in the list
		        }
		    }
		    else if (this.indexOfElementInList(m, subscribedList)>next.intValue()) // if the next to handle has an index which is smaller than "m" index
		        subscribedList.remove(m); // we will just remove m
		    else { // if the next to handle has an index which is greater than "m" index
		        next.set(next.intValue()-1); // we will just set the next index to be minus 1
		        subscribedList.remove(m); // and remove m
		    }
		}
		else // if  the next to handle is the first element at it, we will just remove it
		    subscribedList.remove(m); // we will just remove m
	}
     
     
    /**
     * Auxiliary method. find the j element at subscribedList.
     * 
     * @param j index of the subscribedList we would like to get the MicroService in it
     * @param subscribedList list of MicroServices which subscribed to the same Message
     * @return the j-th element in subscribedList if exists - null otherwise.
     */
    private MicroService findElementAt(int j, ConcurrentLinkedQueue<MicroService> subscribedList){
        Iterator<MicroService> k= subscribedList.iterator(); // we will iterate over the subscribed list until we find it
        
        if (!k.hasNext()) // if at start we don't have next
            return null;
        for (int i=0; i<j; ++i){ // we will get here the j-1 element. the j element will be returned at the last line of the method
            if (!k.hasNext()) // a case when we don't have index i (0<i<j) in the list
                return null;
            k.next();
        }
        if (!k.hasNext()) // if we have only j-1 elements
            return null;
        
        return k.next();
    }
     
    /**
     * Auxiliary method. find the index of an element at subscribedList.
     * 
     * @param m the MicroService we want to get the index of in subscribedList.
     * @param subscribedList the list in which we would like to get m's index.
     * @return the index of m in subscribedList. If subscribedList doesn't contain m - returns -1.
     */
    private int indexOfElementInList(MicroService m, ConcurrentLinkedQueue<MicroService> subscribedList){
        int index=-1;
        Iterator<MicroService> i= subscribedList.iterator(); // we will iterate over the subscribed list until we find it
        MicroService next;
        
        while (i.hasNext()){
            index++;
            next=i.next();
            if (m==next)
                break;
        }
        
        return index;
    }
     
 
    /**
     * using this method, a <b>registered</b> micro-service can take message
     * from its allocated queue.
     * This method is blocking -meaning that if no messages
     * are available in the micro-service queue it
     * should wait until a message became available.
     * The method should throw the {@link IllegalStateException} in the case
     * where {@code m} was never registered.
     * <p>
     * @param m the micro-service requesting to take a message from its message
     *          queue
     * @return the next message in the {@code m}'s queue (blocking)
     * @throws InterruptedException if interrupted while waiting for a message
     *                              to became available.
     */
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
     

    //Auxiliary method. we will return the subscribed list for the given message type input. if the input isn't in any list
    // we will add it, create a new subscribedList for it, and return this new subscribedList
    private ConcurrentLinkedQueue<MicroService> subscribeListOfRequestType(Class<? extends Request> type){
        boolean isFound=false;
        ConcurrentLinkedQueue<MicroService> subscribedList=null;
        
        for (Class<? extends Request> mes: this.fSubscribedRequestList.keySet()){  // we will look for the message in our list
            if (mes.getName().compareTo(type.getName())==0){ // if we found the message
                isFound=true;
                subscribedList=this.fSubscribedRequestList.get(mes);  
                if (subscribedList==null) // that can happen if a message was in the list before, and we removed it for some reason
                    subscribedList=new ConcurrentLinkedQueue<MicroService>(); // create a new list for it 
                break; // and then break the loop, because we already found the message and handled the case
            }
        }
        
        if (!isFound){ // if we didn't find the message in the list
            subscribedList=new ConcurrentLinkedQueue<MicroService>(); // we will create new list for it, and add her to the list
            this.fSubscribedRequestList.put(type, subscribedList);
            this.fRoundRobin.put(type, new AtomicInteger()); // we also add her to fRoundRobin list- for later uses.
        }
        if (subscribedList==null)
            throw new RuntimeException("illegal situation at subscribeListOfRequestType method");
        
        return subscribedList;
    }
     
    // Auxiliary method. same as the previous one, just for broadcast
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
    	ConcurrentLinkedQueue<MicroService> subscribedList;
    	
    	synchronized(this.fLockSubscribeRequest){
            if (isRegistered(m)){  // if "m" is registered
                subscribedList=this.subscribeListOfRequestType(type); // find the subscribed list for "type"
                subscribedList.add(m); //and add "m" to it
            }
            else
            	throw new IllegalStateException(m.getName()+ " tried to subscribe a message, without registering first"); // according to Assignment forum: "you should throw some exception if A tries to subscribe without registering "
        }
    }    
     
     
    /**
     * subscribes {@code m} to receive {@link Broadcast}s of type {@code type}.
     * <p>
     * @param type the type to subscribe to
     * @param m    the subscribing micro-service
     */
     
    public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m){
    	ConcurrentLinkedQueue<MicroService> subscribedList;
    	
    	synchronized(this.fLockSubscribeBroadcast){
            if (isRegistered(m)){  // if "m" is registered
                subscribedList=this.subscribeListOfBroadcastType(type);
                subscribedList.add(m);
            }
            else
            	throw new IllegalStateException(m.getName()+ " tried to subscribe a message, without registering first"); // according to Assignment forum: "you should throw some exception if A tries to subscribe without registering "
        }
    }
     
    /**
     * add the {@link Broadcast} {@code b} to the message queues of all the
     * micro-services subscribed to {@code b.getClass()}.
     * <p>
     * @param b the message to add to the queues.
     */
    
    public void sendBroadcast(Broadcast b){
        Class<? extends Broadcast> bClassRepresentation=b.getClass(); // we will need the class representation for searching in our lists
        ConcurrentLinkedQueue<MicroService> subscribedList=subscribeListOfBroadcastType(bClassRepresentation);
        ConcurrentLinkedQueue<Message> q;
        Iterator<MicroService> i = subscribedList.iterator();
        MicroService m;
        
        while(i.hasNext()){ // we will iterate the micro-services that subscribed to "b"
          m= i.next();
          if (!this.isRegistered(m))
              throw new IllegalStateException("A micro-services that is subscribed to a message must be registered");
          
          q=this.fMicroServices.get(m);
          synchronized(this){
              q.add(b); // and add the message "b" to their queue
              this.notifyAll(); // notify them if their queue was empty till now
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
    	ConcurrentLinkedQueue<Message> q;
    	MicroService m;
    	
		if (!this.isRegistered(requester))
            throw new IllegalStateException(requester.getName()+ " tried to send a request, but he is not registered");
        
        this.fmessagesRequests.put(r, requester); // we will update our field (will help us in other method)
        rClassRepresentation=r.getClass();
        subscribedList=subscribeListOfRequestType(rClassRepresentation); // we will find the micro-services that were subscribed to this kind of messages
        if(subscribedList.size()==0){ // if no one subscribed
            return false;
        }   
        else{ // if someone subscribed, we will find the next micro-service to handle the message, in a round-robin fashion
            m=nextInRoundRobinFashion(subscribedList, rClassRepresentation); // detailed explanation in the Auxiliary function
            if (!this.isRegistered(m))
                throw new IllegalStateException("Illegal state- The next to handle a request must be registered");
            
            q=this.fMicroServices.get(m);
            synchronized(this){
                q.add(r); // and add the message to his queue
                this.notifyAll(); // and also notify him if his queue was empty till now
            }   
            return true;
        }
    }
    
    //Auxiliary function. it will find the next MS to handle a request message (in round-robin fashion). after the MS found, we will update the index of the next MS in the queue of that message (in a round robin fashion) that needs to handle a request message 
    private MicroService nextInRoundRobinFashion(ConcurrentLinkedQueue<MicroService> subscribedList, Class <? extends Request> type){
    	AtomicInteger index;
    	Iterator<MicroService> j;
    	
    	synchronized(this.fLockUnregister){
            index= this.fRoundRobin.get(type); // we will find the index of the next MS in the subscribeList to handle the message (round-robin fashion)
            j= subscribedList.iterator(); // we will iterate over the subscribed list until we find the element in this index
            for (int i=0; i<index.intValue(); ++i) // we will get here the [index-1] element. the [index] element will be returned at the last line of the method
                j.next();
            index.updateAndGet(value-> value+1>subscribedList.size()-1? 0: value+1); // now we need to update the index in round-robin fashion, for the next Request messages (after this one) that we will receive
            
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
        MicroService microServiceRequested=this.fmessagesRequests.get(r); // first we find the MS that requested the input
        ConcurrentLinkedQueue<Message> q;
        
        if (microServiceRequested==null)
            throw new IllegalStateException("illegal state- someone must has sent a request message if we now try to complete it");
        if (!this.isRegistered(microServiceRequested))
        	throw new IllegalStateException("a micro-service will not unregister itself if it has pending requests"); // from the forum of the Assignment: "You can assume that a micro-service will not unregister itself if it has pending requests. "
	    
        q=this.fMicroServices.get(microServiceRequested); // we will find the MS in our fMicroServices list, and then add the completed message to his queue
	    synchronized(this){
	        q.add(completedMessage);
	        this.notifyAll(); // notifying him if his queue is empty till now
	    }
    }
    
     
}