package fi.bitrite.android.ws;

import roboguice.config.AbstractAndroidModule;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.persistence.impl.StarredHostDaoImpl;
import fi.bitrite.android.ws.search.SearchFactory;
import fi.bitrite.android.ws.search.impl.MockSearchFactory;

public class WSAndroidModule extends AbstractAndroidModule {

	@Override
	protected void configure() {
	    bind(StarredHostDao.class).to(StarredHostDaoImpl.class);
	    bind(SearchFactory.class).to(MockSearchFactory.class);
	}
}
