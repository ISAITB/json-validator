package eu.europa.ec.itb.json.utils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;

public class Utils {

    public static XMLGregorianCalendar getXMLGregorianCalendarDateTime() throws DatatypeConfigurationException {
        GregorianCalendar calendar = new GregorianCalendar();
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
    }

}
