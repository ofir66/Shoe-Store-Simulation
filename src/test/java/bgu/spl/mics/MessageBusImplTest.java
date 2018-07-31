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

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void testGetInstance() {
		assertEquals(this.messageBusImpl,MessageBusImpl.getInstance());
	}

	@Test
	public void testRegister() {
		this.messageBusImpl.register(m);
		assertFalse(this.messageBusImpl.sendRequest(new PurchaseOrderRequest("sender", null, false, 0, 0),m));//test should work because there is no one to handle the request, will fail (give an exception) if m wasn't registered
		//would print to the console: "there is no one to handle test request of type: PurchaseOrderRequest"
	}

	@Test (expected=IllegalStateException.class) // we expect this exception because awaitMessage won't find m in the bus.
	public void testUnregister() {
		this.args = new String[]{"broadcast"};
		this.m = new ExampleMessageSenderService("test", args);
		this.messageBusImpl.register(m); // we already tested register so we can assume it's good
		this.messageBusImpl.unregister(m);
		try {
			assertNotNull(this.messageBusImpl.awaitMessage(m));
		} catch (InterruptedException e) {
			e.getStackTrace();
		} 
	}

	// In unregister we saw that awaitMessage works fine when m is not registered.
	// We would like to see if it works when m is registered
	@Test
	public void testAwaitMessage() {
		Broadcast mes = new ExampleBroadcast("broadcast");
		this.messageBusImpl.register(m); // we already tested register so we can assume it's good
		this.messageBusImpl.subscribeBroadcast(mes.getClass(), m); // m wants to recieve broadcasts
		this.messageBusImpl.sendBroadcast(mes); // sends a broadcast
		try {
			assertNotNull(this.messageBusImpl.awaitMessage(m)); // our broadcast message supposed to return - it's not null!
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testSubscribeRequest() { // if it is subscribed to request it means that when someone sends broadcast it will be notified by awaitMessage
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
		
		this.messageBusImpl.register(handler);
		this.messageBusImpl.register(sender);
		Thread handlerT = new Thread(handler);
		Thread senderT = new Thread(sender);
		
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
		
		this.messageBusImpl.register(handler);
		this.messageBusImpl.register(sender);
		Thread handlerT = new Thread(handler);
		Thread senderT = new Thread(sender);
		
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
		
		this.messageBusImpl.register(handler);
		this.messageBusImpl.register(sender);
		Thread handlerT = new Thread(handler);
		Thread senderT = new Thread(sender);
		
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
		this.messageBusImpl.register(sender);
		Thread senderT = new Thread(sender);
		senderT.start();
		
		MicroService sender2= new MicroService("sender2"){
			protected void initialize(){
				assertTrue(this.sendRequest(new ExampleRequest("request"), result -> {}));
			}
		};
		this.messageBusImpl.register(sender2);
		Thread sender2T = new Thread(sender2);
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
		
		this.messageBusImpl.register(handler);
		this.messageBusImpl.register(sender);
		Thread handlerT = new Thread(handler);
		Thread senderT = new Thread(sender);
		
		handlerT.start();
		try{
			Thread.sleep(50);
		}
		catch (InterruptedException e){}
		senderT.start();
	}

}
