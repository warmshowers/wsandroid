package fi.bitrite.android.ws.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fi.bitrite.android.ws.ui.AuthenticatorActivity;
import fi.bitrite.android.ws.ui.MainActivity;

@Module
public abstract class ActivitiesModule {
    @ContributesAndroidInjector
    abstract AuthenticatorActivity contributeAuthenticatorActivity();

    @ContributesAndroidInjector(modules = MainActivityModule.class)
    abstract MainActivity contributeMainActivity();
}
