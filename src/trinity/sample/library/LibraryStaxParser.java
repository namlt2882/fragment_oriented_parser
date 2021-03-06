/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trinity.sample.library;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
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
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import trinity.sample.library.book.Book;
import trinity.sample.library.book.BookErrorDataParser;
import trinity.sample.library.employee.Employee;
import trinity.sample.library.employee.EmployeeErrorDataParser;

/**
 *
 * @author ADMIN
 */
public class LibraryStaxParser {

    private XMLStreamReader streamReader;
    private XMLInputFactory f = XMLInputFactory.newInstance();
    private Reader reader;
    private StreamExceptionHandler exceptionHandler = new StreamExceptionHandler();
    private static XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    private List<Book> bookRs;
    private List<Book> bookErrRs;
    private List<Employee> employeeRs;
    private List<Employee> employeeErrRs;

    public List<Book> getBookRs() {
        return bookRs;
    }

    public List<Book> getBookErrRs() {
        return bookErrRs;
    }

    public List<Employee> getEmployeeRs() {
        return employeeRs;
    }

    public List<Employee> getEmployeeErrRs() {
        return employeeErrRs;
    }

    public static void main(String[] args) throws XMLStreamException,
            FileNotFoundException {
        LibraryStaxParser parser = new LibraryStaxParser();
        parser.parse();
        System.out.println("Book data----");
        for (Book book : parser.getBookRs()) {
            System.out.println(book);
        }
        System.out.println("Book error data----");
        for (Book book : parser.getBookErrRs()) {
            System.out.println(book);
        }

        System.out.println("\nEmployee data----");
        for (Employee employee : parser.getEmployeeRs()) {
            System.out.println(employee);
        }
        System.out.println("Employee error data----");
        for (Employee employee : parser.getEmployeeErrRs()) {
            System.out.println(employee);
        }
    }

    private Reader getGoodNestedTagFromFile(String fileName, String rootTag)
            throws FileNotFoundException, IOException {
        FileReader fr = new FileReader(fileName);
        StringBuilder sb = new StringBuilder();
        int c;
        while (true) {
            c = fr.read();
            if (c == -1) {
                break;
            }
            sb.append((char) c);
        }
        return new StringReader(getGoodNestedTagFromString(sb.toString(), rootTag));
    }

    private String getGoodNestedTagFromString(String notFormatedString, String rootTag)
            throws FileNotFoundException, IOException {
        StringBuilder sb = new StringBuilder();
        List<String> newRs = null;
        newRs = NestedTagResolver.formatNestedTag(notFormatedString, rootTag);
        for (String resource : newRs) {
            sb.append(resource);
        }
        String rs = sb.toString();
        return rs;
    }

