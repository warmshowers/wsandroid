package fi.bitrite.android.ws;

import roboguice.config.AbstractAndroidModule;
import fi.bitrite.android.ws.auth.AuthenticationService;
import fi.bitrite.android.ws.auth.CredentialsService;
import fi.bitrite.android.ws.auth.impl.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.impl.ExceptionalCredentialsService;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.persistence.impl.StarredHostDaoImpl;
import fi.bitrite.android.ws.search.SearchFactory;
import fi.bitrite.android.ws.search.impl.HttpSearchFactory;

public class WSAndroidModule extends AbstractAndroidModule {

	@Override
	protected void configure() {
	    bind(StarredHostDao.class).to(StarredHostDaoImpl.class);
	    bind(SearchFactory.class).to(HttpSearchFactory.class);
	    bind(CredentialsService.class).to(ExceptionalCredentialsService.class);
	    bind(AuthenticationService.class).to(HttpAuthenticationService.class);
	}
}
