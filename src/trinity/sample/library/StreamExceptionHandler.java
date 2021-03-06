package trinity.sample.library;

import com.sun.xml.internal.stream.events.EndElementEvent;
import com.sun.xml.internal.stream.events.StartElementEvent;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public class StreamExceptionHandler {

    static XMLInputFactory f = XMLInputFactory.newInstance();

    public XMLEvent hanleStreamException(XMLEventReader reader, XMLStreamException exception) {
        String message = exception.getMessage();
        String errorString = "The element type \"";
        XMLEvent event = null;
        if (message.contains(errorString)) {
            String tagName = message.substring(message.indexOf(errorString)
                    + errorString.length(), message.indexOf("\" must be terminated"));
            //lost of end tag
            if (message.contains("end-tag")) {
                event = newEndElement(tagName);
            }
        }
        return event;
    }

    public XMLEvent newStartElement(String tagName) {
        return new StartElementEvent(new QName(tagName));
    }

    public XMLEvent newEndElement(String tagName) {
        return new EndElementEvent(new QName(tagName));
    }
}
