package fi.bitrite.android.ws.activity;

import java.util.List;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import fi.bitrite.android.ws.model.Host;
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
		
		List<Host> hosts = search.doSearch();
 		
		Message msg = handler.obtainMessage();
		handler.sendMessage(msg);
	}

}
