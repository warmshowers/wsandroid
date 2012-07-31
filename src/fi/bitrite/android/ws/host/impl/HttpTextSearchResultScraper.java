package fi.bitrite.android.ws.host.impl;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.util.Log;
import fi.bitrite.android.ws.WSAndroidApplication;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.http.HttpException;

public class HttpTextSearchResultScraper {

	private static final String HOST_NODE_XPATH 	= "//h2[text()=\"Search results\"]/following-sibling::div/table/tbody/tr";

	private static final String HOST_FULLNAME_XPATH = "td[1]/a[1]/text()";
	private static final String HOST_URL_XPATH 		= "td[1]/a[1]/@href";
	private static final String HOST_LOCATION_XPATH = "td[1]/a[2]/text()";
	private static final String HOST_COMMENTS_XPATH = "td[2]/p/text()";

	private String html;

	public HttpTextSearchResultScraper(String html) {
		this.html = html;
	}

	public List<HostBriefInfo> getHosts() {
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse(new InputSource(new StringReader(html)));

			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression expr = xpath.compile(HOST_NODE_XPATH);

			NodeList hostNodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

			XPathExpression nameExpr = xpath.compile(HOST_URL_XPATH);
			XPathExpression fullnameExpr = xpath.compile(HOST_FULLNAME_XPATH);
			XPathExpression locationExpr = xpath.compile(HOST_LOCATION_XPATH);
			XPathExpression commentsExpr = xpath.compile(HOST_COMMENTS_XPATH);

			List<HostBriefInfo> hostList = new ArrayList<HostBriefInfo>();
			for (int i = 0; i < hostNodes.getLength(); i++) {
				Node hostNode = hostNodes.item(i);

				String hostUrl = nameExpr.evaluate(hostNode, XPathConstants.STRING).toString().trim();
				String name = getNameFromHostUrl(hostUrl);
				Log.e(WSAndroidApplication.TAG, name);
				
				String fullname = fullnameExpr.evaluate(hostNode, XPathConstants.STRING).toString().trim();
				String location = locationExpr.evaluate(hostNode, XPathConstants.STRING).toString().trim();
				String comments = commentsExpr.evaluate(hostNode, XPathConstants.STRING).toString().trim();

				hostList.add(new HostBriefInfo(0, name, fullname, location, comments));
			}

			return hostList;
		}

		catch (Exception e) {
			throw new HttpException(e);
		}
	}

	private String getNameFromHostUrl(String hostUrl) {
		return hostUrl.substring(hostUrl.lastIndexOf('/')+1, hostUrl.length());
	}
}
