package com.paulish.widgets.stocks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class QuoteViewActivity extends Activity{
	
	public static final String EXTRA_QUOTE_SYMBOL = "EXTRA_QUOTE_SYMBOL";
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.stocks_widget_quoteview);
		
		final WebView browser = (WebView)findViewById(R.id.quoteBrowser);
		browser.getSettings().setJavaScriptEnabled(true);
		final Activity activity = this;
		browser.setWebChromeClient(new WebChromeClient() {
		   public void onProgressChanged(WebView view, int progress) {
		       // convert from browser progress to window progress
			   activity.setProgress(progress * 100);
		   }
		 });	
		browser.setWebViewClient(new WebViewClient() {
		   public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
		     Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
		   }
		 });
		// Get the extras from intent
		final Bundle extras = getIntent().getExtras();
        if (extras != null) {
        	final String quoteSymbol = extras.getString(EXTRA_QUOTE_SYMBOL);
        	setTitle(quoteSymbol);
    		browser.loadUrl("http://m.yahoo.com/w/yfinance/quote/" + quoteSymbol + "/");
        }
        else 
            finish();
	}
	
	public static Intent getOpenForSymbolIntent(Context context, String symbol) {
		final Intent quoteViewIntent = new Intent(context, QuoteViewActivity.class);
		quoteViewIntent.putExtra(QuoteViewActivity.EXTRA_QUOTE_SYMBOL, symbol);
		quoteViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return quoteViewIntent;
	}
	
	public static void openForSymbol(Context context, String symbol) {
		context.startActivity(getOpenForSymbolIntent(context, symbol));		
	}
}
