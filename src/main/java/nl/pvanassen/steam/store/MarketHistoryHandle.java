package nl.pvanassen.steam.store;

import java.io.*;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import nl.pvanassen.steam.http.DefaultHandle;

import org.apache.html.dom.HTMLDocumentImpl;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLDocument;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class MarketHistoryHandle extends DefaultHandle {
    private final Logger logger = LoggerFactory.getLogger( getClass() );
    private final List<MarketHistory> marketHistory = new LinkedList<>();
    private final List<MarketHistoryPart> marketHistoryParts = new LinkedList<>();
    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();
    private static final XPath XPATH = XPATH_FACTORY.newXPath();
    private static final XPathExpression HISTORY_ROW_XPATH;
    private static final XPathExpression GAIN_LOSS_XPATH;
    private static final XPathExpression DATE_XPATH;
    private static final XPathExpression PRICE_XPATH;
    private static final XPathExpression BUYER_XPATH;
    private static final XPathExpression ACTED_XPATH;
    private int totalCount;
    private boolean error = false;

    static {
        XPathExpression historyRowXpath = null;
        XPathExpression gainLossXpath = null;
        XPathExpression dateXpath = null;
        XPathExpression priceXpath = null;
        XPathExpression buyerXpath = null;
        XPathExpression actedXpath = null;
        try {
            historyRowXpath = XPATH.compile( "//DIV[@class='market_listing_row market_recent_listing_row']" );
            gainLossXpath = XPATH.compile( ".//DIV[@class='market_listing_left_cell market_listing_gainorloss']" );
            dateXpath = XPATH.compile( ".//DIV[@class='market_listing_right_cell market_listing_listed_date']" );
            priceXpath = XPATH.compile( ".//SPAN[@class='market_listing_price']" );
            buyerXpath = XPATH.compile( ".//DIV[@class='market_listing_whoactedwith_name_block']" );
            actedXpath = XPATH.compile( ".//DIV[@class='market_listing_right_cell market_listing_whoactedwith']" );
        }
        catch ( XPathExpressionException e ) {
            LoggerFactory.getLogger( MarketHistory.class ).error( "Error instantiating XPATH", e );
        }
        HISTORY_ROW_XPATH = historyRowXpath;
        GAIN_LOSS_XPATH = gainLossXpath;
        DATE_XPATH = dateXpath;
        PRICE_XPATH = priceXpath;
        BUYER_XPATH = buyerXpath;
        ACTED_XPATH = actedXpath;
    }

    MarketHistoryHandle() {
        super();
    }

    private static class Asset {
    	private final int appId;
    	private final int contextId;
    	private final String urlName;
    	Asset(int appId, int contextId, String urlName) {
			this.appId = appId;
			this.contextId = contextId;
			this.urlName = urlName;
		}
    }
    @Override
    public void handle( InputStream stream ) throws IOException {
        error = false;
        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.readTree( stream );
        totalCount = node.get("total_count").asInt();
        String resultHtml = node.get( "results_html" ).asText();
        if (resultHtml.contains("market_listing_table_message")) {
        	logger.error("There was an error: " + resultHtml);
        	error = true;
        	return;
        }

        DOMFragmentParser parser = new DOMFragmentParser();
        HTMLDocument document = new HTMLDocumentImpl();
        try {
            SimpleDateFormat formatter = new SimpleDateFormat( "d MMM", Locale.US );

            Map<String, Asset> assetMap = getAssetMap( node );
            Map<String, String> hoverMap = getHovers( node );

            DocumentFragment fragment = document.createDocumentFragment();
            parser.parse( new InputSource( new StringReader( resultHtml ) ), fragment );

            NodeList nodeSet = ( NodeList ) HISTORY_ROW_XPATH.evaluate( fragment, XPathConstants.NODESET );
            for ( int i = 0; i < nodeSet.getLength(); i++ ) {
                Node historyRow = nodeSet.item( i );
                String rowName = historyRow.getAttributes().getNamedItem( "id" ).getTextContent();
                Node gainLoss = ( Node ) GAIN_LOSS_XPATH.evaluate( historyRow, XPathConstants.NODE );
                String gainLossText = gainLoss.getTextContent().trim();
                MarketHistoryStatus status = null;
                if ("-".equals(gainLossText)) {
                	status = MarketHistoryStatus.SOLD;
                }
                else if ("+".equals(gainLossText)) {
                    status = MarketHistoryStatus.BOUGHT;
                }
                else {
                	Node actedNode = (Node)ACTED_XPATH.evaluate(historyRow, XPathConstants.NODE);
                	String acted = actedNode.getTextContent().trim();
                	if ("Listing created".equals(acted)) {
                		status = MarketHistoryStatus.CREATED;
                	}
                	else if ("Listing canceled".equals(acted)) {
                		status = MarketHistoryStatus.REMOVED;
                	}
                }
                NodeList dates = ( NodeList ) DATE_XPATH.evaluate( historyRow, XPathConstants.NODESET );
                String listedStr = dates.item( 0 ).getTextContent().trim();
                String actedStr = dates.item( 0 ).getTextContent().trim();
                String priceStr = ( ( Node ) PRICE_XPATH.evaluate( historyRow, XPathConstants.NODE ) ).getTextContent().trim();
                String buyer = "";
                if (status == MarketHistoryStatus.BOUGHT || status == MarketHistoryStatus.SOLD) {
                	buyer = ( ( Node ) BUYER_XPATH.evaluate( historyRow, XPathConstants.NODE ) ).getTextContent().replace( "Buyer:", "" ).trim();
                }
                int eventIdx = rowName.indexOf( "_event" );
                if (eventIdx > -1) {
                    rowName = rowName.substring( 0, eventIdx );
                }
                Asset asset = assetMap.get(hoverMap.get(rowName));
                try {
                    Date acted = formatter.parse( actedStr );
                    Date listed = formatter.parse( listedStr );
                    // 0,10&#8364;
                    int price = 0;
                    if (!"".equals(priceStr)) {
                    	price = Integer.parseInt( priceStr.replace( ",", "" ).replace( "€", "" ).replace( "--", "00" ).trim() );
                    }
                    if (asset == null) {
                        // Listing created or sold
                        continue;
                    }
                    else {
                        marketHistory.add( new MarketHistory( rowName, asset.appId, asset.contextId, asset.urlName, listed, acted, price, buyer, status ) );
                    }
                }
                catch ( ParseException e ) {
                    logger.error( "Error parsing date", e );
                }
            }
        }
        catch ( SAXException | XPathExpressionException e ) {
            logger.error( "Error parsing html", e );
        }
    }

    private Map<String, Asset> getAssetMap( JsonNode node ) throws UnsupportedEncodingException {
        Map<String,Asset> assetMap = new HashMap<>();
        JsonNode assets = node.get( "assets" );
        for ( JsonNode appId : assets ) {
            for ( JsonNode contextId : appId ) {
                for ( JsonNode item : contextId ) {
                    String urlName = URLEncoder.encode( item.get( "market_hash_name" ).asText(), "UTF-8" ).replace( "+", "%20" );
                	assetMap.put( item.get( "id" ).asText(), new Asset(item.get("appid").asInt(), item.get("contextid").asInt(), urlName));
                }
            }
        }
        return assetMap;
    }

    private Map<String, String> getHovers( JsonNode node) {
        Map<String, String> hoverMap = new HashMap<>();
        String hovers = node.get( "hovers" ).asText();
        for ( String hoverStr : hovers.split( ";" ) ) {
            String hover = hoverStr.trim();
            if ( hover.startsWith( "CreateItemHoverFromContainer" ) ) {
                // CreateItemHoverFromContainer( g_rgAssets, 'history_row_2853334817499652196_2853334817499652205_name', 753, '6', '619156810', 0 );
                String[] items = hover.replaceAll( "'", "" ).split( "," );
                String id = items[ 4 ].trim();
                String rowName = items[1].trim();
                hoverMap.put( rowName, id );
                int cutoffIdx = rowName.indexOf( '_',  40);
                hoverMap.put( rowName.substring( 0, cutoffIdx ), id );
            }
        }
        return hoverMap;
    }

    List<MarketHistory> getMarketHistory() {
        return marketHistory;
    }
    
    int getTotalCount() {
        return totalCount;
    }
    
    boolean isError() {
        return error;
    }
}
