package fi.bitrite.android.ws.search.impl;

import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.google.android.maps.GeoPoint;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationFailedException;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.search.Search;
import fi.bitrite.android.ws.util.http.HttpUtils;

public class HttpMapSearch implements Search {

	private static final String WARMSHOWERS_MAP_SEARCH_URL = "http://www.warmshowers.org/wsmap_xml_hosts";

	private int numHostsCutoff;
	private MapSearchArea searchArea;

	private HttpSessionContainer sessionContainer;
	
	public HttpMapSearch(GeoPoint topLeft, GeoPoint bottomRight, int numHostsCutoff, HttpSessionContainer sessionContainer) {
		this.searchArea = MapSearchArea.fromGeoPoints(topLeft, bottomRight);
		this.numHostsCutoff = numHostsCutoff; 
		this.sessionContainer = sessionContainer;
	}

	public List<HostBriefInfo> doSearch() {
		String xml = getSearchResultXml();
		return new HttpMapSearchXmlParser(xml, numHostsCutoff).getHosts();
	}

	private String getSearchResultXml() {
		HttpClient client = new DefaultHttpClient();
		String xml;
		try {
			String url = HttpUtils.encodeUrl(generateUrlWithSearchParams());
			HttpGet get = new HttpGet(url);
			HttpContext context = sessionContainer.getSessionContext();
			HttpResponse response = client.execute(get, context);
			HttpEntity entity = response.getEntity();
			xml = EntityUtils.toString(entity);
		}

		catch (Exception e) {
			throw new HttpAuthenticationFailedException(e);
		}

		finally {
			client.getConnectionManager().shutdown();
		}
		
		return xml;
	}

	private String generateUrlWithSearchParams() {
		return new StringBuilder().append(WARMSHOWERS_MAP_SEARCH_URL)
				.append("?minlat=").append(searchArea.minLat)
				.append("&maxlat=").append(searchArea.maxLat)
				.append("&minlon=").append(searchArea.minLon)
				.append("&maxlon=").append(searchArea.maxLon)
				.append("&centerlat=").append(searchArea.centerLat)
				.append("&centerlon=").append(searchArea.centerLon)
				.append("&limitlow=0&maxresults=5000").toString();
	}

}
