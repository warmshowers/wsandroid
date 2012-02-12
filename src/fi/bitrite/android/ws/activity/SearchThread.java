package fi.bitrite.android.ws.activity;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import fi.bitrite.android.ws.search.Search;

public class SearchThread extends Thread {
	
	Handler handler;
	Search search;

	public SearchThread(Handler handler, Search search) {
		this.handler = handler;
		this.search = search;
	}
	
	public void run() {
		try {
			Thread.sleep(2000);
		}
		catch (InterruptedException e) {
			Log.e("ERROR", "Search interrupted");
		}
		
		Message msg = handler.obtainMessage();
		
		try {
			msg.obj = search.doSearch();
		}
		
		catch (Exception e) {
			Log.e("SearchThread", e.toString());
			msg.obj = e;
		}
		
		handler.sendMessage(msg);
	}

}
