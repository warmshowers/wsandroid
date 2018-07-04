package fi.bitrite.android.ws.util;

import android.app.Application;
import android.os.Build;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.Locale;

import fi.bitrite.android.ws.ui.MainActivity;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(RobolectricTestRunner.class)
@Config(qualifiers = "fr-rFR")
public class ToolsTest {

    private static Application application;
    @Before
    public void setUp() {
        application = RuntimeEnvironment.application;
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.N)
    public void getLocale_noContext() {
        final ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class);
        controller.setup();

        // Get Locale for current setup. Should be fr-FR as specified in Test's config
        final Locale locale = Tools.getLocale(null);

        Assert.assertThat("If no context supplied, should always be the same", locale,
                is(equalTo(Locale.getDefault())));

        // Resets the qualifier to set the Locale to German
        RuntimeEnvironment.setQualifiers("de-rDE");
        controller.configurationChange();

        // Get Locale for changed setup. Should normally be de-DE now but since the
        // Locale.getDefault method doesn't change upon configuration changes,
        // it will still be fr-FR.
        final Locale localeAfterChange = Tools.getLocale(null);

        Assert.assertThat("If no context supplied, should always be the same",
                localeAfterChange, is(equalTo(Locale.getDefault())));
    }


    @Test
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    public void getLocale_androidPreN() {
        final ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class);
        controller.setup();

        // Get Locale for current setup. Should be fr-FR as specified in Test's config
        final Locale locale = Tools.getLocale(application);

        Assert.assertThat("At this test stage, both should be the same", locale,
                is(equalTo(Locale.getDefault())));

        // Resets the qualifier to set the Locale to German
        RuntimeEnvironment.setQualifiers("de-rDE");
        controller.configurationChange();

        // Get Locale for changed setup. Should be de-DE now
        final Locale localeAfterChange = Tools.getLocale(application);

        Assert.assertThat("At this test stage, those two should differ", localeAfterChange,
                is(not(equalTo(Locale.getDefault()))));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.N)
    public void getLocale_androidN() {
        final ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class);
        controller.setup();

        // Get Locale for current setup. Should be fr-FR as specified in Test's config
        final Locale locale = Tools.getLocale(application);

        Assert.assertThat("At this test stage, both should be the same", locale,
                is(equalTo(Locale.getDefault())));

        // Resets the qualifier to set the Locale to German
        RuntimeEnvironment.setQualifiers("de-rDE");
        controller.configurationChange();

        // Get Locale for changed setup. Should be de-DE now
        final Locale localeAfterChange = Tools.getLocale(application);

        Assert.assertThat("At this test stage, those two should differ", localeAfterChange,
                is(not(equalTo(Locale.getDefault()))));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.O)
    public void getLocale_androidPastN() {
        final ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class);
        controller.setup();

        // Get Locale for current setup. Should be fr-FR as specified in Test's config
        final Locale locale = Tools.getLocale(application);

        Assert.assertThat("At this test stage, both should be the same", locale,
                is(equalTo(Locale.getDefault())));

        // Resets the qualifier to set the Locale to German
        RuntimeEnvironment.setQualifiers("de-rDE");
        controller.configurationChange();

        // Get Locale for changed setup. Should be de-DE now
        final Locale localeAfterChange = Tools.getLocale(application);

        Assert.assertThat("At this test stage, those two should differ", localeAfterChange,
                is(not(equalTo(Locale.getDefault()))));
    }
}