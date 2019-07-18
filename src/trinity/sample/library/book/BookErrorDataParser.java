package trinity.sample.library.book;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import trinity.sample.library.SAXErrorDataParser;

public class BookErrorDataParser extends DefaultHandler implements SAXErrorDataParser {

    private String id, name, author;
    private String price;
    private String currentTagName;
    boolean found = false;
    private List<Book> result = null;

    public BookErrorDataParser() {
        reset();
    }

    public void reset() {
        result = new ArrayList<>();
        id = null;
        name = null;
        author = null;
        price = null;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) 
            throws SAXException {
        if (qName.equals("book")) {
            found = true;
        }
        currentTagName = qName;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        currentTagName = "";
        if (found && qName.equals("book")) {
            Book book = null;
            if (price != null) {
                book = new Book(id, name, author, new BigInteger(price));
            } else {
                book = new Book(id, name, author);
            }
            result.add(book);
            found = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (!found) {
            return;
        }
        String s = new String(ch, start, length);
        switch (currentTagName) {
            case "id":
                id = s;
                break;
            case "name":
                name = s;
                break;
            case "author":
                author = s;
                break;
            case "price":
                price = s;
                break;
        }
    }

    @Override
    public List getResult() {
        return result;
    }

}
