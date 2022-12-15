package de.cimt.talendcomp.xmldynamic.filter;

import java.net.URI;
import javax.xml.XMLConstants;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * checks definitions for references to other definitions. these may/can be 
 * relocated to a new schemalocation
 * @author dkoch
 */
public class DependencyFilter extends BaseFilter {
    final URI root;
    
    public DependencyFilter(URI root){
        this.root=root;
    }
    
    protected String getRelocatedSchemaLocation(String location){
        return getRelocatedSchemaLocation(root, location);
    }
 
    protected String getRelocatedSchemaLocation(URI root, String location) throws IllegalArgumentException{
        if (location == null || location.length() == 0) {
            return null;
        }
        try {
            URI nestedUri = new URI(location);

            if (!nestedUri.isAbsolute()) {
                nestedUri = root.resolve(nestedUri);
            }

            return nestedUri.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (equalUris(XMLConstants.W3C_XML_SCHEMA_NS_URI, uri)) {
            final String n = toLocalName(localName, qName);
            if (n.equals("include") || n.equals("import") || n.equals("redefine")) {
                AttributesImpl atts2 = new AttributesImpl(atts);
                String location = getRelocatedSchemaLocation( atts.getValue("schemaLocation") );
                if (location != null) {
                    atts2.setValue(atts2.getIndex("schemaLocation"), location);
                } else {
                    // only possble for imports in combination with catalogs, well known namespaces 
                    // or absolute (accessible) urls as namespace 
                    location = getRelocatedSchemaLocation( atts.getValue("namespace") );
                    if (location != null) {
                        atts2.addAttribute("", "schemaLocation", "", "CDATA", getRelocatedSchemaLocation(location) );
                    }
                }
                atts=atts2;
            }
            
        }
        super.startElement(uri, localName, qName, atts);
    }
}
