package de.cimt.talendcomp.xmldynamic.filter;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.cimt.talendcomp.xmldynamic.InlineSchemaPlugin;
import de.cimt.talendcomp.xmldynamic.Pair;

/**
 *
 * @author dkoch
 */
public class PluginFilter extends BaseFilter {

    public static final QName JAXB = new QName("https://jakarta.ee/xml/ns/jaxb", "jaxb", "jaxb");
    public static final QName XJC  = new QName("https://jakarta.ee/xml/ns/jaxb/xjc", "xjc", "xjc");
    private class ElementStored{
        String uri;
        String prefix;
        AttributesImpl attributes;
        
    }
    ElementStored elementBlock   =null;

    public boolean testManipulationRequired(Pair<String, String> fqtype){
        return false;
    };

    /**
     * Computes for given type the fully qualified type.
     *
     * @param type the pair from uri and name of type
     * @return
     */
    private Pair<String, String> solve(String type) {
        int pos = type.indexOf(":");
        return (pos < 0)
                ? new Pair<String, String>(prefixmapping.get(""), type)
                : new Pair<String, String>(prefixmapping.get(type.substring(0, pos)), type.substring(pos + 1));

    }

    @Override
    public void startDocument() throws SAXException {
        prefixmapping.clear();
        super.startDocument();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        final boolean schemaUri = equalUris(XMLConstants.W3C_XML_SCHEMA_NS_URI, uri);
        String name = toLocalName(localName, qName);
        if (!schemaUri) {
            super.startElement(uri, localName, qName, atts);
            return;
        }
        
        if (name.equalsIgnoreCase("schema")) {
            /**
             * manipulate xml content for schema declarations - ensure namespace
             * prefixmapping for JAXB and XJC is present - add or extend
             * extensionBindingPrefixes to use activate InlineSchemaPlugin
             */
            // <editor-fold defaultstate="collapsed" desc="manipulate schema elements">                          
            String tns = atts.getValue("targetNamespace");
            if (tns!=null && !prefixmapping.containsKey("$TNS")) {
                prefixmapping.put("$TNS", tns);
            }
            startPrefixMapping(InlineSchemaPlugin.PNS.getPrefix(), InlineSchemaPlugin.PNS.getNamespaceURI());

            String jaxbPrefix = getPrefixForUrl(JAXB.getNamespaceURI());
            if (jaxbPrefix == null) {
                jaxbPrefix = composePrefix(JAXB.getNamespaceURI(), JAXB.getPrefix());
                startPrefixMapping(jaxbPrefix, JAXB.getNamespaceURI());
            }
            String xjcPrefix = getPrefixForUrl(XJC.getNamespaceURI());
            if (xjcPrefix == null) {
                xjcPrefix = composePrefix(XJC.getNamespaceURI(), XJC.getPrefix());
                startPrefixMapping(xjcPrefix, XJC.getNamespaceURI());
            }

            AttributesImpl impl = new AttributesImpl();
            boolean hasJaxbBindingPrefixes = false;

            for (int i = 0, max = atts.getLength(); i < max; i++) {
                if (atts.getLocalName(i).equalsIgnoreCase("extensionBindingPrefixes") && atts.getURI(i).equals(JAXB.getNamespaceURI())) {
                    hasJaxbBindingPrefixes = true;
                    String value = atts.getValue(i);
                    if (value.contains(xjcPrefix) == false) {
                        value += " " + xjcPrefix;
                    }
                    if (value.contains(InlineSchemaPlugin.PNS.getPrefix()) == false) {
                        value += " " + InlineSchemaPlugin.PNS.getPrefix();
                    }
                    impl.addAttribute(
                            atts.getURI(i),
                            atts.getLocalName(i),
                            atts.getQName(i),
                            atts.getType(i),
                            value);
                } else {
                    impl.addAttribute(
                            atts.getURI(i),
                            atts.getLocalName(i),
                            atts.getQName(i),
                            atts.getType(i),
                            atts.getValue(i));
                }
            }
            if (hasJaxbBindingPrefixes == false) {
//                impl.addAttribute(JAXB.getNamespaceURI(), "extensionBindingPrefixes", JAXB.getPrefix() + ":extensionBindingPrefixes", "CDATA", InlineSchemaPlugin.PNS.getPrefix());
//                impl.addAttribute(JAXB.getNamespaceURI(), "extensionBindingPrefixes", "extensionBindingPrefixes", "CDATA", InlineSchemaPlugin.PNS.getPrefix());
            }
            super.startElement(uri, localName, qName, impl);
            // </editor-fold> 
        } else if (name.equalsIgnoreCase("element")) {
            /**
             * change xml content for elements using a special type
             * testOverwriteRequired returning true
             */
            // <editor-fold defaultstate="collapsed" desc="change xml content">                          
            String prefix = getPrefixForUrl(uri);
            prefix = (prefix == null || prefix.length()==0) ? "" : (prefix + ":");
            String type = null;
            AttributesImpl impl = new AttributesImpl();

            /**
             * copy each attribute without changes but type when testManipulationRequired returns true.
             * in that case skip this attribute and create annonymous complex type extending found type
             */
            
            for (int i = 0, max = atts.getLength(); i < max; i++) {
                String cname = atts.getLocalName(i);
                if (cname == null) {
                    cname = atts.getQName(i);
                }

                if (cname.equalsIgnoreCase("type") && testManipulationRequired(solve(atts.getValue(i)))) {
                    type = atts.getValue(i);
                    continue;
                }
                
                impl.addAttribute(
                        atts.getURI(i),
                        atts.getLocalName(i),
                        atts.getQName(i),
                        atts.getType(i),
                        atts.getValue(i));
            }
            super.startElement(uri, localName, qName, impl);

            if (type != null) {
                elementBlock = new ElementStored();
                
                impl = new AttributesImpl();
                impl.addAttribute("", "base", "base", "string", type);
                elementBlock.uri=uri;
                elementBlock.prefix=prefix;
                elementBlock.attributes=impl;
                
             
            }
            // </editor-fold> 
        } else if (name.equalsIgnoreCase("complexType")) {
            String cname = atts.getValue("name");
            AttributesImpl impl = new AttributesImpl(atts);
            if (testManipulationRequired(new Pair("$TNS", cname)) && impl.getIndex("abstract") < 0) {
                impl.addAttribute("", "abstract", "abstract", "CDATA", "true");
            }
            super.startElement(uri, localName, qName, atts);
        } else {
            super.startElement(uri, localName, qName, atts);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {    
        final boolean schemaUri = equalUris(XMLConstants.W3C_XML_SCHEMA_NS_URI, uri);
        String name = toLocalName(localName, qName);
        if (!schemaUri) {
            super.endElement(uri, localName, qName);
            return;
        }
        
        if ( (name.equalsIgnoreCase("annotation") || name.equalsIgnoreCase("element")) && elementBlock!=null) {
            
            if(name.equalsIgnoreCase("annotation"))
                super.endElement(uri, localName, qName);
            
            super.startElement(elementBlock.uri, "complexType", elementBlock.prefix + "complexType", new AttributesImpl());
            super.startElement(elementBlock.uri, "complexContent", elementBlock.prefix + "complexContent", new AttributesImpl());
            super.startElement(elementBlock.uri, "extension", elementBlock.prefix + "extension", elementBlock.attributes);
            super.endElement(elementBlock.uri, "extension", elementBlock.prefix + "extension");
            super.endElement(elementBlock.uri, "complexContent", elementBlock.prefix + "complexContent");
            super.endElement(elementBlock.uri, "complexType", elementBlock.prefix + "complexType");
            elementBlock=null;
            
            if(name.equalsIgnoreCase("annotation"))
                return;
        } 
        
        super.endElement(uri, localName, qName);
        
    
    }

}
