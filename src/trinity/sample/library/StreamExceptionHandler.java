package trinity.sample.library;

import com.sun.xml.internal.stream.events.EndElementEvent;
import com.sun.xml.internal.stream.events.StartElementEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
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

    public void skipThisElement(XMLStreamReader reader) {
        try {
            XMLEventReader eventReader = f.createXMLEventReader(reader);
            skipThisElement(eventReader);
        } catch (XMLStreamException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void skipThisElement(XMLEventReader reader) {
        reader.next();
    }

    public XMLEvent newStartElement(String tagName) {
        return new StartElementEvent(new QName(tagName));
    }

    public XMLEvent newEndElement(String tagName) {
        return new EndElementEvent(new QName(tagName));
    }
}
