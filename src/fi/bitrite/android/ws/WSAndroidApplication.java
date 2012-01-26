package fi.bitrite.android.ws;

import java.util.List;

import roboguice.application.RoboApplication;

import com.google.inject.Module;

public class WSAndroidApplication extends RoboApplication {

	@Override
	protected void addApplicationModules(List<Module> modules) {
        modules.add(new WSAndroidModule());
	}

}
