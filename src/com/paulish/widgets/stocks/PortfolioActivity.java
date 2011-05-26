package com.paulish.widgets.stocks;

import java.util.List;

import com.android.music.TouchInterceptor;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.*;

public class PortfolioActivity extends ListActivity implements OnClickListener {
	
	public final static String TAG_SKIP_UPDATE = "SkipUpdate";
	private final static int requestSymbolSearch = 1;

	private List<String> tickers;
	private ArrayAdapter<String> adapter;
	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private boolean skipUpdate = false;
	private ListView tickersList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stocks_widget_portfolio_edit);
		findViewById(R.id.save).setOnClickListener(this);
		Button btn = (Button)findViewById(R.id.cancel);
		btn.setText(android.R.string.cancel);
		btn.setOnClickListener(this);			
		
		tickersList = getListView();
		registerForContextMenu(tickersList);		
		
		// prepare the listview
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
			if (extras.containsKey(TAG_SKIP_UPDATE))
				skipUpdate = extras.getBoolean(TAG_SKIP_UPDATE);			
		
            ((TouchInterceptor) tickersList).setDropListener(mDropListener);
            ((TouchInterceptor) tickersList).setRemoveListener(mRemoveListener);
            ((TouchInterceptor) tickersList).setRemoveMode(TouchInterceptor.FLING);
            tickersList.setCacheColorHint(0);
            
			tickers = Preferences.getPortfolio(this, appWidgetId);
			adapter = new PortfolioAdapter(this, tickers);
			adapter.add(getString(R.string.addTickerSymbol));            
			tickersList.setAdapter(adapter);
			
		} else
			finish();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cancel: 
			finish(); 
			break;
		case R.id.save:
			savePreferences();
			Intent resultValue = new Intent();                    
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            if (!skipUpdate)
            	StocksProvider.loadFromYahooInBackgroud(this, appWidgetId);
            finish();            
			break;
		}						
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v == tickersList) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			
			if (info.position != tickers.size() - 1) {			
				menu.setHeaderTitle(tickers.get(info.position));
				menu.add(Menu.NONE, 0, 0, R.string.openTickerSymbol);
				menu.add(Menu.NONE, 1, 1, R.string.editTickerSymbol);
				menu.add(Menu.NONE, 2, 2, R.string.deleteTickerSymbol);
			}
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		final int menuItemIndex = item.getItemId();
		final int position = info.position;
		switch (menuItemIndex) {
		case 0:
			QuoteViewActivity.openForSymbol(this, tickers.get(position));
			break;
		case 1:
			editSymbol(position);
			break;
		case 2:
			tickers.remove(position);
			adapter.notifyDataSetChanged();
			break;
		}
		return true;
	}

	@Override
	public void onListItemClick(ListView l, View view, int position, long id) {
		if (position == tickers.size() - 1)
			editSymbol(-1);
		else
			QuoteViewActivity.openForSymbol(this, tickers.get(position));
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == requestSymbolSearch) {
			if (resultCode == RESULT_OK) {
				final int position = data.getIntExtra(SymbolSearchActivity.TAG_POSITION, -1);
				final String symbol = data.getStringExtra(SymbolSearchActivity.TAG_SYMBOL);
				if (position == -1)
					adapter.insert(symbol, tickers.size() - 1);
				else 
					tickers.set(position, symbol);
				adapter.notifyDataSetChanged();
			}			
		} else
			super.onActivityResult(requestCode, resultCode, data);
	}
	
	static class PortfolioAdapter extends ArrayAdapter<String> {
		
		PortfolioAdapter(Context context, List<String> portfolio) {
			super(context, R.layout.stocks_widget_portfolio_edit_list_item, android.R.id.text1, portfolio);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
	        View v = super.getView(position, convertView, parent);	        
	        ImageView iv = (ImageView) v.findViewById(R.id.icon);
	        if (position < getCount() - 1) {
	            iv.setVisibility(View.VISIBLE);
	            iv.setImageResource(R.drawable.ic_mp_move);
	        } else
	        	iv.setVisibility(View.GONE);
	        
			return v;
		}

	}	
    
    private TouchInterceptor.DropListener mDropListener =
        new TouchInterceptor.DropListener() {
        public void drop(int from, int to) {
        	final int last = adapter.getCount() - 1;
        	if (from >= last)
        		from = last - 1;
        	if (to >= last)
        		to = last - 1;
			final String curValue = tickers.get(from);
			tickers.set(from, tickers.get(to));
			tickers.set(to, curValue);
            adapter.notifyDataSetChanged();
            getListView().invalidateViews();
        }
    };
    
    private TouchInterceptor.RemoveListener mRemoveListener =
        new TouchInterceptor.RemoveListener() {
        public void remove(int which) {
        	if (which < adapter.getCount() - 1) {
                View v = tickersList.getChildAt(which - tickersList.getFirstVisiblePosition());
                v.setVisibility(View.GONE);
                tickersList.invalidateViews();
    			tickers.remove(which);
    			adapter.notifyDataSetChanged();            
                v.setVisibility(View.VISIBLE);
                tickersList.invalidateViews();
        	}
        }
    };
    
	private void editSymbol(final int position) {
		Intent search = new Intent(this, SymbolSearchActivity.class);
		search.setAction(Intent.ACTION_EDIT);
		search.putExtra(SymbolSearchActivity.TAG_POSITION, position);
		if (position != -1)
			search.putExtra(SymbolSearchActivity.TAG_SYMBOL, tickers.get(position));
		startActivityForResult(search, requestSymbolSearch);
	}
		
	private void savePreferences() {
		StringBuffer result = new StringBuffer();
		final int count = tickers.size();
		if (count > 1) {
			result.append(tickers.get(0));
			for (int i = 1; i < count - 1; i++) {
				result.append(",");
				result.append(tickers.get(i));
			}
		}
		Preferences.setPortfolio(this, appWidgetId, result.toString());
	}
}
