package fi.bitrite.android.ws.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osmdroid.util.BoundingBox;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import fi.bitrite.android.ws.WSTest;
import fi.bitrite.android.ws.shadow.ShadowAccountManager;
import io.reactivex.disposables.Disposable;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(RobolectricTestRunner.class)
@Config(shadows={ShadowAccountManager.class})
public class UserRegionalCacheTest {

    @Inject MockWebServer mMockWebServer;
    @Inject UserRegionalCache mUserRegionalCache;

    @Before
    public void setup() {
        WSTest.accountHelper()
                .createAccount()
                .inject(this);
    }

    /**
     * Verifies that no uncaught exception appears when the disposable returned from
     * {@link UserRegionalCache#searchByLocation(BoundingBox)} is disposed when the internal call
     * to the webservice is only failing later.
     */
    @Test
    public void testSearchByLocationTimeoutOnDisposed() {
        WSTest.UncaughtRxExceptionHelper uncaughtRxExceptionHelper =
                WSTest.createUncaughtRxExceptionHelper();

        final Lock lock = new ReentrantLock();
        final AtomicBoolean requestStarted = new AtomicBoolean(false);
        final AtomicBoolean requestServed = new AtomicBoolean(false);

        mMockWebServer.setDispatcher(new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (!request.getPath().equals("/services/rest/hosts/by_location")) {
                    return new MockResponse()
                            .setStatus("HTTP/1.1 500")
                            .setBody("[\"Unhandled\"]");
                }

                requestStarted.set(true);
                try {
                    // Wait for the lock to be released. Then we are sure that the disposable is
                    // disposed.
                    lock.lock();

                    return new MockResponse()
                            .setStatus("HTTP/1.1 500")
                            .setBody("[\"Mock error\"]");
                } finally {
                    requestServed.set(true);
                    lock.unlock();
                }
            }
        });

        // Set up the request. Hold the lock to ensure the request not being served before the
        // disposable is disposed.
        try {
            lock.lock();
            Disposable disposable = mUserRegionalCache.searchByLocation(new BoundingBox(0, 3, 2, 1))
                    .subscribe(
                            searchResult -> fail("No result expected from disposed"),
                            throwable -> fail("No error expected from disposed"));

            for (int i = 0; i < 10; i++) {
                if (requestStarted.get()) {
                    break;
                }
                WSTest.sleepAndFlush(100);
            }
            assertThat(requestStarted.get()).isTrue();

            disposable.dispose();
        } finally {
            lock.unlock();
        }

        // Now wait some time to verify that no uncaught exception shows up.
        for (int i = 0; i < 10; ++i) {
            WSTest.sleepAndFlush(100);
        }
        assertThat(requestServed.get()).isTrue();
        uncaughtRxExceptionHelper.assertNoException();
    }

}