    public void parse() {
        try {
            reader = getGoodNestedTagFromFile("src/library.xml", "library");
            streamReader = f.createXMLStreamReader(reader);

            List<String> bookResource = null;
            List<String> employeeResource = null;
            SchemaValidator validator = new SchemaValidator();
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
                        bookResource = parseBooks(streamReader);
                    } else if ("employees".equals(elementName)) {
                        employeeResource = parseEmployees(streamReader);
                    }
                }
            }
            try {
                //validate book data with schema
                List<String> notValidateBookResource = validator.validate(bookResource, Book.class);
                bookRs = validator.getResultList(); //good book result
                //parse error book data by SAX parser and collect data
                BookErrorDataParser bedp = new BookErrorDataParser();
                for (String string : notValidateBookResource) {
                    parseBySaxHandler(string, bedp);
                }
                bookErrRs = bedp.getResult();
            } catch (JAXBException | SAXException e) {
                throw new Exception("Error when validate Book");
            }

            try {
                //validate employee data with schema
                List<String> notValidateEmployeeResource = validator.validate(employeeResource, Employee.class);
                employeeRs = validator.getResultList(); //good employee result
                //parse error book data by SAX parser and collect data
                EmployeeErrorDataParser eedp = new EmployeeErrorDataParser();
                for (String string : notValidateEmployeeResource) {
                    parseBySaxHandler(string, eedp);
                }
                employeeErrRs = eedp.getResult();
            } catch (JAXBException | SAXException e) {
                throw new Exception("Error when validate Employee");
            }
        } catch (Exception ex) {
            Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public List parseBySaxHandler(String str, DefaultHandler handler) {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser;
        try {
            parser = spf.newSAXParser();
            parser.parse(SchemaValidator.createInputSource(str), handler);
        } catch (Exception ex) {
            Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ((SAXErrorDataParser) handler).getResult();
    }

    public String convertXMLEventToString(List<XMLEvent> list, String rootTag) {
        StringWriter sr = new StringWriter();
        XMLEventWriter xew;
        try {
            xew = outputFactory.createXMLEventWriter(sr);
            for (XMLEvent event : list) {
                xew.add(event);
            }
            xew.flush();
            xew.close();
        } catch (XMLStreamException ex) {
            Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        String s = sr.toString();
        try {
            s = getGoodNestedTagFromString(s, rootTag);
            System.out.println(s);
            return s;
        } catch (IOException ex) {
            Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    public List<String> convertEventListToStringList(List<List<XMLEvent>> eventLists, String rootTag) {
        List<String> rs = new LinkedList<>();
        for (List<XMLEvent> eventList : eventLists) {
            rs.add(convertXMLEventToString(eventList, rootTag));
        }
        return rs;
    }

    public List<String> parseBooks(XMLStreamReader streamReader) {
        List<List<XMLEvent>> eventLists;
        try {
            eventLists = getXmlFragments(streamReader, "books", "book");
            return convertEventListToStringList(eventLists, "book");
        } catch (XMLStreamException ex) {
            Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }

    public List<String> parseEmployees(XMLStreamReader streamReader) {
        List<List<XMLEvent>> eventLists;
        try {
            eventLists = getXmlFragments(streamReader, "employees", "employee");
            return convertEventListToStringList(eventLists, "employee");
        } catch (XMLStreamException ex) {
            Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }

    public List<List<XMLEvent>> getXmlFragments(XMLStreamReader streamReader, String endTag,
            String fragmentRootTag) throws XMLStreamException {
        List<List<XMLEvent>> rs = new ArrayList<>();
        String tagName;
        int cursor = 0;
        while (streamReader.hasNext()) {
            try {
                cursor = streamReader.next();
                if (cursor == XMLStreamConstants.START_ELEMENT) {
                    tagName = streamReader.getLocalName();
                    if (tagName.equals(fragmentRootTag)) {
                        try {
                            getXmlFragment(streamReader, fragmentRootTag, rs);
                        } catch (XMLStreamException ex) {
                        }
                    }
                } else if (cursor == XMLStreamConstants.END_ELEMENT) {
                    tagName = streamReader.getLocalName();
                    if (tagName.equals(endTag)) {
                        break;
                    }
                }
            } catch (XMLStreamException ex) {
                Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                break;
            }
        }
        return rs;
    }

    public void getXmlFragment(XMLStreamReader streamReader, String rootTag,
            List<List<XMLEvent>> rs) throws XMLStreamException {
        XMLEventReader xr = f.createXMLEventReader(streamReader);
        boolean found = false;
        List<XMLEvent> tmp = null;
        while (xr.hasNext()) {
            XMLEvent e = null;
            e = xr.nextEvent();
            if (e.isStartElement()
                    && ((StartElement) e).getName().getLocalPart().equals(rootTag)) {
                if (found) {
                    tmp.add(exceptionHandler.newEndElement(rootTag));
                    rs.add(tmp);
                    break;
                } else {
                    found = true;
                    tmp = new ArrayList<>();
                }
            } else if (e.isEndElement()
                    && ((EndElement) e).getName().getLocalPart().equals(rootTag)) {
                tmp.add(e);
                rs.add(tmp);
                break;
            }
            if (e != null) {
                if (!e.isCharacters() || (e.isCharacters() && !e.asCharacters().isWhiteSpace())) {
                    tmp.add(e);
                }
            }
        }
    }

}
