package de.cimt.talendcomp.xmldynamic.filter;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.cimt.talendcomp.xmldynamic.Util;

/**
 * extracts schemainformations from wsdl
 *
 * @author dkoch
 */
public class WSDLSchemaFilter extends BaseFilter {

    private ContentHandler def = new DefaultHandler();
    private ContentHandler handler;
    private final Map<String, String> schemas = new HashMap<String, String>();

    private StringWriter writer = null;

    public WSDLSchemaFilter(){
        super.setContentHandler(def);
    }

    protected synchronized String createInputSource(String xmlbuffer) throws RuntimeException{
        return Util.uniqueString() + ".xsd";
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        final String name = toLocalName(localName, qName);

        if (equalUris(W3C_XML_SCHEMA_NS_URI, uri) && "schema".equals(name)) {
            try {
                SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
                TransformerHandler transform = stf.newTransformerHandler();
                writer = new StringWriter();
                transform.setResult(new StreamResult(writer));
                def = transform;
                prefixmapping.put("$TNS", atts.getValue("targetNamespace"));

                super.setContentHandler(def);
                def.startDocument();
                for (Entry<String, String> mapping : prefixmapping.entrySet()) {
                    if (mapping.getKey().startsWith("$")) {
                        continue;
                    }
                    transform.startPrefixMapping(mapping.getKey(), mapping.getValue());
                }
            } catch (TransformerConfigurationException ex) {
            	LOG.error(ex.getLocalizedMessage(), ex);
            }
        }
        super.startElement(uri, localName, qName, atts);
    }

    @Override
    public synchronized void endElement(String uri, String localName, String qName) throws SAXException {
        final String name = toLocalName(localName, qName);
        super.endElement(uri, localName, qName);

        if (equalUris(W3C_XML_SCHEMA_NS_URI, uri) && "schema".equals(name)) {
            for (String prefix : prefixmapping.keySet()) {
                if (prefix.startsWith("$")) {
                    continue;
                }
                def.endPrefixMapping(prefix);
            }
            def.endDocument();
            def = new DefaultHandler();
            super.setContentHandler(def);
            try {
                writer.close();
            } catch (IOException ex) {
            }
            schemas.put(prefixmapping.get("$TNS"), writer.toString());
        }
    }

    @Override
    public void endDocument() throws SAXException, RuntimeException {
        try {
            if (schemas.size() < 1) {
                super.endDocument();
                return;
            }

            synchronized(this){
                final Transformer transformer = TransformerFactory.newInstance().newTransformer();
                boolean first=true;
                for (Entry<String, String> entry : schemas.entrySet()) {
                    if (first) {
                        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                        transformer.transform(new StreamSource(new StringReader(schemas.get(prefixmapping.get("$TNS")))), new SAXResult(handler));
                        first=false;
                    } else {
                        createInputSource(entry.getValue());
                    }
                }

                super.endDocument();

            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public ContentHandler getContentHandler() {
        return this;
    }

    @Override
    public void setContentHandler(ContentHandler handler) {
        this.handler = handler;
    }

}
