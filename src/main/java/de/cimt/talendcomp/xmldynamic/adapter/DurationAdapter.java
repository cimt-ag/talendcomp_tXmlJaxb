package de.cimt.talendcomp.xmldynamic.adapter;

import java.util.Date;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeFactory;

/**
 * Uses Long as replacement for duration type. In most cases it should
 * be enough to handle these values by using the delta of time im milliseconds
 * @author dkoch
 */
public class DurationAdapter extends XmlAdapter<String, Long>{
    @Override
    public Long unmarshal(String v) throws Exception {
        if(v==null)
            return null;
        
        return DatatypeFactory.newInstance().newDuration(v).getTimeInMillis( new Date() );
    }

    @Override
    public String marshal(Long v) throws Exception {
        if(v==null)
            return null;
        
        return DatatypeFactory.newInstance().newDuration(v).toString();
    }
    
}
