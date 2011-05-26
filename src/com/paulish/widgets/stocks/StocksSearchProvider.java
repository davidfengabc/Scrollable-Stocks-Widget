package com.paulish.widgets.stocks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.paulish.internet.RequestMethod;
import com.paulish.internet.RestClient;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class StocksSearchProvider extends ContentProvider {

	private static final String SEARCH_URL = "http://d.yimg.com/autoc.finance.yahoo.com/autoc";
	
    public static final Pattern PATTERN_RESPONSE = Pattern.compile(
            "YAHOO\\.Finance\\.SymbolSuggest\\.ssCallback\\((\\{.*?\\})\\)"
    );
	
	
	public static final String AUTHORITY = "com.paulish.widgets.stocks.searchprovider";	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    private static final int SEARCH_SUGGEST = 0;
    private static final int SHORTCUT_REFRESH = 1;
	
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT, SHORTCUT_REFRESH);
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", SHORTCUT_REFRESH);
	}
	
    private static final String[] COLUMNS = {
        "_id",
        SearchManager.SUGGEST_COLUMN_TEXT_1,
        SearchManager.SUGGEST_COLUMN_TEXT_2,
        SearchManager.SUGGEST_COLUMN_INTENT_DATA
    };

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
        throw new UnsupportedOperationException();
	}

	@Override
	public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
        case SEARCH_SUGGEST:
            return SearchManager.SUGGEST_MIME_TYPE;
        case SHORTCUT_REFRESH:
            return SearchManager.SHORTCUT_MIME_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }
	}

	@Override
	public Uri insert(Uri arg0, ContentValues arg1) {
        throw new UnsupportedOperationException();
	}

	@Override
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (URI_MATCHER.match(uri)) {
        case SEARCH_SUGGEST:
            String query = null;
            if (uri.getPathSegments().size() > 1) {
                query = uri.getLastPathSegment().toLowerCase();
            }
            return getSuggestions(query, projection);
        case SHORTCUT_REFRESH:
            String shortcutId = null;
            if (uri.getPathSegments().size() > 1) {
                shortcutId = uri.getLastPathSegment();
            }
            return refreshShortcut(shortcutId, projection);
        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }
	}
	
    private Cursor getSuggestions(String query, String[] projection) {    	
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        
        if (query != null) {
            final RestClient client = new RestClient(SEARCH_URL);
            client.AddParam("query", query);
            client.AddParam("callback", "YAHOO.Finance.SymbolSuggest.ssCallback");
             
            try {
                client.Execute(RequestMethod.GET);
            } catch (Exception e) {
                e.printStackTrace();
            }
             
            String response = client.getResponse();
            if (response == null)
            	return cursor;
            
            Matcher m = PATTERN_RESPONSE.matcher(response);
            if (m.find()) {
            	response = m.group(1);
            	try {
            		JSONArray ja = new JSONObject(response).getJSONObject("ResultSet").getJSONArray("Result");
                    for (int id = 0; id < ja.length(); id++) {
                    	JSONObject jo = ja.getJSONObject(id);
                    	String symbol = jo.getString("symbol");
                    	cursor.addRow(new Object[] {id, symbol, jo.getString("name"), symbol});
                    }
            		
            	} catch (JSONException e) {
        			e.printStackTrace();            		
            	}
            }
        }

        return cursor;
    }
    
    private Cursor refreshShortcut(String shortcutId, String[] projection) {
        return null;
    }    	

}
