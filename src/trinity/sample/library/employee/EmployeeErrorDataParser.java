package trinity.sample.library.employee;

import trinity.sample.library.book.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import trinity.sample.library.SAXErrorDataParser;

public class EmployeeErrorDataParser extends DefaultHandler implements SAXErrorDataParser {

    private String id, name;
    private String currentTagName;
    boolean found = false;
    private List<Employee> result = null;

    public EmployeeErrorDataParser() {
        reset();
    }

    public void reset() {
        result = new ArrayList<>();
        id = null;
        name = null;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("employee")) {
            found = true;
        }
        currentTagName = qName;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        currentTagName = "";
        if (found && qName.equals("employee")) {
            Employee employee = new Employee(id, name);
            result.add(employee);
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
        }
    }

    @Override
    public List getResult() {
        return result;
    }

}
