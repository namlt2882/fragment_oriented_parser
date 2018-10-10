/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trinity.sample.library;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
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

    public static void main(String[] args) throws XMLStreamException, FileNotFoundException {
        LibraryNotWellFormFilter filter = new LibraryNotWellFormFilter();
        XMLInputFactory factory = XMLInputFactory.newFactory();
        XMLStreamReader streamReader = factory.createXMLStreamReader(new FileReader("src/library.xml"));
//        streamReader = factory.createFilteredReader(streamReader, filter);

        LibraryStaxParser parser = new LibraryStaxParser();
        try {
            while (streamReader.hasNext()) {
                streamReader.next();
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

    public void parseBooks(XMLStreamReader streamReader) throws XMLStreamException {
        List<Writer> writers = getXmlFragments(streamReader, "books", "book");
        writers.forEach(System.out::println);
    }

    public void parseEmployees(XMLStreamReader streamReader) throws XMLStreamException {
        List<Writer> writers = getXmlFragments(streamReader, "employees", "employee");
        writers.forEach(System.out::println);
    }

    public static List<Writer> getXmlFragments(XMLStreamReader streamReader, String endTag, String fragmentRootTag) throws XMLStreamException {
        List<Writer> rs = new ArrayList<>();
        boolean inFragment = false;
        String tagName;
        int cursor = 0;
        XMLInputFactory f = XMLInputFactory.newInstance();
        XMLEventReader xr;
        while (streamReader.hasNext()) {
            try {
                cursor = streamReader.next();
                if (cursor == XMLStreamConstants.START_ELEMENT) {
                    int c = streamReader.getLocation().getColumnNumber();
                    int l = streamReader.getLocation().getLineNumber();
                    System.out.println("Start offset: [" + l + "," + c + "]");
                    tagName = streamReader.getLocalName();
                    if (tagName.equals(fragmentRootTag)) {
                        inFragment = true;
                    }
                } else if (cursor == XMLStreamConstants.END_ELEMENT) {
                    int c = streamReader.getLocation().getColumnNumber();
                    int l = streamReader.getLocation().getLineNumber();
                    System.out.println("End offset: [" + l + "," + c + "]");
                    tagName = streamReader.getLocalName();
                    if (tagName.equals(endTag)) {
                        break;
                    }
                }
                if (inFragment) {
                    try {
                        rs.add(getXmlFragment(streamReader, fragmentRootTag));
                    } catch (XMLStreamException ex) {
                        foundException = true;
                    }
                    inFragment = false;
                }
            } catch (XMLStreamException ex) {
                int c = streamReader.getLocation().getColumnNumber();
                int l = streamReader.getLocation().getLineNumber();
                int off = streamReader.getLocation().getCharacterOffset();
                System.out.println("Offset: " + off);
                System.out.println("End offset: [" + l + "," + c + "]");
                Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
                if (ex.getMessage().contains("end-tag")) {
                    xr = f.createXMLEventReader(streamReader);
                    xr.peek();
                }
            } catch (Exception ex) {
                break;
            }
        }
        return rs;
    }

    public static Writer getXmlFragment(XMLStreamReader streamReader, String parentTag) throws XMLStreamException {
        StringWriter sw = new StringWriter();
        XMLOutputFactory of = XMLOutputFactory.newInstance();
        XMLEventWriter xw = null;
        XMLInputFactory f = XMLInputFactory.newInstance();
        XMLEventReader xr = f.createXMLEventReader(streamReader);

        while (xr.hasNext()) {
            XMLEvent e = xr.nextEvent();
            if (e.isStartElement()
                    && ((StartElement) e).getName().getLocalPart().equals(parentTag)) {
                xw = of.createXMLEventWriter(sw);
            } else if (e.isEndElement()
                    && ((EndElement) e).getName().getLocalPart().equals(parentTag)) {
                break;
            } else if (xw != null) {
                xw.add(e);
            }
        }
        return sw;
    }

}
