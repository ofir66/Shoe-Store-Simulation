package bgu.spl.app;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bgu.spl.app.passiveObjects.ShoeStorageInfo;
import bgu.spl.app.passiveObjects.Store;
import bgu.spl.app.passiveObjects.Store.BuyResult;


 
public class StoreTest {
  private Store store;
  private ShoeStorageInfo [] shoes={new ShoeStorageInfo("brooks adrenalin", 0, 0),
                                    new ShoeStorageInfo("brooks ghost", 1, 0),
                                    new ShoeStorageInfo("brooks glycerin", 5, 1), 
                                    new ShoeStorageInfo("assics nimbus", 4, 3),
                                    new ShoeStorageInfo("assics palo", 5, 3)};


  @Before
  public void setUp() throws Exception {
    this.store= Store.getInstance();        
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testGetInstance() {
    assertEquals(this.store,Store.getInstance());
  }

  @Test
  public void testLoad() {
    String takeResult;

    store.load(shoes);

    takeResult= this.store.take("brooks adrenalin", false).name();
    assertEquals("NOT_IN_STOCK",takeResult);

    takeResult=this.store.take("brooks ghost", false).name();
    assertEquals("REGULAR_PRICE",takeResult);

    takeResult=this.store.take("brooks glycerin", false).name();
    assertEquals("DISCOUNTED_PRICE",takeResult);

    takeResult=this.store.take("assics nimbus", false).name();
    assertEquals("DISCOUNTED_PRICE",takeResult);		
  }

  @Test
  public void testTake() {
    store.load(shoes);

    assertEquals(BuyResult.NOT_ON_DISCOUNT,store.take("brooks adrenalin", true));
    assertEquals(BuyResult.NOT_IN_STOCK,store.take("brooks adrenalin", false));
    assertEquals(BuyResult.NOT_ON_DISCOUNT,store.take("brooks ghost", true));
    assertEquals(BuyResult.REGULAR_PRICE,store.take("brooks ghost", false));
    assertEquals(0,this.shoes[1].getAmountOnStorage());
    assertEquals(BuyResult.DISCOUNTED_PRICE,store.take("brooks glycerin", true));
    assertEquals(0,this.shoes[2].getDiscountedAmount());
    assertEquals(BuyResult.DISCOUNTED_PRICE,store.take("assics nimbus", false));
    assertEquals(3,this.shoes[3].getAmountOnStorage());
  }

  @Test
  public void testAdd() {
    store.load(shoes);

    store.add("brooks adrenalin", 7);
    assertEquals(7, this.shoes[0].getAmountOnStorage());
    store.add("brooks ghost", 0);
    assertEquals(1, this.shoes[1].getAmountOnStorage());
    store.add("brooks glycerin", 1);
    assertEquals(6, this.shoes[2].getAmountOnStorage());
  }

  @Test
  public void testAddDiscount() {
    store.load(shoes);

    store.addDiscount("brooks adrenalin", 3);
    assertEquals(0, this.shoes[0].getDiscountedAmount());

    store.add("brooks adrenalin", 7);
    store.addDiscount("brooks adrenalin", 3);
    assertEquals(3, this.shoes[0].getDiscountedAmount());

    store.addDiscount("brooks glycerin", 20);
    assertEquals(5, this.shoes[2].getDiscountedAmount());

    store.addDiscount("assics nimbus", 1);
    assertEquals(4, this.shoes[3].getDiscountedAmount());

    store.addDiscount("assics palo", 3);
    assertEquals(5, this.shoes[4].getDiscountedAmount());
  }
 
}