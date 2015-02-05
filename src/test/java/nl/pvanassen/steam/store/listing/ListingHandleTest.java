package nl.pvanassen.steam.store.listing;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nl.pvanassen.steam.store.common.Listing;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

public class ListingHandleTest {
    private ListingHandle handle;
    private ListingDeque listingQueue;

    @Before
    public void setUp() {
        listingQueue = new ListingDeque(5000);
        handle = new ListingHandle(new ObjectMapper(), listingQueue, "NL");
    }

    @Test
    public void testHandle() throws IOException {
        handle.handle(getClass().getResourceAsStream("/listing.json"));
        List<Listing> listings = new ArrayList<>(listingQueue.getDeque());
        assertEquals(10, listings.size());
        assertEquals(730, listings.get(9).getAppId());
        assertEquals("eSports%202014%20Summer%20Case", listings.get(9).getUrlName());
        assertEquals(2, listings.get(9).getFee());
        assertEquals(1, listings.get(9).getPublisherFee());
        assertEquals(730, listings.get(9).getPublisherFeeApp());
        assertEquals("443930322874172198", listings.get(9).getListingId());
        assertEquals(1, listings.get(9).getSteamFee());
        assertEquals(4, listings.get(9).getSubTotal());
    }


    @Test
    public void testHandleListingPage() throws IOException {
        handle.handle(getClass().getResourceAsStream("/listing-page.json"));
        List<Listing> listings = new ArrayList<>(listingQueue.getDeque());
        assertEquals(10, listings.size());
        assertEquals(730, listings.get(9).getAppId());
        assertEquals("M4A1-S%20%7C%20Atomic%20Alloy%20%28Minimal%20Wear%29", listings.get(9).getUrlName());
        assertEquals(64, listings.get(9).getFee());
        assertEquals(43, listings.get(9).getPublisherFee());
        assertEquals(730, listings.get(9).getPublisherFeeApp());
        assertEquals("433799027041528860", listings.get(9).getListingId());
        assertEquals(21, listings.get(9).getSteamFee());
        assertEquals(438, listings.get(9).getSubTotal());
    }

    @Test
    public void testHandleNewListingPage() throws IOException {
        handle.handle(getClass().getResourceAsStream("/new-listing.json"));
        List<Listing> listings = new ArrayList<>(listingQueue.getDeque());
        assertEquals(10, listings.size());
        assertEquals(730, listings.get(9).getAppId());
        assertEquals("Nova%20%7C%20Bloomstick%20%28Minimal%20Wear%29", listings.get(9).getUrlName());
        assertEquals(27, listings.get(9).getFee());
        assertEquals(18, listings.get(9).getPublisherFee());
        assertEquals(730, listings.get(9).getPublisherFeeApp());
        assertEquals("433805375084121848", listings.get(9).getListingId());
        assertEquals(9, listings.get(9).getSteamFee());
        assertEquals(186, listings.get(9).getSubTotal());
    }

    public void testPerformance() throws IOException {
        for (int i = 0; i != 5000; i++) {
            handle.handle(getClass().getResourceAsStream("/listing.json"));
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i != 10000; i++) {
            handle.handle(getClass().getResourceAsStream("/listing.json"));
        }
        long time = System.currentTimeMillis() - start;
        System.out.println("Took: " + time);
    }

}
