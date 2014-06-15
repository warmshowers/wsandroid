package fi.bitrite.android.ws.host.impl;

import fi.bitrite.android.ws.activity.model.HostInformation;
import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.http.HttpException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class HttpTextSearchResultScraper {

    private static final String HOST_NODE_XPATH     = "//tbody/tr";

    private final String html;

    public HttpTextSearchResultScraper(String html) {
        this.html = html;
    }

    public List<HostBriefInfo> getHosts() {
        try {
			String body = html.substring(html.indexOf("<body"));
			String full = "<html>" + body;

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(full)));

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile(HOST_NODE_XPATH);

            NodeList hostNodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            List<HostBriefInfo> hostList = new ArrayList<HostBriefInfo>();
            for (int i = 0; i < hostNodes.getLength(); i++) {
                Node hostNode = hostNodes.item(i);

				String fullname = hostNode.getFirstChild().getFirstChild().getTextContent().trim();
				String hostUrl = hostNode.getFirstChild().getFirstChild().getAttributes().getNamedItem("href").getTextContent();
				String name = hostUrl.substring(hostUrl.lastIndexOf("/") + 1);

				int id = HostInformation.NO_ID;

				try {
					id = Integer.parseInt(name);
					name = null;
				}

				catch (NumberFormatException e) {
					// do nothing
				}

				String location = hostNode.getFirstChild().getFirstChild().getNextSibling().getNextSibling().getTextContent().trim();
				String comments = hostNode.getFirstChild().getNextSibling().getTextContent();

                hostList.add(new HostBriefInfo(id, name, fullname, location, comments));
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
