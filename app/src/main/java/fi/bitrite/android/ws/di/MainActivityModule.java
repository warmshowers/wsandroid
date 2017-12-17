package fi.bitrite.android.ws.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fi.bitrite.android.ws.ui.AboutFragment;
import fi.bitrite.android.ws.ui.ContactUserFragment;
import fi.bitrite.android.ws.ui.FavoriteUsersFragment;
import fi.bitrite.android.ws.ui.FeedbackFragment;
import fi.bitrite.android.ws.ui.MapFragment;
import fi.bitrite.android.ws.ui.MessagesFragment;
import fi.bitrite.android.ws.ui.SearchFragment;
import fi.bitrite.android.ws.ui.SettingsFragment;
import fi.bitrite.android.ws.ui.UserFragment;

@Module
public abstract class MainActivityModule {
    @ContributesAndroidInjector
    abstract AboutFragment contributeAboutFragment();

    @ContributesAndroidInjector
    abstract ContactUserFragment contributeHostContactFragment();

    @ContributesAndroidInjector
    abstract FavoriteUsersFragment contributeStarredHostTabFragment();

    @ContributesAndroidInjector
    abstract FeedbackFragment contributeFeedbackFragment();

    @ContributesAndroidInjector
    abstract MapFragment contributeMapFragment();

    @ContributesAndroidInjector
    abstract MessagesFragment contributeMessagesTabFragment();

    @ContributesAndroidInjector
    abstract SearchFragment contributeListSearchTabFragment();

    @ContributesAndroidInjector
    abstract SettingsFragment contributeSettingsFragment();

    @ContributesAndroidInjector
    abstract UserFragment contributeHostInformationFragment();
}
