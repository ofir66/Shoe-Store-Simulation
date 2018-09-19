package bgu.spl.app;
 
 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import bgu.spl.app.passiveObjects.DiscountSchedule;
import bgu.spl.app.passiveObjects.PurchaseSchedule;
import bgu.spl.app.passiveObjects.ShoeStorageInfo;
import bgu.spl.app.passiveObjects.Store;
import bgu.spl.app.services.ManagementService;
import bgu.spl.app.services.SellingService;
import bgu.spl.app.services.ShoeFactoryService;
import bgu.spl.app.services.TimeService;
import bgu.spl.app.services.WebsiteClientService;
import bgu.spl.mics.MicroService;
 
/**
* Main class of the program which runs the whole simulated shoe store using the 
* {@link bgu.spl.mics.impl.MessageBusImpl MessageBus} and 
* {@link bgu.spl.mics.MicroService MicroServices}.
* <p>
* When started, it will receive as an argument the name of the json input file to 
* read. The ShoeStoreRunner would read the input file (using {@link com.google.gson.Gson Gson} Library),
* then add the initial storage to the store and start the micro-services. 
* <p>
* When the current tick number is larger than the duration given to the TimeService in the input file, all 
* the micro-services would gracefully terminate themselves.
* <p>
* After all the micro-services terminate themselves, the ShoeStoreRunner will call the Store’s
* print function and exit.
* 
*
*/
public class ShoeStoreRunner {
 
  /**
  * Logger - a logger for printing commands
  */
  private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  public static void main(String[] args) {
    JsonReader jreader;
    JsonParser jparser = new JsonParser();
    JsonElement element;
    JsonObject jobject;
    Store store = Store.getInstance();
    CountDownLatch latchForEnding;

    (new File("Log")).mkdir(); // creates a Log folder if doesn't exist
    for(File file: new File("Log").listFiles()) file.delete(); // cleans the Log folder from the former run of the program
    System.setProperty("java.util.logging.SimpleFormatter.format","%4$s: %5$s [%1$tc]%n"); // reorders the log lines to make it easier to the eye

    initFileHandler();
    jreader = openJsonReader(args);
    element = jparser.parse(jreader);

    if (element.isJsonObject()){
      jobject = element.getAsJsonObject();
      parseInitialStorage(store, jobject);
      latchForEnding = parseServices(jobject);
      try{
        latchForEnding.await();
      }
      catch(InterruptedException e){
        e.printStackTrace();
      }
      store.print();
    }
  }

  private static void initFileHandler() {
    FileHandler handler;

    try {
      handler = new FileHandler("Log/ShoeStoreRunner.txt");
      handler.setFormatter(new SimpleFormatter());
      LOGGER.addHandler(handler);
    } 
    catch (SecurityException e1) {
      LOGGER.severe(e1.getLocalizedMessage());
    } 
    catch (IOException e1) {
      LOGGER.severe(e1.getLocalizedMessage());
    }
  }

  private static JsonReader openJsonReader(String[] args) {
    JsonReader jreader=null;

    try {
      jreader = new JsonReader(new FileReader(args[0]));
    } 
    catch (FileNotFoundException e1) {
      LOGGER.severe("File not found. \nSYSTEM IS SHUTTING DOWN!");
      System.exit(1);
    }
    return jreader;
  }

  private static CountDownLatch parseServices(JsonObject jobject) {
    JsonObject services = jobject.get("services").getAsJsonObject();
    JsonObject jTime = services.get("time").getAsJsonObject();
    JsonObject jManager = services.get("manager").getAsJsonObject();
    JsonArray jcustomers = services.get("customers").getAsJsonArray();

    int speed = jTime.get("speed").getAsInt();
    int duration = jTime.get("duration").getAsInt();
    int factories = services.get("factories").getAsInt();
    int sellers = services.get("sellers").getAsInt();
    int numOfServices = 1 + factories + sellers + jcustomers.size(); // 1 is for the manager

    CountDownLatch latchForInit = new CountDownLatch(numOfServices);
    CountDownLatch latchForEnding = new CountDownLatch(numOfServices+1); // +1 for the timer

    List<DiscountSchedule> dischedule = new ArrayList<DiscountSchedule>();
    List<WebsiteClientService> customers = new ArrayList<WebsiteClientService>();
    List<MicroService> listOfSellers = parseSellers(sellers, latchForInit, latchForEnding);

    MicroService timer= new TimeService(speed,duration,latchForInit, latchForEnding);
    MicroService manager;

    parseDiscounts(dischedule, jManager);
    parseCustomers(customers, jcustomers, latchForInit, latchForEnding);

    manager= new ManagementService(dischedule,latchForInit, latchForEnding);

    startStoreSimulation(factories, customers, latchForInit, latchForEnding, timer, manager, listOfSellers);

    return latchForEnding;
  }

