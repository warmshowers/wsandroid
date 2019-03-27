package fi.bitrite.android.ws.di.account;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fi.bitrite.android.ws.ui.AboutFragment;
import fi.bitrite.android.ws.ui.ContactUserFragment;
import fi.bitrite.android.ws.ui.FavoriteUsersFragment;
import fi.bitrite.android.ws.ui.FeedbackFragment;
import fi.bitrite.android.ws.ui.FilterListFragment;
import fi.bitrite.android.ws.ui.MapFragment;
import fi.bitrite.android.ws.ui.MessageThreadFragment;
import fi.bitrite.android.ws.ui.MessageThreadsFragment;
import fi.bitrite.android.ws.ui.SearchFragment;
import fi.bitrite.android.ws.ui.SettingsFragment;
import fi.bitrite.android.ws.ui.UserFragment;

@Module
abstract class ActivitiesModule {
    @ContributesAndroidInjector
    abstract AboutFragment contributeAboutFragment();

    @ContributesAndroidInjector
    abstract ContactUserFragment contributeContactUserFragment();

    @ContributesAndroidInjector
    abstract FavoriteUsersFragment contributeFavoriteUsersFragment();

    @ContributesAndroidInjector
    abstract FeedbackFragment contributeFeedbackFragment();

    @ContributesAndroidInjector
    abstract MapFragment contributeMapFragment();

    @ContributesAndroidInjector
    abstract MessageThreadFragment contributeMessageThreadFragment();

    @ContributesAndroidInjector
    abstract MessageThreadsFragment contributeMessageThreadsFragment();

    @ContributesAndroidInjector
    abstract SearchFragment contributeSearchFragment();

    @ContributesAndroidInjector
    abstract SettingsFragment contributeSettingsFragment();

    @ContributesAndroidInjector
    abstract UserFragment contributeUserFragment();

    @ContributesAndroidInjector
    abstract FilterListFragment contributeFilterListFragment();
}
