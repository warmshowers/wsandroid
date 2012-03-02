package fi.bitrite.android.ws;

import roboguice.config.AbstractAndroidModule;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationServiceProvider;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.auth.http.HttpSessionContainerProvider;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.persistence.impl.StarredHostDaoImpl;
import fi.bitrite.android.ws.search.SearchFactory;
import fi.bitrite.android.ws.search.impl.HttpSearchFactory;

public class WSAndroidModule extends AbstractAndroidModule {

	@Override
	protected void configure() {
	    bind(StarredHostDao.class).to(StarredHostDaoImpl.class);
	    bind(SearchFactory.class).to(HttpSearchFactory.class);
	    bind(HttpAuthenticationService.class).to(HttpAuthenticationServiceProvider.class);
	    bind(HttpSessionContainer.class).to(HttpSessionContainerProvider.class);
	}
}
