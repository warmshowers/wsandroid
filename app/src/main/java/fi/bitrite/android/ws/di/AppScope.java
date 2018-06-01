package fi.bitrite.android.ws.di;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;

/**
 * Scope for all instances that do not need access to a user account.
 */
@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface AppScope {
}