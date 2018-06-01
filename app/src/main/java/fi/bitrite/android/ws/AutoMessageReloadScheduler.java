package fi.bitrite.android.ws;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import fi.bitrite.android.ws.auth.AccountManager;
import fi.bitrite.android.ws.di.AppScope;
import fi.bitrite.android.ws.di.account.AccountComponentManager;
import fi.bitrite.android.ws.repository.MessageRepository;
import fi.bitrite.android.ws.repository.SettingsRepository;
import io.reactivex.Completable;

@AppScope
public class AutoMessageReloadScheduler
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final AccountComponentManager mAccountComponentManager;
    private final AccountManager mAccountManager;
    private final Context mContext;
    private final SettingsRepository mSettingsRepository;

    private boolean mHasAccounts = true;
    private long mMessageReloadIntervalMs;

    @Inject
    AutoMessageReloadScheduler(AccountManager accountManager,
                               AccountComponentManager accountComponentManager, Context context,
                               SettingsRepository settingsRepository) {
        mAccountComponentManager = accountComponentManager;
        mAccountManager = accountManager;
        mContext = context;
        mSettingsRepository = settingsRepository;

        mAccountManager.getAccounts().subscribe(accounts -> {
            boolean hasAccounts = accounts.length > 0;
            if (hasAccounts == mHasAccounts) {
                return;
            }
            mHasAccounts = hasAccounts;

            reschedule();
        });

        // Register for settings updates. That triggers an initial run of the change listener.
        mSettingsRepository.registerOnChangeListener(this);
    }

    public long getMessageReloadIntervalMs() {
        return mHasAccounts ? mMessageReloadIntervalMs : 0;
    }

    public static class AccountHelper {
        @Inject MessageRepository messageRepository;
    }

    public Completable reloadMessagesInAllAccounts() {
        AccountHelper accountHelper = new AccountHelper();
        List<Completable> completables = new LinkedList<>();
        for (Account account : mAccountManager.getAccounts().getValue()) {
            mAccountComponentManager.get(account).inject(accountHelper);
            completables.add(accountHelper.messageRepository.reloadThreads());
        }
        return Completable.mergeDelayError(completables);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null || key.equals(mSettingsRepository.getMessageRefreshIntervalKey())) {
            mMessageReloadIntervalMs = TimeUnit.MINUTES.toMillis(
                    mSettingsRepository.getMessageRefreshIntervalMin());
            reschedule();
        }
    }

    private void reschedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AutoMessageReloadJobService.reschedule(mContext, getMessageReloadIntervalMs());
        } else {
            AutoMessageReloadService.reschedule(mContext, getMessageReloadIntervalMs());
        }

    }
}

