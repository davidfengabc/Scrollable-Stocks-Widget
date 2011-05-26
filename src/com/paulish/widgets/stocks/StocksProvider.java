package com.paulish.widgets.stocks;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.*;
import com.paulish.internet.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.net.Uri;
import android.os.AsyncTask;

public class StocksProvider extends ContentProvider {
	public static final String TAG = "paulish.StocksProvider";
	
	private static class DatabaseHelper extends SQLiteOpenHelper {		
		public static final String DATABASE_NAME = "stocks.db";
		public static final int DATABASE_VERSION = 2;

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);			
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL( "CREATE TABLE IF NOT EXISTS quotes (symbol TEXT PRIMARY KEY ON CONFLICT REPLACE, name TEXT, price TEXT, price_date DATE, change DOUBLE, pchange TEXT);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS quotes");
			onCreate(db);
		}
	}
	
	private static class YahooUpdateTask extends AsyncTask<Object, Void, Void> {
		private Context ctx = null;
		private Integer[] appWidgetIds = null;
		
		@Override
		protected Void doInBackground(Object... args) {
			ctx = (Context)args[0]; 
			Integer appWidgetId = (Integer)args[1];
			if (appWidgetId == null) {
				final int[] tmpWidgetIds = Preferences.getAllWidgetIds(ctx);
				appWidgetIds = new Integer[tmpWidgetIds.length];
				for (int i = 0; i < tmpWidgetIds.length; i++)
					appWidgetIds[i] = tmpWidgetIds[i];
			}
			else {
				appWidgetIds = new Integer[1];
				appWidgetIds[0] = appWidgetId.intValue();
			}
			StocksWidget.setLoading(ctx, appWidgetIds, true);
		    StocksProvider.loadFromYahoo(ctx);
		    return null;
		}
				
		protected void onPostExecute(Void result) {
	        StocksWidget.setLoading(ctx, appWidgetIds, false);
	     }		
	}

	public static final String AUTHORITY = "com.paulish.widgets.stocks.provider";	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	public static final Uri CONTENT_URI_QUOTES = CONTENT_URI.buildUpon().appendEncodedPath("quotes").build();
	public static final Uri CONTENT_URI_WIDGET_QUOTES = CONTENT_URI.buildUpon().appendEncodedPath("widget_quotes").build();
	
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	private static final int URI_QUOTES = 0;
	private static final int URI_QUOTE = 1;
	private static final int URI_WIDGET_QUOTES = 2;
		
	public static final String QUOTES_TABLE_NAME = "quotes";
	
	public enum QuotesColumns {
		symbol, name, price, price_date, change, pchange, stateimage
	}

	public static final String[] PROJECTION_QUOTES = new String[] {
		QuotesColumns.symbol.toString(),
		QuotesColumns.name.toString(), 
		QuotesColumns.price.toString(),
		QuotesColumns.price_date.toString(),
		"CASE WHEN change is NULL THEN \"\" WHEN change > 0 THEN \"+\" || change ELSE \"\" || change END as " + QuotesColumns.change.toString(),
		"CASE WHEN pchange is NULL THEN \"\" ELSE pchange END as " + QuotesColumns.pchange.toString(),
		"CASE WHEN change IS NULL THEN " + Integer.toString(R.drawable.stocks_widget_state_gray) + 
		    " WHEN change = 0 THEN " + Integer.toString(R.drawable.stocks_widget_state_gray) + 
		    " WHEN change < 0 THEN " + Integer.toString(R.drawable.stocks_widget_state_red) + 
		    " ELSE " + Integer.toString(R.drawable.stocks_widget_state_green) + " END as " + QuotesColumns.stateimage.toString()
	};

	private Context ctx = null;
	private SQLiteDatabase stocksDB = null;

	static {
		URI_MATCHER.addURI(AUTHORITY, "quotes", URI_QUOTES);
		URI_MATCHER.addURI(AUTHORITY, "quotes/#", URI_QUOTE);
		URI_MATCHER.addURI(AUTHORITY, "widget_quotes/#", URI_WIDGET_QUOTES);
	}

	@Override
	public boolean onCreate() {
		ctx = getContext();
		DatabaseHelper dbHelper = new DatabaseHelper(ctx);
		stocksDB = dbHelper.getReadableDatabase();
		
	    return (stocksDB == null)? false:true;
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (stocksDB != null)
			stocksDB.close();
		super.finalize();
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// Log.d(TAG, "start loading data");
        
		final SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();

        // Set the table we're querying.
        qBuilder.setTables(QUOTES_TABLE_NAME);
        
        // replace price_date column with the required format
        final String priceDateStr = QuotesColumns.price_date.toString();
        for (int i = 0; i < projection.length; i++) {
        	if (priceDateStr.equals(projection[i]))
        		projection[i] = Preferences.formatFieldDate(ctx, priceDateStr) + "|| ', ' || " + Preferences.formatFieldTime(ctx, priceDateStr) + " as " + priceDateStr;         		        		
        }
        
        // If the query ends in a specific record number, we're
        // being asked for a specific record, so set the
        // WHERE clause in our query.
		if ((URI_MATCHER.match(uri)) == URI_WIDGET_QUOTES) {
			final List<String> pathSegs = uri.getPathSegments();
			final int appWId = Integer.parseInt(pathSegs.get(pathSegs.size() - 1));
			final List<String> tickers = Preferences.getPortfolio(ctx, appWId);
			qBuilder.appendWhere("symbol in (" + prepareTickers(tickers) + ")");
			sortOrder = buildSortOrder(tickers);
		} else if ((URI_MATCHER.match(uri)) == URI_QUOTE) {
			final List<String> pathSegs = uri.getPathSegments();
			final String quote = pathSegs.get(pathSegs.size() - 1);
			qBuilder.appendWhere("symbol = \"" + quote.toUpperCase() + "\"");
		}        	
        
        // Log.d(TAG, "sort order = " + sortOrder);
        
        // Make the query.
        Cursor c = qBuilder.query(stocksDB,
                projection,
                selection,
                selectionArgs,
                "",
                "",
                sortOrder);        
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}
	
	public static void notifyDatabaseModification(Context ctx, int widgetId) {		
		final Uri widgetUri = CONTENT_URI_WIDGET_QUOTES.buildUpon().appendEncodedPath(Integer.toString(widgetId)).build();
		ctx.getContentResolver().notifyChange(widgetUri, null);
	}
	
	public static void notifyAllWidgetsModification(Context ctx) {
		final int[] appWidgetIds = Preferences.getAllWidgetIds(ctx);
		for (int appWidgetId : appWidgetIds) {
			notifyDatabaseModification(ctx, appWidgetId);
		}
	}
	
	private static String prepareTickers(List<String> tickers) {
		final StringBuffer result = new StringBuffer();
		final int size = tickers.size(); 
	    if (size > 0) {
	        for (String ticker : tickers) {
		    	result.append("\"");
		    	ticker = ticker.toUpperCase();
		    	if (ticker.equals("^DJI")) {
		            result.append("INDU\",\"");		    		
		    	}
	            result.append(ticker);
		    	result.append("\"");
		    	result.append(",");
	        }
	        // remove the final ","
	        result.setLength(result.length() - 1);
	    }
	    return result.toString();
	}
	
	private static String buildSortOrder(List<String> tickers) {
		final StringBuffer result = new StringBuffer();
		final int size = tickers.size(); 
	    if (size > 1) {
	    	result.append("CASE symbol");
	    	for (int i = 0; i < size; i++) {
	    		result.append(" WHEN \"");
	    		result.append(tickers.get(i).toUpperCase());
	    		result.append("\" THEN ");
	    		result.append(Integer.toString(i));
	    	}
	    	result.append(" END");
	    }		
		return result.toString();
	}
	
	public static void loadFromYahoo(Context ctx) {
		final List<String> tickers = Preferences.getAllPortfolios(ctx);
		loadFromYahoo(ctx, tickers);
	    notifyAllWidgetsModification(ctx);
	}
	
	public static void loadFromYahooInBackgroud(Context ctx, Integer appWidgetId) {
		final YahooUpdateTask yahooUpdateTask = new YahooUpdateTask();
        yahooUpdateTask.execute(ctx, appWidgetId); 
	}
	
	// helper for loadFromYahoo
	private static void setValuesFromJSONObject(ContentValues values, JSONObject jo) {
		values.clear();
		try {
			values.put(QuotesColumns.symbol.toString(), jo.getString("Symbol"));
			if (!jo.isNull("Name"))
				values.put(QuotesColumns.name.toString(), jo.getString("Name"));
			if (!jo.isNull("LastTradePriceOnly")) {
				values.put(QuotesColumns.price.toString(), jo.getString("LastTradePriceOnly"));
				// get the date + time in EDT
				final String priceDateStr = jo.getString("LastTradeDate") + " " + jo.getString("LastTradeTime");
				try {
					SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mma");
					dateFormat.setTimeZone(TimeZone.getTimeZone("EST5EDT"));
					Date priceDate = dateFormat.parse(priceDateStr);
					dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					dateFormat.setTimeZone(TimeZone.getDefault());
					values.put(QuotesColumns.price_date.toString(), dateFormat.format(priceDate));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			// percent change is N/A when change is null
			if (!jo.isNull("Change")) {
				values.put(QuotesColumns.change.toString(), jo.getDouble("Change"));
				values.put(QuotesColumns.pchange.toString(), jo.getString("PercentChange"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private static void loadFromYahoo(Context ctx, List<String> tickers) {
		
		final ContentValues values = new ContentValues();
		
		final RestClient client = new RestClient("http://query.yahooapis.com/v1/public/yql");
        client.AddParam("q", "select Symbol, Name, LastTradePriceOnly, Change, PercentChange, LastTradeDate, LastTradeTime from yahoo.finance.quotes where symbol in (" + prepareTickers(tickers) + ")");
        client.AddParam("format", "json");
        client.AddParam("env", "http://datatables.org/alltables.env");
        client.AddParam("callback", "");
         
        try {
            client.Execute(RequestMethod.GET);
        } catch (Exception e) {
            e.printStackTrace();
        }
         
        final String response = client.getResponse();
        if (response == null)
        	return;
        //Log.d(TAG, "... response: " + response);
               
        try {
			SQLiteDatabase stocksDB = new DatabaseHelper(ctx).getWritableDatabase();
			try {
				JSONObject jo = new JSONObject(response).getJSONObject("query").getJSONObject("results");
				// we can get either an array of quotes or just one quote
				final JSONArray ja = jo.optJSONArray("quote");			
				if (ja != null) {
					for (int i = 0; i < ja.length(); i++) {
						jo = ja.getJSONObject(i);
						setValuesFromJSONObject(values, jo);
						stocksDB.insert(QUOTES_TABLE_NAME, null, values);
					}
				} else
				{
					jo = jo.optJSONObject("quote");
					setValuesFromJSONObject(values, jo);
					stocksDB.insert(QUOTES_TABLE_NAME, null, values);
				}
			} finally {
				stocksDB.close();
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}				
	}	
}