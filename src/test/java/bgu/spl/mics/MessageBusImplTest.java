package bgu.spl.mics;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import bgu.spl.app.messages.ManufacturingOrderRequest;
import bgu.spl.app.messages.PurchaseOrderRequest;
import bgu.spl.app.messages.TickBroadcast;
import bgu.spl.mics.example.messages.ExampleBroadcast;
import bgu.spl.mics.example.messages.ExampleRequest;
import bgu.spl.mics.example.services.ExampleMessageSenderService;
import bgu.spl.mics.impl.MessageBusImpl;

public class MessageBusImplTest {

  MessageBus messageBusImpl;
  String[] args;
  MicroService m;

  @Before
  public void setUp() throws Exception {
    this.messageBusImpl = MessageBusImpl.getInstance();
    this.args = new String[]{"broadcast"};
    this.m = new ExampleMessageSenderService("test", args);
  }

  @Test
  public void testGetInstance() {
    assertEquals(this.messageBusImpl,MessageBusImpl.getInstance());
  }

  @Test
  public void testRegister() {
    PurchaseOrderRequest req = new PurchaseOrderRequest("sender", null, false, 0, 0);

    this.messageBusImpl.register(m);
    assertFalse(this.messageBusImpl.sendRequest(req,m));// needs to be false since there is no one to handle the request in this case
  }

  @Test (expected=IllegalStateException.class) // expect this exception because awaitMessage won't find m in the message bus.
  public void testUnregister() {
    this.messageBusImpl.register(m);
    this.messageBusImpl.unregister(m);
    try {
      assertNotNull(this.messageBusImpl.awaitMessage(m));
    } 
    catch (InterruptedException e) {
      e.getStackTrace();
    } 
  }

  @Test
  public void testAwaitMessage() {
    Broadcast mes = new ExampleBroadcast("broadcast");

    this.messageBusImpl.register(m);
    this.messageBusImpl.subscribeBroadcast(mes.getClass(), m);
    this.messageBusImpl.sendBroadcast(mes);
    try {
      assertNotNull(this.messageBusImpl.awaitMessage(m));
    } 
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  // check the following case: if a MicroService m is subscribed to a request, it means that when someone sends a request - m will be notified by awaitMessage
  @Test
  public void testSubscribeRequest() {
    MicroService handler= new MicroService("handler"){
      protected void initialize(){
        this.subscribeRequest(ExampleRequest.class, ExampleRequest -> {
          this.complete(ExampleRequest, "success");
        });
      }
    };
    MicroService sender= new MicroService("sender"){
      protected void initialize(){
        this.sendRequest(new ExampleRequest("request"), result -> {
          assertEquals("success", result);
        });
      }
    };

    Thread handlerT = new Thread(handler);
    Thread senderT = new Thread(sender);

    this.messageBusImpl.register(handler);
    this.messageBusImpl.register(sender);
    handlerT.start();
    try{
      Thread.sleep(50);
    }
    catch (InterruptedException e){}
    senderT.start();
  }

  @Test
  public void testSubscribeBroadcast() {
    MicroService handler= new MicroService("handler"){
      protected void initialize(){
        this.subscribeBroadcast(ExampleBroadcast.class, ExampleBroadcast -> {
          System.out.println("echo");
          assert(true);
        });
      }
    };
    MicroService sender= new MicroService("sender"){
      protected void initialize(){
        this.sendBroadcast(new ExampleBroadcast("broadcast"));
      }
    };

    Thread handlerT = new Thread(handler);
    Thread senderT = new Thread(sender);

    this.messageBusImpl.register(handler);
    this.messageBusImpl.register(sender);
    handlerT.start();
    try{
      Thread.sleep(50);
    }
    catch (InterruptedException e){}
    senderT.start();
  }

  @Test
  public void testSendBroadcast() {
    MicroService handler= new MicroService("handler"){
      protected void initialize(){
        this.subscribeBroadcast(TickBroadcast.class, TickBroadcast -> {
          System.out.println("testSendBroadcast");
          assert(true);
        });
      }
    };
    MicroService sender= new MicroService("sender"){
      protected void initialize(){
        this.sendBroadcast(new TickBroadcast("sender",0,0));
      }
    };

    Thread handlerT = new Thread(handler);
    Thread senderT = new Thread(sender);

    this.messageBusImpl.register(handler);
    this.messageBusImpl.register(sender);
    handlerT.start();
    try{
      Thread.sleep(50);
    }
    catch (InterruptedException e){}
    senderT.start();
  }

  @Test
  public void testSendRequest() {
    MicroService sender= new MicroService("sender"){
      protected void initialize(){
        assertFalse(this.sendRequest(new PurchaseOrderRequest("sender", null, false, 0, 0), result -> {}));
      }
    };
    MicroService sender2= new MicroService("sender2"){
      protected void initialize(){
        assertTrue(this.sendRequest(new ExampleRequest("request"), result -> {}));
      }
    };

    Thread senderT = new Thread(sender);
    Thread sender2T = new Thread(sender2);

    this.messageBusImpl.register(sender);
    senderT.start();

    this.messageBusImpl.register(sender2);
    sender2T.start();
  }


  @Test
  public void testComplete() {
    MicroService handler= new MicroService("handler"){
      protected void initialize(){
        this.subscribeRequest(ManufacturingOrderRequest.class, ManufacturingOrderRequest -> {
          this.complete(ManufacturingOrderRequest, null);
        });
      }
    };
    MicroService sender= new MicroService("sender"){
      protected void initialize(){
        ManufacturingOrderRequest req= new ManufacturingOrderRequest("","",0,0);
        this.sendRequest(req, result -> {
          assertNull(result);
        });
      }
    };

    Thread handlerT = new Thread(handler);
    Thread senderT = new Thread(sender);

    this.messageBusImpl.register(handler);
    this.messageBusImpl.register(sender);
    handlerT.start();
    try{
      Thread.sleep(50);
    }
    catch (InterruptedException e){}
    senderT.start();
  }

}