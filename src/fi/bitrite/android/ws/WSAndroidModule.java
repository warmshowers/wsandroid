package fi.bitrite.android.ws;

import fi.bitrite.android.ws.host.impl.WsSearchFactory;
import roboguice.config.AbstractAndroidModule;
import fi.bitrite.android.ws.host.SearchFactory;
import fi.bitrite.android.ws.persistence.StarredHostDao;
import fi.bitrite.android.ws.persistence.impl.StarredHostDaoImpl;

public class WSAndroidModule extends AbstractAndroidModule {

	@Override
	protected void configure() {
	    bind(StarredHostDao.class).to(StarredHostDaoImpl.class);
	    bind(SearchFactory.class).to(WsSearchFactory.class);
	}
}
