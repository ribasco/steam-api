/**
 * 
 */
package nl.pvanassen.steam.store.sell;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import nl.pvanassen.steam.error.SteamException;
import nl.pvanassen.steam.http.Http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Paul van Assen
 *
 */
public class SteamSellService implements SellService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Http http;
    private final String username;
    
    public SteamSellService(String cookies, String username) {
        this(Http.getInstance(cookies, username), username);
    }

    /**
     * @param http For mocking
     */
    public SteamSellService(Http http, String username) {
        this.http = http;
        this.username = username;
    }
    
    @Override
    public boolean sell(String assetId, int appId, String urlName, int contextId, int price) {
        try {
        	if (price < 1) {
        		throw new SteamException("Error, price is too low: " + price);
        	}
            Map<String, String> params = new HashMap<>();
            params.put("amount", "1");
            params.put("appid", Integer.toString(appId));
            params.put("assetid", assetId);
            params.put("contextid", Integer.toString(contextId));
            params.put("price", Integer.toString(price));
            logger.info(params.toString());
            SellHandle sellHandle = new SellHandle();
            http.post("https://steamcommunity.com/market/sellitem/", params, sellHandle, "http://steamcommunity.com/id/" + username + "/inventory/");
            return !sellHandle.isError();
        }
        catch (IOException | RuntimeException e) {
            logger.error("Error posting data", e);
            return false;
        }
    }

}