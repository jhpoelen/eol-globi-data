package org.eol.globi.taxon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.service.PropertyEnricherException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

public class ServiceUtil {
    public static String extractPath(String xmlContent, String elementName, String valuePrefix) throws PropertyEnricherException {
        return extractPath(xmlContent, elementName, valuePrefix, "");
    }

    public static List<String> extractPathNoJoin(String xmlContent, String elementName, String valuePrefix) throws PropertyEnricherException {
        return extractPathNoJoin(xmlContent, elementName, valuePrefix, "");
    }

    public static String extractPath(String xmlContent, String elementName, String valuePrefix, String valueSuffix) throws PropertyEnricherException {
        List<String> ranks = extractPathNoJoin(xmlContent, elementName, valuePrefix, valueSuffix);
        return StringUtils.join(ranks, CharsetConstant.SEPARATOR);
    }

    public static List<String> extractPathNoJoin(String xmlContent, String elementName, String valuePrefix, String valueSuffix) throws PropertyEnricherException {
        List<String> ranks = new ArrayList<String>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc;
        try {
            doc = factory.newDocumentBuilder().parse(IOUtils.toInputStream(xmlContent, "UTF-8"));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            Object result = xpath.compile("//*[local-name() = '" + elementName + "']").evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                Node item = nodes.item(i);
                Node firstChild = item.getFirstChild();
                if (null != firstChild) {
                    String nodeValue = firstChild.getNodeValue();
                    if (StringUtils.isNotBlank(nodeValue)) {
                        ranks.add(valuePrefix + nodeValue + valueSuffix);
                    }
                }
            }

        } catch (Exception e) {
            throw new PropertyEnricherException("failed to handle response [" + xmlContent + "]", e);
        }
        return ranks;
    }

    public static String extractName(String xmlContent, String elementName) throws PropertyEnricherException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc;
        try {
            doc = factory.newDocumentBuilder().parse(IOUtils.toInputStream(xmlContent, "UTF-8"));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            Object result = xpath.compile("//*[local-name() = '" + elementName + "']").evaluate(doc, XPathConstants.STRING);
            return (String) result;
        } catch (Exception e) {
            throw new PropertyEnricherException("failed to handle response [" + xmlContent + "]", e);
        }
    }
}
