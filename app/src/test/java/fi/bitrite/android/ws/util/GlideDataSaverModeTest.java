package fi.bitrite.android.ws.util;

import android.app.Application;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.di.DaggerTestAppComponent;
import fi.bitrite.android.ws.di.TestAppComponent;
import fi.bitrite.android.ws.repository.SettingsRepository;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests that Glide by default does not load any images from the web when in dataSaver mode.
 * This can be overridden on a per-request basis. Also, cached images are expected to still be
 * provided.
 */
@RunWith(RobolectricTestRunner.class)
public class GlideDataSaverModeTest {
    @Inject MockWebServer mMockWebServer;
    @Inject @Named("WSBaseUrl") String mMockWebServerBaseUrl;
    @Inject SettingsRepository mSettingsRepository;

    @Before
    public void setup() {
        WSAndroidApplication app = (WSAndroidApplication) RuntimeEnvironment.application;

        TestAppComponent appComponent = DaggerTestAppComponent.builder()
                .application(app)
                .build();
        appComponent.inject(this);
    }

    @Test
    public void testGlideDefaultOptionsUpdateForDataSaverMode() {
        final AtomicInteger webLoadCount = new AtomicInteger(); // We need a final object.
        mMockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                webLoadCount.incrementAndGet();
                return new MockResponse();
            }
        });

        final Semaphore semaphore = new Semaphore(0);
        final Target loadOkTarget = new SimpleTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource,
                                        @Nullable Transition<? super Drawable> transition) {
                semaphore.release();
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                assertThat(false).isTrue();
            }
        };
        final Target loadFailedTarget = new SimpleTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource,
                                        @Nullable Transition<? super Drawable> transition) {
                assertThat(false).isTrue();
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                semaphore.release();
            }
        };

        final Application app = RuntimeEnvironment.application;

        // 1) Enable dataSaver mode.
        setDataSaverMode(true);
        webLoadCount.set(0);
        WSGlide.with(app)
                .load(mMockWebServerBaseUrl + "cat0.jpg")
                .into(loadFailedTarget);
        waitForTestCaseCompletion(semaphore);
        assertThat(webLoadCount.get()).isZero();

        // 2) Disable dataSaver mode.
        setDataSaverMode(false);
        webLoadCount.set(0);
        WSGlide.with(app)
                .load(mMockWebServerBaseUrl + "cat1.jpg")
                .into(loadOkTarget);
        waitForTestCaseCompletion(semaphore);
        assertThat(webLoadCount.get()).isNotZero();

        // 3) Enable dataSaver mode once more.
        setDataSaverMode(true);
        webLoadCount.set(0);
        WSGlide.with(app)
                .load(mMockWebServerBaseUrl + "cat2.jpg")
                .into(loadFailedTarget);
        waitForTestCaseCompletion(semaphore);
        assertThat(webLoadCount.get()).isZero();

        // 4) Force load even though dataSaver mode is enabled.
        webLoadCount.set(0);
        WSGlide.with(app)
                .load(mMockWebServerBaseUrl + "cat3.jpg")
                .apply(new RequestOptions().onlyRetrieveFromCache(false))
                .into(loadOkTarget);
        waitForTestCaseCompletion(semaphore);
        assertThat(webLoadCount.get()).isNotZero();

        // 5) Expect an image not to be loaded from the web but still being available if cached.
        webLoadCount.set(0);
        WSGlide.with(app)
                .load(mMockWebServerBaseUrl + "cat3.jpg")
                .into(loadOkTarget);
        waitForTestCaseCompletion(semaphore);
        assertThat(webLoadCount.get()).isZero();
    }

    private void setDataSaverMode(boolean dataSaverMode) {
        mSettingsRepository.mSharedPreferences
                .edit()
                .putBoolean(mSettingsRepository.getDataSaverModeKey(), dataSaverMode)
                .commit();
    }

    /**
     * Wait for the given semaphore to become available but flush the foreground and background
     * thread schedulers while waiting.
     */
    private static void waitForTestCaseCompletion(Semaphore semaphore) {
        int counter = 10000;
        while (--counter != 0) {
            Robolectric.flushForegroundThreadScheduler();
            Robolectric.flushBackgroundThreadScheduler();

            if (semaphore.tryAcquire()) {
                return;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        assertThat(true).isFalse();
    }
}