package fi.bitrite.android.ws.search.impl;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import fi.bitrite.android.ws.model.HostBriefInfo;
import fi.bitrite.android.ws.util.http.HttpException;

public class HttpMapSearchXmlParser {

	private String xml;
	private int numHostsCutoff;
	
	public HttpMapSearchXmlParser(String xml, int numHostsCutoff) {
		this.xml = xml;
		this.numHostsCutoff = numHostsCutoff;
	}

	public List<HostBriefInfo> getHosts() {
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse(new InputSource(new StringReader(xml)));

			if (!isComplete(doc)) {
				throw new IncompleteResultsException("Could not retrieve hosts. Try again.");
			}
			
			int numHosts = getNumHosts(doc);
			if (numHosts > numHostsCutoff) {
				throw new TooManyHostsException(numHosts);
			}
			
			return parseHostNodes(doc);
		}

		catch (HttpException e) {
			throw e;
		}
		
		catch (Exception e) {
			throw new HttpException(e);
		}
	}

	private boolean isComplete(Document doc) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile("//status");
		Node statusNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
		return statusNode.getTextContent().equals("complete");
	}

	private int getNumHosts(Document doc) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile("//status");
		Node statusNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
		
		NamedNodeMap attributes = statusNode.getAttributes();
		Node totalresults = attributes.getNamedItem("totalresults");
		Integer numHosts = new Integer(totalresults.getTextContent());
		
		return numHosts.intValue();
	}

	private List<HostBriefInfo> parseHostNodes(Document doc) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile("/root/hosts/host");

		NodeList hostNodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

		List<HostBriefInfo> hostList = new ArrayList<HostBriefInfo>();
		for (int i = 0; i < hostNodes.getLength(); i++) {
			Node hostNode = hostNodes.item(i);
			NamedNodeMap attributes = hostNode.getAttributes();

			int id = new Integer(attributes.getNamedItem("u").getTextContent()).intValue();
			
			Node fullnameNode = attributes.getNamedItem("n");
			String fullname = (fullnameNode == null) ? "(Unknown host)" : fullnameNode.getTextContent();
			
			Node streetNode = attributes.getNamedItem("s");
			StringBuilder location = new StringBuilder();
			if (streetNode != null) {
				location.append(attributes.getNamedItem("s").getTextContent());
			}
			
			if (location.length() == 0) {
				location.append(attributes.getNamedItem("c").getTextContent()).append(", ")
					.append(attributes.getNamedItem("p").getTextContent()).append(", ")
					.append(attributes.getNamedItem("cnt").getTextContent().toUpperCase())
					.toString();				
			}

			String latitude = attributes.getNamedItem("la").getTextContent();
			String longitude = attributes.getNamedItem("ln").getTextContent();
			
			HostBriefInfo host = new HostBriefInfo(id, null, fullname, location.toString(), null);
			host.setLatitude(latitude);
			host.setLongitude(longitude);
			hostList.add(host);
		}

		return hostList;
	}
	
}
