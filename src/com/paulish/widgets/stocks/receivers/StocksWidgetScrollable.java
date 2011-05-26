package com.paulish.widgets.stocks.receivers;

import mobi.intuitit.android.content.LauncherIntent;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.paulish.widgets.stocks.*;

public class StocksWidgetScrollable extends StocksWidget{
	
	@Override 
	protected void updateWidget(Context context, int appWidgetId, Boolean loading) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stocks_widget_scrollable);
        
        Intent intent = new Intent(context, UpdateService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        // put appWidgetId here or intent will replace an intent of another widget
        PendingIntent pendingIntent = PendingIntent.getService(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.refresh_button, pendingIntent);
        
        intent = new Intent(context, ConfigurationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.portfolio_edit_button, pendingIntent);

        // don't touch the previous state when loading is not defined
        if (loading != null) {
            if (loading)
            	views.setTextViewText(R.id.refresh_icon, " " + context.getString(R.string.loading));
            else
            	views.setTextViewText(R.id.refresh_icon, "");
        }
       
        final AppWidgetManager awm = AppWidgetManager.getInstance(context);
        awm.updateAppWidget(appWidgetId, views);
	}

	@Override 
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		//Log.d(TAG, "received -> " +  action);
		if (TextUtils.equals(action, LauncherIntent.Action.ACTION_READY)) {
			// Receive ready signal
			// Log.d(TAG, "widget ready");
			onAppWidgetReady(context, intent);			
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_FINISH)) {

		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_ITEM_CLICK)) {
			// onItemClickListener
			onClick(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_VIEW_CLICK)) {
			// onClickListener
			onClick(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Error.ERROR_SCROLL_CURSOR)) {
			// An error occurred
		    Log.d(TAG, intent.getStringExtra(LauncherIntent.Extra.EXTRA_ERROR_MESSAGE));
		} else
			super.onReceive(context, intent);
	}
	
	/**
	 * On click of a child view in an item
	 */
	private void onClick(Context context, Intent intent) {
		// open quote view activity
		QuoteViewActivity.openForSymbol(context, intent.getStringExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS));
	}
	
	/**
	 * Receive ready intent from Launcher, prepare scroll view resources
	 */
	public void onAppWidgetReady(Context context, Intent intent) {
		if (intent == null)
			return;

		int appWidgetId = intent.getExtras().getInt(LauncherIntent.Extra.EXTRA_APPWIDGET_ID, -1);

		if (appWidgetId < 0)
			return;
		
		updateWidget(context, appWidgetId, null);
		final Intent replaceDummy = CreateMakeScrollableIntent(context, appWidgetId);
		// Send it out
		context.sendBroadcast(replaceDummy);				
	}
	
	/**
	 * Constructs a Intent that tells the launcher to replace the dummy with the ListView
	 */
	public Intent CreateMakeScrollableIntent(Context context, int appWidgetId) {
		// Log.d(TAG, "creating ACTION_SCROLL_WIDGET_START intent");
		
		String widgetUri = StocksProvider.CONTENT_URI_WIDGET_QUOTES.buildUpon().appendEncodedPath(
				Integer.toString(appWidgetId)).toString();
		
		Intent clearIntent = new Intent(LauncherIntent.Action.ACTION_SCROLL_WIDGET_CLOSE);
		clearIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		clearIntent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI, widgetUri);
		context.sendBroadcast(clearIntent);
		
		Intent result = new Intent(LauncherIntent.Action.ACTION_SCROLL_WIDGET_START);

		// Put widget info
		result.putExtra(LauncherIntent.Extra.EXTRA_APPWIDGET_ID, appWidgetId);
		result.putExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, R.id.content_view);

		result.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_PROVIDER_ALLOW_REQUERY, true);

		// Give a layout resource to be inflated. If this is not given, the launcher will create one		
		result.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_LAYOUT_ID, R.layout.stocks_widget_list);
		result.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_ID, R.layout.stocks_widget_list_item);
		
		putProvider(result, widgetUri);
		putMapping(context, appWidgetId, result);

		// Launcher can set onClickListener for each children of an item. Without
		// explicitly put this
		// extra, it will just set onItemClickListener by default
		result.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_CHILDREN_CLICKABLE, true);
		return result;
	}

	/**
	 * Put provider info as extras in the specified intent
	 * 
	 * @param intent
	 */
	protected void putProvider(Intent intent, String widgetUri) {
		if (intent == null)
			return;

		final String whereClause = null;
		final String orderBy = null;
		final String[] selectionArgs = null;

		// Put the data uri in as a string. Do not use setData, Home++ does not
		// have a filter for that
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI, widgetUri);

		// Other arguments for managed query
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_PROJECTION, StocksProvider.PROJECTION_QUOTES);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION, whereClause);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION_ARGUMENTS, selectionArgs);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SORT_ORDER, orderBy);
	}

	/**
	 * Put mapping info as extras in intent
	 */
	protected void putMapping(Context context, int appWidgetId, Intent intent) {
		if (intent == null)
			return;

		final int NB_ITEMS_TO_FILL = 7;
		final int[] cursorIndices = new int[NB_ITEMS_TO_FILL];
		final int[] viewTypes = new int[NB_ITEMS_TO_FILL];
		final int[] layoutIds = new int[NB_ITEMS_TO_FILL];
		final boolean[] clickable = new boolean[NB_ITEMS_TO_FILL];
		final int[] defResources = new int[NB_ITEMS_TO_FILL];

		int iItem = 0;
		
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_ACTION_VIEW_URI_INDEX, 
				StocksProvider.QuotesColumns.symbol.ordinal());
		
		cursorIndices[iItem] = StocksProvider.QuotesColumns.symbol.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
		layoutIds[iItem] = R.id.quoteSymbol;
		clickable[iItem] = true;
		defResources[iItem] = 0;
		iItem++;
		
		cursorIndices[iItem] = StocksProvider.QuotesColumns.name.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
		layoutIds[iItem] = R.id.quoteName;
		clickable[iItem] = true;
		defResources[iItem] = 0;
		iItem++;
		
		cursorIndices[iItem] = StocksProvider.QuotesColumns.price.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
		layoutIds[iItem] = R.id.quotePrice;
		clickable[iItem] = true;
		defResources[iItem] = 0;
		iItem++;

		cursorIndices[iItem] = StocksProvider.QuotesColumns.price_date.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
		layoutIds[iItem] = R.id.quoteTime;
		clickable[iItem] = true;
		defResources[iItem] = 0;
		iItem++;

		cursorIndices[iItem] = StocksProvider.QuotesColumns.change.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
		layoutIds[iItem] = R.id.quoteChange;
		clickable[iItem] = true;
		defResources[iItem] = 0;
		iItem++;
		
		cursorIndices[iItem] = StocksProvider.QuotesColumns.pchange.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
		layoutIds[iItem] = R.id.quoteChangePercent;
		clickable[iItem] = true;
		defResources[iItem] = 0;
		iItem++;

		cursorIndices[iItem] = StocksProvider.QuotesColumns.stateimage.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.IMAGERESOURCE;
		layoutIds[iItem] = R.id.stateImage;
		clickable[iItem] = true;
		defResources[iItem] = 0;			

		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_IDS, layoutIds);
		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_TYPES, viewTypes);
		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_CLICKABLE, clickable);
		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_CURSOR_INDICES, cursorIndices);
		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_DEFAULT_RESOURCES, defResources);
	}
}
