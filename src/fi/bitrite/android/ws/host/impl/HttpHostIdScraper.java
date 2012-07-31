package fi.bitrite.android.ws.host.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.bitrite.android.ws.util.http.HttpException;

public class HttpHostIdScraper {

	private String html;

	public HttpHostIdScraper(String html) {
		this.html = html;
	}

	public int getId() {
		Pattern p = Pattern.compile("http://www.warmshowers.org/user/(\\d+)");
		Matcher m = p.matcher(html);
		if (m.find()) {
			return new Integer(m.group(1)).intValue();
		} else {
			throw new HttpException("Could not parse host ID");
		}
	}

}
