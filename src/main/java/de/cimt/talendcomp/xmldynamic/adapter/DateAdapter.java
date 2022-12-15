package de.cimt.talendcomp.xmldynamic.adapter;

import java.util.Calendar;
import java.util.Date;
import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 * @author dkoch
 */
public class DateAdapter extends XmlAdapter<String, Date>{

    @Override
    public Date unmarshal(String v) {
        if(v==null)
            return null;
        return DatatypeConverter.parseDate(v).getTime();
    }

    @Override
    public String marshal(Date v) {
        if(v==null)
            return null;
        Calendar cal=Calendar.getInstance();
        cal.setTime(v);
        return DatatypeConverter.printDate(cal);
    }
    
}
