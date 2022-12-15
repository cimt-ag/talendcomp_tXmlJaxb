package de.cimt.talendcomp.xmldynamic.filter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 *
 * @author dkoch
 */
public class PrintingFilter extends XMLFilterImpl {

    StringBuffer buffer;
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        buffer.append( "</"  ).append( qName.length()>0 ? qName : localName ).append( ">\n"  );
        super.endElement(uri, localName, qName); 
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        buffer.append( "<"  ).append( qName.length()>0 ? qName : localName );
        for(int i=0, max=atts.getLength(); i<max; i++)
            buffer.append( " " ).append(atts.getLocalName(i)).append("=\"").append( atts.getValue(i) ).append("\"");
        
        buffer.append( ">\n"  );
        super.startElement(uri, localName, qName, atts); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument(); 
        System.err.println("\n"+ buffer.toString() );
    }
    @Override
    public void startDocument() throws SAXException {
        buffer=new StringBuffer();
        super.startDocument(); 
    }

}
