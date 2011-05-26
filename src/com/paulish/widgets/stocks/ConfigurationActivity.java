package com.paulish.widgets.stocks;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class ConfigurationActivity extends PreferenceActivity implements OnPreferenceClickListener {
	
	private int appWidgetId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// a hack to convert update interval from integer to string
		// to use it with ListPrefence
		Preferences.setUpdateInterval(this, Preferences.getUpdateInterval(this));
		super.onCreate(savedInstanceState);
		// Build GUI from resource
		addPreferencesFromResource(R.xml.preferences);
		
		// Get the starting Intent
		Intent launchIntent = getIntent();
		Bundle extras = launchIntent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            // Cancel by default
            Intent cancelResultValue = new Intent();
            cancelResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_CANCELED, cancelResultValue);
        } else {
            finish();
        }
        // prepare the GUI components
		preparePortfolioBtn();
		prepareSaveBtn();
		prepareAboutBtn();
	}

	private void preparePortfolioBtn() {
		Preference pref = findPreference("EDIT_PORTFOLIO");
		pref.setOnPreferenceClickListener(this);
	}

	private void prepareAboutBtn() {
		Preference pref = findPreference("ABOUT");
		pref.setOnPreferenceClickListener(this);
	}

	private void prepareSaveBtn() {
		final Context context = this;
		Preference pref = findPreference("SAVE");
		// Bind the "onClick" for the save preferences to close the activity
		// and postback "OK"
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(final Preference preference) {
				Intent resultValue = new Intent();                    
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);
                StocksProvider.loadFromYahooInBackgroud(context, appWidgetId);
                UpdateService.registerService(context);
                finish();
                return false;
			}
		});		
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		final String key = preference.getKey();
		if (key.equals("ABOUT")) {
			AlertDialog alertDialog;
			alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle(getString(R.string.about));
			alertDialog.setMessage(getString(R.string.about_text));
			alertDialog.setButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {			
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			alertDialog.show();			
		} else if (key.equals("EDIT_PORTFOLIO")) {
			Intent intent = new Intent(this, PortfolioActivity.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			intent.putExtra(PortfolioActivity.TAG_SKIP_UPDATE, true);
			startActivity(intent);
		}
		
		return false;
	}

}
