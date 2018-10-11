/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trinity.sample.library;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author ADMIN
 */
public class LibraryStaxParser {

    private static boolean foundException = false;
    static XMLStreamReader streamReader;
    static XMLInputFactory f = XMLInputFactory.newInstance();
    static FileReader reader;
    static StreamExceptionHandler exceptionHandler = new StreamExceptionHandler();

    public static void main(String[] args) throws XMLStreamException, FileNotFoundException {
        LibraryNotWellFormFilter filter = new LibraryNotWellFormFilter();
        reader = new FileReader("src/library.xml");
        streamReader = f.createXMLStreamReader(reader);
        
        LibraryStaxParser parser = new LibraryStaxParser();
        try {
            while (streamReader.hasNext()) {
                try {
                    streamReader.next();
                } catch (XMLStreamException e) {
                    Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, e);
                } catch (Exception ex) {
                    break;
                }
                if (streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
                    String elementName = streamReader.getLocalName();
                    if ("books".equals(elementName)) {
                        parser.parseBooks(streamReader);
                    } else if ("employees".equals(elementName)) {
                        parser.parseEmployees(streamReader);
                    }
                }
            }
        } finally {
            int c = streamReader.getLocation().getColumnNumber();
            int l = streamReader.getLocation().getLineNumber();
            System.out.println("End offset: [" + l + "," + c + "]");
        }
    }

    public InputStream convertXMLEventToInputSource(List<XMLEvent> list) {
        StringBuilder builder = new StringBuilder();
        list.forEach(e -> builder.append(e));
        InputStream stream = null;
        try {
            stream = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        return stream;
    }

    public InputStream parseBooks(XMLStreamReader streamReader) {
        List<XMLEvent> writers;
        try {
            writers = getXmlFragments(streamReader, "books", "book");
            return convertXMLEventToInputSource(writers);
        } catch (XMLStreamException ex) {
            Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public InputStream parseEmployees(XMLStreamReader streamReader) {
        List<XMLEvent> writers;
        try {
            writers = getXmlFragments(streamReader, "employees", "employee");
            return convertXMLEventToInputSource(writers);
        } catch (XMLStreamException ex) {
            Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public List<XMLEvent> getXmlFragments(XMLStreamReader streamReader, String endTag, String fragmentRootTag) throws XMLStreamException {
        List<XMLEvent> rs = new ArrayList<>();
        boolean inFragment = false;
        String tagName;
        int cursor = 0;
        XMLInputFactory f = XMLInputFactory.newInstance();
        XMLEventReader xr;
        while (streamReader.hasNext()) {
            try {
                cursor = streamReader.next();
                if (cursor == XMLStreamConstants.START_ELEMENT) {
                    tagName = streamReader.getLocalName();
                    if (tagName.equals(fragmentRootTag)) {
                        inFragment = true;
                    }
                } else if (cursor == XMLStreamConstants.END_ELEMENT) {
                    tagName = streamReader.getLocalName();
                    if (tagName.equals(endTag)) {
                        break;
                    }
                }
                if (inFragment) {
                    try {
                        getXmlFragment(streamReader, fragmentRootTag).forEach(e -> rs.add(e));
                    } catch (XMLStreamException ex) {
                        foundException = true;
                    }
                    inFragment = false;
                }
            } catch (XMLStreamException ex) {
                Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
//                exceptionHandler.skipThisElement(s);
            } catch (Exception ex) {
                break;
            }
        }
        return rs;
    }

    public List<XMLEvent> getXmlFragment(XMLStreamReader streamReader, String parentTag) throws XMLStreamException {
        List<XMLEvent> sw = new ArrayList<>();
        XMLOutputFactory of = XMLOutputFactory.newInstance();
        XMLEventReader xr = f.createXMLEventReader(streamReader);
        boolean found = false;
        while (xr.hasNext()) {
            XMLEvent e = null;
            try {
                e = xr.nextEvent();
            } catch (XMLStreamException ex) {
                e = exceptionHandler.hanleStreamException(xr, ex);
            }
            if (e.isStartElement()
                    && ((StartElement) e).getName().getLocalPart().equals(parentTag)) {
                if (found) {
                    sw.add(exceptionHandler.newEndElement(parentTag));
                    break;
                } else {
                    found = true;
                }
            } else if (e.isEndElement()
                    && ((EndElement) e).getName().getLocalPart().equals(parentTag)) {
                sw.add(e);
                break;
            }
            if (e != null) {
                if (!e.isCharacters() || (e.isCharacters() && !e.asCharacters().isWhiteSpace())) {
                    sw.add(e);
                }
            }
        }
        return sw;
    }

}
