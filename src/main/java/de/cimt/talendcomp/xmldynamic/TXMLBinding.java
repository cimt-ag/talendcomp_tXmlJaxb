package de.cimt.talendcomp.xmldynamic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchema;
import javax.xml.namespace.QName;

/**
 * Provides Information about generated Content
 * @author dkoch
 * 
 * 
 * 
 * 
 * 
 */
public interface TXMLBinding {
    public static String ANYNAMESPACE="##any";
    public List<Class<TXMLObject>> getClasses();
    public Class<TXMLObject>[] getElements();
    public Class<TXMLObject>[] getTypes();
    public String[] getNamespaces();
    public long getTimestamp();
    
    public boolean isMember(QName qn);
    
    /**
     * Searches for a class implementing the given qname. 
     * @param qn name and namespace of the element implementation to be searched for
     * @return the implementing class or null if not found
     */
    public Class<TXMLObject> find(QName qn);
}

