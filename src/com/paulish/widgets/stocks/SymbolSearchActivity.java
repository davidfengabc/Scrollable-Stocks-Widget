package com.paulish.widgets.stocks;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class SymbolSearchActivity extends ListActivity {
	
	public final static String TAG_POSITION = "position";
	public final static String TAG_SYMBOL = "symbol";

	private int position = -1;
	private String query;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stocks_widget_search);
		
		onNewIntent(getIntent());
	}
	
    @Override
    public void onNewIntent(Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
            ListAdapter adapter = null;

            // Start query for incoming search request
            query = intent.getStringExtra(SearchManager.QUERY);
            
            // search the query
            Cursor cur = managedQuery(StocksSearchProvider.CONTENT_URI.buildUpon().appendEncodedPath(SearchManager.SUGGEST_URI_PATH_QUERY).appendEncodedPath(query).build(), null, null, null, null);
            if (cur.getCount() > 0) {
                startManagingCursor(cur);

                adapter = new SimpleCursorAdapter(
                        this, // Context.
                        android.R.layout.two_line_list_item,
                        cur,                                              	  
                        new String[] {SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2},
                        new int[] {android.R.id.text1, android.R.id.text2});
            } else {
            	cur.close();
            	
    			adapter = new ArrayAdapter<String>(
    					this, 
    					android.R.layout.simple_list_item_1, 
    					new String[] {String.format(getString(R.string.symbolNotFound), query)});
            }
            	
            setListAdapter(adapter);           
        } else if (Intent.ACTION_EDIT.equals(action)) {
        	position = intent.getIntExtra(TAG_POSITION, -1);
        	final String ticker = intent.getStringExtra(TAG_SYMBOL);
        	startSearch(ticker, false, null, false);
        } else if (Intent.ACTION_PICK.equals(action)) {
        	returnSymbol(intent.getDataString());
        }
    }
    
    private void returnSymbol(String symbol) {
		Intent resultValue = new Intent();                    
        resultValue.putExtra(TAG_POSITION, position);
        resultValue.putExtra(TAG_SYMBOL, symbol);
        setResult(RESULT_OK, resultValue);
        finish();    	
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	Object item = getListView().getItemAtPosition(position);
    	if (item instanceof CursorWrapper)
    		returnSymbol(((CursorWrapper)item).getString(1));
    	else
    		returnSymbol(query);
    }

}
