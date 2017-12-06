package fi.bitrite.android.ws.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fi.bitrite.android.ws.activity.AboutActivity;
import fi.bitrite.android.ws.activity.AuthenticatorActivity;
import fi.bitrite.android.ws.activity.FeedbackActivity;
import fi.bitrite.android.ws.activity.HostContactActivity;
import fi.bitrite.android.ws.activity.HostInformationActivity;
import fi.bitrite.android.ws.activity.ListSearchTabActivity;
import fi.bitrite.android.ws.activity.Maps2Activity;
import fi.bitrite.android.ws.activity.MessagesTabActivity;
import fi.bitrite.android.ws.activity.SettingsActivity;
import fi.bitrite.android.ws.activity.StarredHostTabActivity;
import fi.bitrite.android.ws.activity.WSSupportAccountAuthenticatorActivity;
import fi.bitrite.android.ws.activity.WebViewActivity;

@Module
public abstract class ActivitiesModule {
    @ContributesAndroidInjector
    abstract AboutActivity contributeAboutActivity();

    @ContributesAndroidInjector
    abstract AuthenticatorActivity contributeAuthenticatorActivity();

    @ContributesAndroidInjector
    abstract FeedbackActivity contributeFeedbackActivity();

    @ContributesAndroidInjector
    abstract HostContactActivity contributeHostContactActivity();

    @ContributesAndroidInjector
    abstract HostInformationActivity contributeHostInformationActivity();

    @ContributesAndroidInjector
    abstract ListSearchTabActivity contributeListSearchTabActivity();

    @ContributesAndroidInjector
    abstract Maps2Activity contributeMaps2Activity();

    @ContributesAndroidInjector
    abstract MessagesTabActivity contributeMessagesTabActivity();

    @ContributesAndroidInjector
    abstract SettingsActivity contributeSettingsActivity();

    @ContributesAndroidInjector
    abstract StarredHostTabActivity contributeStarredHostTabActivity();

    @ContributesAndroidInjector
    abstract WebViewActivity contributeWebViewActivity();

    @ContributesAndroidInjector
    abstract WSSupportAccountAuthenticatorActivity contributeWSSupportAccountAuthenticatorActivity();
}
