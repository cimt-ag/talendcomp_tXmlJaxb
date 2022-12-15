package de.cimt.talendcomp.xmldynamic.adapter;

import java.util.Calendar;
import java.util.Date;
import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 * @author dkoch
 */
public class DateTimeAdapter extends XmlAdapter<String, Date>{

    @Override
    public Date unmarshal(String v) throws Exception {
        if(v==null)
            return null;
        
        Date d;
        v=v.trim();
        if(!v.toUpperCase().contains("T")){
            // date without time but timezone available
            if(v.indexOf("+")>0 || v.indexOf("-")>0 ){
                int p=Math.max(v.indexOf("+"), v.indexOf("-"));
                v=v.substring(0, p) + "T00:00:00" + v.substring(p);
            } else {
                v=v+"T00:00:00";
            }
        }
        
        
        return DatatypeConverter.parseDateTime(v).getTime();
    }

    @Override
    public String marshal(Date v) throws Exception {
        if(v==null)
            return null;
        Calendar cal=Calendar.getInstance();
        cal.setTime(v);
        return DatatypeConverter.printDate(cal);
    }
    
}
