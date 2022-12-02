package de.cimt.talendcomp.xmldynamic.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 *
 * @author dkoch
 */
public class BaseFilter extends XMLFilterImpl {
    protected static final Logger LOG = LoggerFactory.getLogger("de.cimt.talendcomp.xmldynamic");

    protected Map<String, String> prefixmapping = new HashMap<String, String>();

    static String toLocalName(String localName, String qName) {
        return (localName != null && localName.length() > 0) ? localName : qName.substring(qName.indexOf(":") + 1);
    }

    static String uniqueString() {
        return UUID.randomUUID().toString().replaceAll("[:\\.-]+", "");
    }

    public String composePrefix(String url, String prefered) {
        if (url != null && prefixmapping.containsValue(url)) {
            return getPrefixForUrl(url);
        }
        if (prefixmapping.containsKey(prefered)) {
            int count = 0;
            while (prefixmapping.containsKey(prefered + count)) {
                count++;
            }
            prefered=prefered + count;
        }
        prefixmapping.put(prefered, url);
        return prefered;
    }

    public String getPrefixForUrl(String url) {
        for (Map.Entry<String, String> pair : prefixmapping.entrySet()) {
            if (pair.getValue().equalsIgnoreCase(url)) {
                return pair.getKey();
            }
        }
        return null;
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        prefixmapping.remove(prefix);
        super.endPrefixMapping(prefix);
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        prefixmapping.put(prefix, uri);
        super.startPrefixMapping(prefix, uri);
    }

    boolean equalUris(String uri1, String uri2) {
    	return ( (uri1 != null && uri1.isEmpty() == false) ? uri1 : (prefixmapping.containsKey("") ? prefixmapping.get("") : "") )
    			.equals( (uri2 != null && uri2.isEmpty() == false) ? uri2 : prefixmapping.get("") );
    }

}
