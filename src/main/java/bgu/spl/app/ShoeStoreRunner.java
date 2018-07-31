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
 * Our main class of the program who runs the whole simulated shoe store we created using the 
 * {@link bgu.spl.mics.impl.MessageBusImpl MessageBus} and different 
 * {@link bgu.spl.mics.MicroService MicroServices}.
 * <p>
 * When started, it will recieve as an argument (command line argument) the name of the json input file to 
 * read. The ShoeStoreRunner would read the input file (using {@link com.google.gson.Gson Gson} Library),
 * then add the initial storage to the store and create and start the micro-services. 
 * <p>
 * When the current tick number is larger than the duration given to the TimeService in the input file all 
 * the micro-services would gracefully terminate themselves.
 * <p>
 * After all the micro-services terminate themselves the ShoeStoreRunner will call the Store’s
 * print function and exit.
 * 
 * @author Ofir Hauzer & Liraz Reichenstein
 *
 */
public class ShoeStoreRunner {
 
	/**
	 * Logger - a logger for printing commands
	 */
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void main(String[] args) {
    	(new File("Log")).mkdir(); // Creates a Log folder if doesn't exist
    	for(File file: new File("Log").listFiles()) file.delete(); // cleans the Log folder so we will only have the latest log files after the program runs
    	System.setProperty("java.util.logging.SimpleFormatter.format","%4$s: %5$s [%1$tc]%n"); // reorders the log lines to make it easier on the eye
    	try {
			FileHandler handler = new FileHandler("Log/ShoeStoreRunner.txt");
			handler.setFormatter(new SimpleFormatter());
			LOGGER.addHandler(handler);
		} catch (SecurityException e1) {
			LOGGER.severe(e1.getLocalizedMessage());
		} catch (IOException e1) {
			LOGGER.severe(e1.getLocalizedMessage());
		}
    	JsonReader jreader=null;
		try {
			jreader = new JsonReader(new FileReader(args[0]));
		} catch (FileNotFoundException e1) {
			LOGGER.severe("File not found.");
        	LOGGER.severe("SYSTEM IS SHUTTING DOWN!");
            System.exit(1);
		}
        HashMap<String,Integer> initialStorage = new HashMap<String,Integer>();
        JsonParser jparser = new JsonParser();
        JsonElement element = jparser.parse(jreader);
        int numOfServices=0;//counts the manager, number of factories, number of sellers and number of customers
        int numOfCustomers=0;
        Store store = Store.getInstance();
        if (element.isJsonObject()){
            JsonObject jobject = element.getAsJsonObject();
            JsonArray shoes = jobject.get("initialStorage").getAsJsonArray();
            for (JsonElement shoe : shoes){ // reading the initial storage and creating a map of shoes
                String type = (shoe.getAsJsonObject().get("shoeType").getAsString());
                int amount = (shoe.getAsJsonObject().get("amount").getAsInt());
                initialStorage.put(type, amount);
            }
            ShoeStorageInfo[] initialStorageArray = new ShoeStorageInfo[initialStorage.size()];
            int j=0;
            for (String s: initialStorage.keySet()){
                initialStorageArray[j]= new ShoeStorageInfo(s, initialStorage.get(s),0);
                ++j;
            }
            store.load(initialStorageArray);
            JsonObject services = jobject.get("services").getAsJsonObject(); // from now on we read from services!!!
            int speed = 0;
            int duration = 0;
            if (services.get("time")==null)
                LOGGER.severe("THERE IS NO TIME!");
            else{
                JsonObject time = services.get("time").getAsJsonObject();
                speed = time.get("speed").getAsInt();
                duration = time.get("duration").getAsInt();
            }
            List<DiscountSchedule> dischedule = new ArrayList<DiscountSchedule>();
            if (services.get("manager")==null)
                LOGGER.severe("THERE IS NO MANAGER!");
            else{
                numOfServices = numOfServices + 1; //manager
                JsonObject manager = services.get("manager").getAsJsonObject();
                JsonArray discounts = manager.get("discountSchedule").getAsJsonArray();
                for (JsonElement discount : discounts){
                    JsonObject jdiscount = discount.getAsJsonObject();
                    dischedule.add(new DiscountSchedule(jdiscount.get("shoeType").getAsString(),jdiscount.get("tick").getAsInt(),jdiscount.get("amount").getAsInt()));
                }
            }
            int factories = services.get("factories").getAsInt();
            numOfServices = numOfServices + factories;
            int sellers = services.get("sellers").getAsInt();
            numOfServices = numOfServices + sellers;
            List<WebsiteClientService> customers = new ArrayList<WebsiteClientService>();
            JsonArray jcustomers = services.get("customers").getAsJsonArray();
            numOfCustomers = jcustomers.size();
            numOfServices = numOfServices+numOfCustomers;
            CountDownLatch latchForInit = new CountDownLatch(numOfServices);
            CountDownLatch latchForEnding = new CountDownLatch(numOfServices+1); // * we assume that we have timer
            for (JsonElement customer : jcustomers){
                JsonObject jcustomer = customer.getAsJsonObject();
                List<PurchaseSchedule> purchaseList = new ArrayList<PurchaseSchedule>();
                JsonArray purchaseschedule = jcustomer.get("purchaseSchedule").getAsJsonArray();
                for (JsonElement purchase : purchaseschedule){
                    JsonObject jpurchase = purchase.getAsJsonObject();
                    PurchaseSchedule pschedule = new PurchaseSchedule(jpurchase.get("shoeType").getAsString(),jpurchase.get("tick").getAsInt());
                    purchaseList.add(pschedule);
                }
                JsonArray jwishList = jcustomer.get("wishList").getAsJsonArray();
                Set<String> wishList = new LinkedHashSet<String>();
                for (JsonElement wish : jwishList){
                    wishList.add(wish.getAsString());
                }
                customers.add(new WebsiteClientService(jcustomer.get("name").getAsString(),purchaseList,wishList,latchForInit, latchForEnding));
            }
            MicroService timer= new TimeService(speed,duration,latchForInit, latchForEnding);
            MicroService manager= new ManagementService(dischedule,latchForInit, latchForEnding);
            List<MicroService> listOfSellers = new ArrayList<MicroService>();
            for (int i = 0; i < sellers; i++){
                listOfSellers.add(new SellingService("Seller "+ (i+1), i+1, latchForInit, latchForEnding));
            }
            Thread timerT = new Thread(timer);
            timerT.start();
            Thread managerT= new Thread(manager);
            for (int i = 0; i < customers.size(); i++){
                Thread customerT= new Thread(customers.get(i));
                customerT.start();
            }
            for (int i = 0; i < listOfSellers.size(); i++){
                Thread sellerT= new Thread(listOfSellers.get(i));
                sellerT.start();
            }
            managerT.start();
            for (int i = 0; i < factories; i++){
                Thread factory = new Thread(new ShoeFactoryService("Factory "+ (i+1),latchForInit, latchForEnding));
                factory.start();
            }
            
            try{
            	latchForEnding.await();
            }
            catch(InterruptedException e){
            	e.printStackTrace();
            }
            store.print();
        }
    }
}