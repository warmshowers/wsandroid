package fi.bitrite.android.ws;

import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import fi.bitrite.android.ws.auth.AccountHelper;
import fi.bitrite.android.ws.di.DaggerTestAppComponent;
import fi.bitrite.android.ws.di.TestAppComponent;
import io.reactivex.plugins.RxJavaPlugins;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper class with useful functions that are used in several tests.
 */
public class WSTest {
    private static final TestAppComponent mAppComponent;
    static {
        WSAndroidApplication app = (WSAndroidApplication) RuntimeEnvironment.application;

        mAppComponent = DaggerTestAppComponent.builder()
                .application(app)
                .build();
        mAppComponent.inject(app);
    }

    private static AccountHelper mAccountHelper;

    private WSTest() {}

    static public TestAppComponent appComponent() {
        return mAppComponent;
    }

    static public AccountHelper accountHelper() {
        if (mAccountHelper == null) {
            mAccountHelper = new AccountHelper(
                    mAppComponent.getAccountManager(),
                    mAppComponent.getAccountComponentManager());
        }
        return mAccountHelper;
    }

    static public UncaughtRxExceptionHelper createUncaughtRxExceptionHelper() {
        UncaughtRxExceptionHelper uncaughtRxExceptionHelper = new UncaughtRxExceptionHelper();
        RxJavaPlugins.setErrorHandler(e -> uncaughtRxExceptionHelper.throwable = e);
        return uncaughtRxExceptionHelper;
    }

    public static class UncaughtRxExceptionHelper {
        public Throwable throwable;

        public void assertNoException() {
            assertThat(throwable).isNull();
        }
    }

    public static void flush() {
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
    }

    public static void sleepAndFlush(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Ignore.
        }
        flush();
    }
}
