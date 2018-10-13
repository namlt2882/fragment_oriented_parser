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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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

    private static boolean foundException = false;
    static XMLStreamReader streamReader;
    static XMLInputFactory f = XMLInputFactory.newInstance();
    static Reader reader;
    static StreamExceptionHandler exceptionHandler = new StreamExceptionHandler();
    static XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
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

    public static void main(String[] args) throws XMLStreamException, FileNotFoundException {
        LibraryStaxParser parser = new LibraryStaxParser();
        parser.parse();
        System.out.println("Book data----");
        parser.getBookRs().forEach(System.out::println);
        System.out.println("Book error data----");
        parser.getBookErrRs().forEach(System.out::println);
        System.out.println("\nEmployee data----");
        parser.getEmployeeRs().forEach(System.out::println);
        System.out.println("Employee error data----");
        parser.getEmployeeErrRs().forEach(System.out::println);
    }

    private Reader getGoodNestedTagFromFile(String fileName) throws FileNotFoundException, IOException {
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
        return new StringReader(getGoodNestedTagFromString(sb.toString()));
    }

    private String getGoodNestedTagFromString(String notFormatedString) throws FileNotFoundException, IOException {
        return getGoodNestedTagFromString(notFormatedString, null);
    }
    private String getGoodNestedTagFromString(String notFormatedString, String rootTag) throws FileNotFoundException, IOException {
        StringBuilder sb = new StringBuilder();
        if (rootTag == null) {
            NestedTagResolver.formatNestedTag(notFormatedString).forEach(sb::append);
        } else {
            NestedTagResolver.formatNestedTag(notFormatedString, rootTag).forEach(sb::append);
        }
        String rs = sb.toString();
        return rs;
    }

    public void parse() {
        try {
            reader = getGoodNestedTagFromFile("src/library.xml");
            streamReader = f.createXMLStreamReader(reader);

            List<String> bookIs = null;
            List<String> employeeIs = null;
            SchemaValidator validator = new SchemaValidator();
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
                            bookIs = parseBooks(streamReader);
                        } else if ("employees".equals(elementName)) {
                            employeeIs = parseEmployees(streamReader);
                        }
                    }
                }
                List<String> notValidateBookIs = validator.validate(bookIs, Book.class);
                bookRs = validator.getResultList();
                List<String> notValidateEmployeeIs = validator.validate(employeeIs, Employee.class);
                employeeRs = validator.getResultList();
                BookErrorDataParser bedp = new BookErrorDataParser();
                bookErrRs = notValidateBookIs.stream().flatMap(s -> {
                    bedp.reset();
                    List<Book> rs = parseBySaxHandler(s, bedp);
                    return rs.stream();
                }).collect(Collectors.toList());
                EmployeeErrorDataParser eedp = new EmployeeErrorDataParser();
                employeeErrRs = notValidateEmployeeIs.stream().flatMap(s -> {
                    eedp.reset();
                    List<Employee> rs = parseBySaxHandler(s, eedp);
                    return rs.stream();
                }).collect(Collectors.toList());

            } finally {
                int c = streamReader.getLocation().getColumnNumber();
                int l = streamReader.getLocation().getLineNumber();
//            System.out.println("End offset: [" + l + "," + c + "]");
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

    public String convertXMLEventToString(List<XMLEvent> list) {
        StringWriter sr = new StringWriter();
        XMLEventWriter xew;
        try {
            xew = outputFactory.createXMLEventWriter(sr);
            list.forEach((event) -> {
                try {
                    xew.add(event);
                } catch (XMLStreamException ex) {
                }
            });
            xew.flush();
            xew.close();
        } catch (XMLStreamException ex) {
            Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        String s = sr.toString();
        try {
            s = getGoodNestedTagFromString(s);
            System.out.println(s);
            return s;
        } catch (IOException ex) {
            Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    public List<String> parseBooks(XMLStreamReader streamReader) {
        List<List<XMLEvent>> writers;
        try {
            writers = getXmlFragments(streamReader, "books", "book");
            return writers.stream().map(this::convertXMLEventToString).collect(Collectors.toList());
        } catch (XMLStreamException ex) {
            Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }

    public List<String> parseEmployees(XMLStreamReader streamReader) {
        List<List<XMLEvent>> writers;
        try {
            writers = getXmlFragments(streamReader, "employees", "employee");
            return writers.stream().map(this::convertXMLEventToString).collect(Collectors.toList());
        } catch (XMLStreamException ex) {
            Logger.getLogger(LibraryStaxParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }

    public List<List<XMLEvent>> getXmlFragments(XMLStreamReader streamReader, String endTag, String fragmentRootTag) throws XMLStreamException {
        List<List<XMLEvent>> rs = new ArrayList<>();
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

    public List<List<XMLEvent>> getXmlFragment(XMLStreamReader streamReader, String parentTag) throws XMLStreamException {
        List<List<XMLEvent>> rs = new ArrayList<>();
        XMLOutputFactory of = XMLOutputFactory.newInstance();
        XMLEventReader xr = f.createXMLEventReader(streamReader);
        boolean found = false;
        List<XMLEvent> tmp = null;
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
                    tmp.add(exceptionHandler.newEndElement(parentTag));
                    rs.add(tmp);
                    break;
                } else {
                    found = true;
                    tmp = new ArrayList<>();
                }
            } else if (e.isEndElement()
                    && ((EndElement) e).getName().getLocalPart().equals(parentTag)) {
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
        return rs;
    }

}
