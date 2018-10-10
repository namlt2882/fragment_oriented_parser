/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trinity.sample.library;

import javax.xml.stream.EventFilter;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author ADMIN
 */
public class LibraryNotWellFormFilter implements StreamFilter {

    int eventType;
    String localName;

    public LibraryNotWellFormFilter() {
        setDefault();
    }

    public void setDefault() {
        eventType = -1;
        localName = null;
    }

//    @Override
//    public boolean accept(XMLEvent event) {
//        if (eventType == event.getEventType()) {
//            if (localName != null && event.getSchemaType().getLocalPart().equals(localName)) {
//                return false;
//            }
//        }
//        return true;
//    }
    @Override
    public boolean accept(XMLStreamReader reader) {
        return true;
    }

}
