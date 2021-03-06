package nl.pvanassen.steam.store.tradeoffer;

import nl.pvanassen.steam.http.DefaultHandle;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

class TradeOfferHandle extends DefaultHandle {
    private final ObjectMapper om;
    private int tradeOfferId;
    private boolean error = false;
    private String message;
    
    TradeOfferHandle(ObjectMapper om) {
        this.om = om;
    }

    int getTradeOfferId() {
        return tradeOfferId;
    }

    @Override
    public void handle(InputStream stream) throws IOException {
        JsonNode jsonNode = om.readTree(stream);
        tradeOfferId = jsonNode.get("tradeofferid").asInt();
    }
    
    @Override
    public void handleError(InputStream stream) throws IOException {
        error = true;
        JsonNode jsonNode = om.readTree(stream);
        message = jsonNode.get("strError").asText(); 
    }
    
    public String getMessage() {
        return message;
    }
    
    public boolean isError() {
        return error;
    }
}
