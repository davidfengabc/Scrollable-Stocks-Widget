package com.paulish.widgets.stocks;

import java.util.*;

import com.paulish.widgets.stocks.receivers.StocksWidgetScrollable;
import com.paulish.widgets.stocks.receivers.StocksWidgetSingle;

import android.content.*;
import android.content.SharedPreferences.Editor;
import android.appwidget.AppWidgetManager;
import android.preference.PreferenceManager;

public class Preferences {
    public static final String PORTFOLIO = "Portfolio";
    public static final String PORTFOLIO_OLD = "Portfolio-%d";
    public static final String CURRENT_INDEX = "CurrentIndex-%d";
    public static final String DATE_DAY_FIRST = "key_date_day_first";
    public static final String HOUR_24 = "key_24_hour";
    // let update interval be common for all the widgets
    public static final String UPDATE_INTERVAL = "UpdateInterval";   
    public static final String DEFAULT_UPDATE_INTERVAL = "15"; // 15 minutes
       
    public static String get(String aPref, int aAppWidgetId) {
    	return String.format(aPref, aAppWidgetId);    	
    }
    
    public static List<String> getPortfolio(Context context, int appWidgetId) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	// first get an old portfolio
    	String commaTickers = prefs.getString(Preferences.get(Preferences.PORTFOLIO_OLD, appWidgetId), context.getString(R.string.defaultPortfolio));
    	// but if the new portfolio exists then use it
		commaTickers = prefs.getString(Preferences.PORTFOLIO, commaTickers);
		return new ArrayList<String>(Arrays.asList(commaTickers.split(",")));
    }
    
    public static List<String> getAllPortfolios(Context context) {
    	ArrayList<String> result = new ArrayList<String>();
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	int[] appWidgetIds = getAllWidgetIds(context);
    	String commaTickers;
    	String[] tickers;
    	// use old way first
    	final String defaultPortfolio = context.getString(R.string.defaultPortfolio);
    	for (int appWidgetId : appWidgetIds) {
    		commaTickers = prefs.getString(Preferences.get(Preferences.PORTFOLIO_OLD, appWidgetId), defaultPortfolio);
    		tickers = commaTickers.split(",");
    		for (String ticker : tickers) {
    			if (!result.contains(ticker))
    			  result.add(ticker);
    		}
    	}
    	// then use the one generic portfolio
		commaTickers = prefs.getString(Preferences.PORTFOLIO, defaultPortfolio);
		tickers = commaTickers.split(",");
		for (String ticker : tickers) {
			if (!result.contains(ticker))
			  result.add(ticker);
		}
    	return result;
    }
    
    public static void setPortfolio(Context context, int appWidgetId, String tickers) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	Editor edit = prefs.edit();
    	edit.putString(Preferences.PORTFOLIO, tickers);
    	edit.commit();
    }
    
    public static int getUpdateInterval(Context context) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	try {
    		int interval = prefs.getInt(Preferences.UPDATE_INTERVAL, -1);
    		if (interval == -1)
    			return Integer.parseInt(prefs.getString(Preferences.UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL));
    		else
    			return interval;
    	} catch (ClassCastException e) {
    		return Integer.parseInt(prefs.getString(Preferences.UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL));
    	}    	        	
    }
    
    public static void setUpdateInterval(Context context, int interval) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	Editor edit = prefs.edit();
    	edit.putString(Preferences.UPDATE_INTERVAL, new Integer(interval).toString());
    	edit.commit();    	
    }
    
    public static int getCurrentIndex(Context context, int appWidgetId) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);    	
		return prefs.getInt(Preferences.get(Preferences.CURRENT_INDEX, appWidgetId), 0);    	
    }
    
    public static void setCurrentIndex(Context context, int appWidgetId, int currentIndex) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);    	
    	Editor edit = prefs.edit();
    	edit.putInt(Preferences.get(Preferences.CURRENT_INDEX, appWidgetId), currentIndex);
    	edit.commit();    	
    }
    
    public static void DropSettings(Context context, int[] appWidgetIds) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor edit = prefs.edit();
		for(int appWidgetId : appWidgetIds) {
			edit.remove(Preferences.get(Preferences.CURRENT_INDEX, appWidgetId));
		}
		edit.commit();
    }
    
    public static int[] getAllWidgetIds(Context context) {
    	AppWidgetManager awm = AppWidgetManager.getInstance(context);
    	List<int[]> result = new ArrayList<int[]>();
    	
    	result.add(awm.getAppWidgetIds(new ComponentName(context, StocksWidgetScrollable.class)));
    	result.add(awm.getAppWidgetIds(new ComponentName(context, StocksWidgetSingle.class)));
    	
    	int i = 0;
    	for(int[] arr : result)
    	  i += arr.length;
    	
    	int[] res = new int[i];
    	i = 0;
    	for (int[] arr : result) {
    		for (int id : arr) {
    			res[i++] = id;
    		}
    	}
    	
    	return res;
    }

	public static String formatFieldDate(Context context, String fieldName) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);    	
		return "strftime('" + (prefs.getBoolean(Preferences.DATE_DAY_FIRST, true)?"%d/%m', ":"%m/%d', ") + fieldName + ")";    	
	}

	public static String formatFieldTime(Context context, String fieldName) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	if (prefs.getBoolean(Preferences.HOUR_24, true))
    		return "strftime('%H:%M', " + fieldName + ")";
    	else
    		return "CASE WHEN CAST(strftime('%H', " + fieldName + 
    		  ") as INTEGER) >= 12 THEN '' || (CAST(strftime('%H', " + fieldName + 
    		  ") as INTEGER) - 12) || strftime(':%M pm', " + fieldName + 
    		  ") ELSE '' || CAST(strftime('%H', " + fieldName + ") as INTEGER) || strftime(':%M am', " + fieldName + ") END";    	
	}
}
