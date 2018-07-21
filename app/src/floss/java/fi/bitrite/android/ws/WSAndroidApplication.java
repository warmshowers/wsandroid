package fi.bitrite.android.ws;

import fi.bitrite.android.ws.di.AppInjector;

public class WSAndroidApplication extends BaseWSAndroidApplication {
    protected AppInjector inject() {
        return AppInjector.create(this);
    }
}
