package com.paulish.widgets.stocks.receivers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;

import com.paulish.widgets.stocks.Preferences;
import com.paulish.widgets.stocks.QuoteViewActivity;
import com.paulish.widgets.stocks.R;
import com.paulish.widgets.stocks.StocksProvider;
import com.paulish.widgets.stocks.StocksWidget;

public class StocksWidgetSingle extends StocksWidget {

	// Actions
	public static final String ACTION_SHOW_NEXT = "com.paulish.widgets.stocks.action.SHOW_NEXT";
	public static final String ACTION_SHOW_PREV = "com.paulish.widgets.stocks.action.SHOW_PREV";
	
	@Override
	protected void updateWidget(Context context, int appWidgetId, Boolean loading) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stocks_widget_single);
        
        updateWidgetData(context, appWidgetId, loading, views);
        
        Intent intent = new Intent(context, StocksWidgetSingle.class);
        intent.setAction(ACTION_SHOW_NEXT);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setOnClickPendingIntent(R.id.stateImage, 
        		PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        
        final AppWidgetManager awm = AppWidgetManager.getInstance(context);        
        awm.updateAppWidget(appWidgetId, views);		
	}
		
	private void updateWidgetData(Context context, int appWidgetId, Boolean loading, RemoteViews views) {
		if (loading)
			return;
				
		// query the data
		Uri quotes = StocksProvider.CONTENT_URI_WIDGET_QUOTES.buildUpon().appendEncodedPath(
				Integer.toString(appWidgetId)).build();
		
		final ContentResolver resolver = context.getContentResolver();
		Cursor cur = resolver.query(quotes, StocksProvider.PROJECTION_QUOTES, null, null, null);
		
		int currentIndex = Preferences.getCurrentIndex(context, appWidgetId);
		
		if (cur != null) {
			final int lastIndex = cur.getCount() - 1;
			if (currentIndex > lastIndex) {
				currentIndex = 0;
				Preferences.setCurrentIndex(context, appWidgetId, currentIndex);
			}
			if (currentIndex == -1) {
				currentIndex = lastIndex;
				Preferences.setCurrentIndex(context, appWidgetId, currentIndex);
			}
		}
		
		if ((cur != null) && (cur.moveToPosition(currentIndex))) {
			final String symbol = cur.getString(StocksProvider.QuotesColumns.symbol.ordinal());
			
			views.setTextViewText(R.id.quoteSymbol, symbol);
			views.setTextViewText(R.id.quotePrice, cur.getString(StocksProvider.QuotesColumns.price.ordinal()));
			views.setTextViewText(R.id.quoteChangePercent, cur.getString(StocksProvider.QuotesColumns.pchange.ordinal()));
			views.setTextViewText(R.id.quoteChange, cur.getString(StocksProvider.QuotesColumns.change.ordinal()));
			switch (cur.getInt(StocksProvider.QuotesColumns.stateimage.ordinal())) {
			case R.drawable.stocks_widget_state_red:
				views.setImageViewResource(R.id.stateImage, R.drawable.stocks_widget_arrow_negative);
				break;
			case R.drawable.stocks_widget_state_green:
				views.setImageViewResource(R.id.stateImage, R.drawable.stocks_widget_arrow_positive);
				break;
			default:
				views.setImageViewResource(R.id.stateImage, R.drawable.stocks_widget_arrow_zero);
				break;
			}
			
			Intent openForSymbolIntent = QuoteViewActivity.getOpenForSymbolIntent(context, symbol);
	        views.setOnClickPendingIntent(R.id.widgetLayout, 
	        		PendingIntent.getActivity(context, appWidgetId, openForSymbolIntent, PendingIntent.FLAG_UPDATE_CURRENT));
			
		} else {
			// fill with default values 
			views.setTextViewText(R.id.quoteSymbol, "");
			views.setTextViewText(R.id.quotePrice, "0");
			views.setTextViewText(R.id.quoteChangePercent, "0.0%");
			views.setTextViewText(R.id.quoteChange, "0.0");
			views.setImageViewResource(R.id.stateImage, R.drawable.stocks_widget_arrow_zero);			
		}
		
		if (cur != null)
			cur.close();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if  (ACTION_SHOW_NEXT.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,	AppWidgetManager.INVALID_APPWIDGET_ID);
			if ((appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) && appWidgetClassMatch(context, appWidgetId)) {
				changeCurrent(context, appWidgetId, 1);
			}			
		} else if  (ACTION_SHOW_PREV.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,	AppWidgetManager.INVALID_APPWIDGET_ID);
			if ((appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) && appWidgetClassMatch(context, appWidgetId)) {
				changeCurrent(context, appWidgetId, -1);
			}			
		} else
		super.onReceive(context, intent);
	}
	
	private void changeCurrent(Context context, int appWidgetId, int delta) {
		final int currentIndex = Preferences.getCurrentIndex(context, appWidgetId);
		Preferences.setCurrentIndex(context, appWidgetId, currentIndex + delta);
		updateWidget(context, appWidgetId, false);
	}

}
