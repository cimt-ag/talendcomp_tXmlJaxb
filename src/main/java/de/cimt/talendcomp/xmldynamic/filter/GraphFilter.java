package de.cimt.talendcomp.xmldynamic.filter;

import java.util.Arrays;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.cimt.talendcomp.xmldynamic.ReflectUtil;

/**
 * class to allow printing of document graphs. this is the initial version and 
 * must be enhanced to display more informations, because currently the output
 * is purly usable for people who already examined the definitions
 * 
 * <ul>
 *  <li>Tpyes of Elements/Attributes</li>
 *  <li>Namespaces für References and Types</li>
 *  <li>Manadatory Elements</li>
 * </ul>
 * @author dkoch
 */
public class GraphFilter extends BaseFilter {
    
    private final String refMarker = "#";
    private final String attMarker = "@";
    private final String cpxMarker = "$";

    StringBuilder buffer = new StringBuilder();
    int tabs = 0;
    Stack<Integer> breadcrumb = new Stack<>();

    private String spaces() {
        char[] spaces = new char[tabs];
        Arrays.fill(spaces, '\t');
        return new String(spaces);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        tabs -= breadcrumb.pop();
        super.endElement(uri, localName, qName);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        final String name = toLocalName(localName, qName);
        int step = 0;
        if (name.equals("element")) {
            if (atts.getValue("ref") != null) {
                buffer.append(spaces()).append(refMarker).append(atts.getValue("ref")).append("\n");
            } else {
                if (tabs == 0) 
                    buffer.append("\n");
                
                step = 1;
                buffer.append(spaces()).append(atts.getValue("name")).append("\n");
            }
        } else if (name.equals("attribute")) {
            buffer.append(spaces()).append(attMarker).append(atts.getValue("name")).append("\n");
        } else if (name.equals("complexType")) {
            if (atts.getValue("name") != null) {
                if (tabs == 0) 
                    buffer.append("\n");
                
                step = 1;
                buffer.append(spaces()).append(cpxMarker).append(atts.getValue("name")).append("\n");
            }
        }
        breadcrumb.push(step);
        tabs += step;
        super.startElement(uri, localName, qName, atts);
    }

    @Override
    public void startDocument() throws SAXException {
        buffer = new StringBuilder();
        super.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
        LOG.info(buffer.toString());
    }

}
