package fi.bitrite.android.ws;

import java.util.List;

import roboguice.application.RoboApplication;
import android.content.Context;

import com.google.inject.Module;

public class WSAndroidApplication extends RoboApplication {

    private static Context context;

    public void onCreate(){
        super.onCreate();
        WSAndroidApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return WSAndroidApplication.context;
    }
	
	@Override
	protected void addApplicationModules(List<Module> modules) {
        modules.add(new WSAndroidModule());
	}
	
	

}
