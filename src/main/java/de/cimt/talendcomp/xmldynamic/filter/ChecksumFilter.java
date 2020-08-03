package de.cimt.talendcomp.xmldynamic.filter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *
 * @author dkoch
 */
public class ChecksumFilter extends BaseFilter {

    final MessageDigest digest;

    public ChecksumFilter(){
        MessageDigest nmd = null;
        try {
            nmd = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException ex) {
            LOG.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }
        digest = nmd;
    }

    private void update(String... values) {
        if (digest == null) {
            return;
        }
        for (String value : values) {
            digest.update((value == null ? "-" : value).getBytes());
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        update(new String(ch, start, length));
        super.characters(ch, start, length);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        update(uri, localName, qName);
        for (int i = 0, max = atts.getLength(); i < max; i++) {
            update(atts.getLocalName(i), atts.getQName(i), atts.getValue(i));
        }
        super.startElement(uri, localName, qName, atts);
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        update(uri, prefix);
        super.startPrefixMapping(prefix, uri);
    }

    @Override
    public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
        update(name, publicId, systemId, notationName);
        super.unparsedEntityDecl(name, publicId, systemId, notationName);
    }

    @Override
    public String toString(){
        return new HexBinaryAdapter().marshal(digest.digest());
    }

}
