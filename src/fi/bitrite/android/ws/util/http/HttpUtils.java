package fi.bitrite.android.ws.util.http;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class HttpUtils {

	public static String encodeUrl(String urlString) throws MalformedURLException, URISyntaxException {
		URL url = new URL(urlString);
		URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
		url = uri.toURL();
		return url.toString();
	}

}