  private static List<MicroService> parseSellers(int sellers, CountDownLatch latchForInit, CountDownLatch latchForEnding) {
    List<MicroService> listOfSellers = new ArrayList<MicroService>();

    for (int i = 0; i < sellers; i++){
      listOfSellers.add(new SellingService("Seller "+ (i+1), i+1, latchForInit, latchForEnding));
    }
    return listOfSellers;
  }

  private static void parseDiscounts(List<DiscountSchedule> dischedule, JsonObject manager) {
    JsonArray discounts = manager.get("discountSchedule").getAsJsonArray();
    JsonObject jdiscount;

    for (JsonElement discount : discounts){
      jdiscount = discount.getAsJsonObject();
      dischedule.add(new DiscountSchedule(jdiscount.get("shoeType").getAsString(),jdiscount.get("tick").getAsInt(),jdiscount.get("amount").getAsInt()));
    }
  }

  private static void startStoreSimulation(int factories, List<WebsiteClientService> customers,
                                           CountDownLatch latchForInit, CountDownLatch latchForEnding, MicroService timer, MicroService manager,
                                           List<MicroService> listOfSellers)
  {

    Thread timerT = new Thread(timer);
    Thread managerT= new Thread(manager);
    Thread customerT;
    Thread sellerT;
    Thread factoryT;

    timerT.start();

    for (int i = 0; i < customers.size(); i++){
      customerT= new Thread(customers.get(i));
      customerT.start();
    }

    for (int i = 0; i < listOfSellers.size(); i++){
      sellerT= new Thread(listOfSellers.get(i));
      sellerT.start();
    }

    managerT.start();

    for (int i = 0; i < factories; i++){
      factoryT = new Thread(new ShoeFactoryService("Factory "+ (i+1),latchForInit, latchForEnding));
      factoryT.start();
    }
  }

  private static void parseCustomers(List<WebsiteClientService> customers, JsonArray jcustomers, CountDownLatch latchForInit, CountDownLatch latchForEnding) {

    JsonObject jcustomer;
    JsonObject jpurchase;
    JsonArray purchaseschedule;
    JsonArray jwishList;
    List<PurchaseSchedule> purchaseList;
    PurchaseSchedule pschedule;
    Set<String> wishList;

    for (JsonElement customer : jcustomers){
      jcustomer = customer.getAsJsonObject();
      purchaseList = new ArrayList<PurchaseSchedule>();
      purchaseschedule = jcustomer.get("purchaseSchedule").getAsJsonArray();
      for (JsonElement purchase : purchaseschedule){
        jpurchase = purchase.getAsJsonObject();
        pschedule = new PurchaseSchedule(jpurchase.get("shoeType").getAsString(),jpurchase.get("tick").getAsInt());
        purchaseList.add(pschedule);
      }
      jwishList = jcustomer.get("wishList").getAsJsonArray();
      wishList = new LinkedHashSet<String>();
      for (JsonElement wish : jwishList){
        wishList.add(wish.getAsString());
      }
      customers.add(new WebsiteClientService(jcustomer.get("name").getAsString(),purchaseList,wishList,latchForInit, latchForEnding));
    }
  }

  private static void parseInitialStorage(Store store, JsonObject jobject) {
    HashMap<String,Integer> initialStorage = new HashMap<String,Integer>();
    JsonArray shoes = jobject.get("initialStorage").getAsJsonArray();
    String type;
    int amount;
    int j=0;
    ShoeStorageInfo[] initialStorageArray;

    for (JsonElement shoe : shoes){
      type = (shoe.getAsJsonObject().get("shoeType").getAsString());
      amount = (shoe.getAsJsonObject().get("amount").getAsInt());
      initialStorage.put(type, amount);
    }

    initialStorageArray = new ShoeStorageInfo[initialStorage.size()];
    for (String s: initialStorage.keySet()){
      initialStorageArray[j]= new ShoeStorageInfo(s, initialStorage.get(s),0);
      ++j;
    }
    store.load(initialStorageArray);
  }
}