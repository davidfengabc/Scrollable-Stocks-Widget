package com.paulish.widgets.stocks;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.*;

public class UpdateService extends Service {

	public static PendingIntent serviceIntent = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
			StocksProvider.loadFromYahooInBackgroud(getApplicationContext(), null);
		else
			StocksProvider.loadFromYahooInBackgroud(getApplicationContext(), appWidgetId);
		
		stopSelf(startId);
	    
	    return START_STICKY;
	}
	
	public static void registerService(Context context) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		if (serviceIntent != null)
			alarmManager.cancel(serviceIntent);
		
		final int updateInterval = Preferences.getUpdateInterval(context) * 60000;
	
		if (updateInterval != 0) {
			if (serviceIntent == null) {
				Intent intent = new Intent(context, UpdateService.class);
				serviceIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);							
			}

			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
					SystemClock.elapsedRealtime() + updateInterval, 
					updateInterval, serviceIntent);
		}		
	}
	
	public static void removeService(Context context) {
		if (serviceIntent != null) {
			((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(serviceIntent);
			serviceIntent = null;
		}
	}
	
}
