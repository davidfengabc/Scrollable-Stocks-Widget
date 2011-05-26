package com.paulish.widgets.stocks;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public abstract class StocksWidget extends AppWidgetProvider {
	// Tag for logging
	protected static final String TAG = "paulish.StocksWidget";
	// Actions
	public static final String ACTION_NOTIFY_LOADING = "com.paulish.widgets.stocks.action.NOTIFY_LOADING";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// If no specific widgets requested, collect list of all				

		if (appWidgetIds == null) 
			appWidgetIds = Preferences.getAllWidgetIds(context);

		for (int appWidgetId : appWidgetIds) { 
        	updateWidget(context, appWidgetId, false);
		}		
	}
		
	protected abstract void updateWidget(Context context, int appWidgetId, Boolean loading);
	
	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		//Log.d(TAG, "received -> " +  action);
		if  (ACTION_NOTIFY_LOADING.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,	AppWidgetManager.INVALID_APPWIDGET_ID);
			if ((appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) && appWidgetClassMatch(context, appWidgetId)) {
				updateWidget(context, appWidgetId, intent.getExtras().getBoolean("loading"));
			}			
		} else
			super.onReceive(context, intent);
	}
	
	/**
	 * Will be executed when the widget is removed from the home screen 
	 */
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);			
		// Drop the settings if the widget is deleted
		Preferences.DropSettings(context, appWidgetIds);
	}
	
	@Override
	public void onEnabled(Context context) {
		if (UpdateService.serviceIntent == null)
			UpdateService.registerService(context);
	}
	
    @Override
    public void onDisabled(Context context) {
		UpdateService.removeService(context);
	}
					
	protected boolean appWidgetClassMatch(Context context, int appWidgetId) {
        final AppWidgetManager awm = AppWidgetManager.getInstance(context);
        final AppWidgetProviderInfo info = awm.getAppWidgetInfo(appWidgetId);
        if (info != null)
        	return info.provider.equals(new ComponentName(context, getClass()));
        else
        	return false;
	}
	
	public static void setLoading(Context context, Integer[] appWidgetIds, boolean loading) {
		final Intent intent = new Intent(ACTION_NOTIFY_LOADING);
		intent.putExtra("loading", loading);
		for (int appWidgetId : appWidgetIds) {
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			context.sendBroadcast(intent);
		}		
	}
}