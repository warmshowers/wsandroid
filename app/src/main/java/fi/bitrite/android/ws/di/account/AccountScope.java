package fi.bitrite.android.ws.di.account;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;

/**
 * Scope for all instances that need access to a user account.
 */
@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface AccountScope {
}