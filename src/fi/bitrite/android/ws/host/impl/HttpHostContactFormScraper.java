package fi.bitrite.android.ws.host.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import fi.bitrite.android.ws.util.http.HttpException;

public class HttpHostContactFormScraper {

	private static final Pattern opPattern = Pattern.compile("name=\"op\".*?value=\"(.*?)\"");;
	private static final Pattern formBuildIdpattern = Pattern.compile("name=\"form_build_id\".*?value=\"(.*?)\"");;
	private static final Pattern formTokenPattern = Pattern.compile("name=\"form_token\".*?value=\"(.*?)\"");;
	private static final Pattern formIdPattern = Pattern.compile("name=\"form_id\".*?value=\"(.*?)\"");;

	private String html;

	public HttpHostContactFormScraper(String html) {
		this.html = html;

	}

	public List<NameValuePair> getFormDetails() {
		List<NameValuePair> details = new ArrayList<NameValuePair>();

		addFormDetail(details, "op", opPattern);
		addFormDetail(details, "form_build_id", formBuildIdpattern);
		addFormDetail(details, "form_token", formTokenPattern);
		addFormDetail(details, "form_id", formIdPattern);

		if (details.size() != 4) {
			throw new HttpException("Could not parse contact form");
		}

		return details;
	}

	private void addFormDetail(List<NameValuePair> details, String key, Pattern p) {
		Matcher m = p.matcher(html);
		if (m.find()) {
			details.add(new BasicNameValuePair(key, m.group(1)));
		}
	}

}
