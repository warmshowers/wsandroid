package fi.bitrite.android.ws;

import roboguice.config.AbstractAndroidModule;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.persistence.impl.StarredHostDaoImpl;
import fi.bitrite.android.ws.search.TextSearch;
import fi.bitrite.android.ws.search.impl.TextSearchImpl;

public class WSAndroidModule extends AbstractAndroidModule {

	@Override
	protected void configure() {
	    bind(StarredHostDao.class).to(StarredHostDaoImpl.class);
	    bind(TextSearch.class).to(TextSearchImpl.class);
	}
}
