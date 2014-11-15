package nl.pvanassen.steam.store.item;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import nl.pvanassen.steam.http.DefaultHandle;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Handler to parse the listing page
 *
 * @author Paul van Assen
 */
public class ListingPageScriptHandle extends DefaultHandle {

    private final ObjectMapper om;
    private JsonNode listingInfo;
    private JsonNode priceHistoryInfo;
    private boolean error;
    private boolean buyOrders;

    ListingPageScriptHandle(ObjectMapper om) {
        this.om = om;
        error = false;
        buyOrders = false;
    }

    JsonNode getListingInfo() {
        return listingInfo;
    }

    JsonNode getPriceHistoryInfo() {
        return priceHistoryInfo;
    }

    @Override
    public void handle(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        boolean listingFound = false;
        boolean salesFound = false;
        boolean buyOrdersFound = false;
        String line;
        while ((line = reader.readLine()) != null) {
            if (!buyOrdersFound) {
                if (line.indexOf("market_commodity_order_block") > -1) {
                    buyOrders = true;
                    buyOrdersFound = true;
                }
            }
            if (!listingFound) {
                int listingStart = line.indexOf("g_rgListingInfo");
                if (listingStart > -1) {
                    int objectStart = line.indexOf('{', listingStart);
                    if (objectStart > -1) {
                        listingInfo = om.readTree(line.substring(objectStart, line.length() - 1));
                        listingFound = true;
                    }
                }
            }
            if (!salesFound) {
                int salesStart = line.indexOf("line1");
                if (salesStart > -1) {
                    int objectStart = line.indexOf('[', salesStart);
                    if (objectStart <= -1) {
                        continue;
                    }
                    priceHistoryInfo = om.readTree(line.substring(objectStart, line.length() - 1));
                    salesFound = true;
                }
            }
        }
    }

    @Override
    public void handleError(InputStream stream) throws IOException {
        error = true;
    }

    boolean isError() {
        return error;
    }
    
    /**
     * @return Does this item support buy orders?
     */
    boolean isBuyOrders() {
        return buyOrders;
    }
}
