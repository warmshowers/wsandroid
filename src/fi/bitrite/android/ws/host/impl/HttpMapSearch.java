package fi.bitrite.android.ws.host.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.google.android.maps.GeoPoint;

import fi.bitrite.android.ws.auth.http.HttpAuthenticationFailedException;
import fi.bitrite.android.ws.auth.http.HttpAuthenticationService;
import fi.bitrite.android.ws.auth.http.HttpSessionContainer;
import fi.bitrite.android.ws.host.Search;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.http.HttpUtils;

public class HttpMapSearch implements Search {

	private static final String WARMSHOWERS_MAP_SEARCH_URL = "http://www.warmshowers.org/services/rest/hosts/by_location";

	private int numHostsCutoff;
	private MapSearchArea searchArea;

	private HttpAuthenticationService authenticationService;
	private HttpSessionContainer sessionContainer;
	
	public HttpMapSearch(GeoPoint topLeft, GeoPoint bottomRight, int numHostsCutoff, HttpAuthenticationService authenticationService, HttpSessionContainer sessionContainer) {
		this.searchArea = MapSearchArea.fromGeoPoints(topLeft, bottomRight);
		this.numHostsCutoff = numHostsCutoff; 
		this.authenticationService = authenticationService;
		this.sessionContainer = sessionContainer;
	}

	public List<HostBriefInfo> doSearch() {
		// The map search works even if we're not authenticated,
		// but it returns less data. Easier to check first using
		// a simple GET
		if (!authenticationService.isAuthenticated()) {
			authenticationService.authenticate();
		}

		String xml = getHostsJson();
		return new HttpMapSearchJsonParser(xml, numHostsCutoff).getHosts();
	}

	private String getHostsJson() {
		HttpClient client = HttpUtils.getDefaultClient();

		String json;
		try {
            List<NameValuePair> searchParams = getSearchParameters();

            HttpPost post = new HttpPost(WARMSHOWERS_MAP_SEARCH_URL);
            post.setEntity(new UrlEncodedFormEntity(searchParams));
            HttpContext httpContext = sessionContainer.getSessionContext();
            HttpResponse response = client.execute(post, httpContext);

			HttpEntity entity = response.getEntity();
            json = EntityUtils.toString(entity, "UTF-8");
		}

		catch (Exception e) {
			throw new HttpAuthenticationFailedException(e);
		}

		finally {
			client.getConnectionManager().shutdown();
		}
		
		return json;
	}

    private List<NameValuePair> getSearchParameters() {
        List<NameValuePair> args = new ArrayList<NameValuePair>();
        args.add(new BasicNameValuePair("minlat", String.valueOf(searchArea.minLat)));
        args.add(new BasicNameValuePair("maxlat", String.valueOf(searchArea.maxLat)));
        args.add(new BasicNameValuePair("minlon", String.valueOf(searchArea.minLon)));
        args.add(new BasicNameValuePair("maxlon", String.valueOf(searchArea.maxLon)));
        args.add(new BasicNameValuePair("centerlat", String.valueOf(searchArea.centerLat)));
        args.add(new BasicNameValuePair("centerlon", String.valueOf(searchArea.centerLon)));
        args.add(new BasicNameValuePair("limit", String.valueOf(this.numHostsCutoff)));
        return args;
    }

}
