/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trinity.sample.library;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import trinity.sample.library.book.Book;
import trinity.sample.library.employee.Employee;

/**
 *
 * @author ADMIN
 */
public class SchemaValidator {

    private SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    private List resultList;

    public String getChemaLocation(Class clazz) {
        String rs = "";
        if (clazz.equals(Book.class)) {
            rs = "src/book.xsd";
        } else if (clazz.equals(Employee.class)) {
            rs = "src/employee.xsd";
        } else {
            throw new RuntimeException("Not found schema");
        }
        return rs;
    }

    public <A> List<String> validate(List<String> fragments, Class<A> clazz) 
            throws JAXBException, SAXException {
        List<String> rs = new ArrayList<>();
        resultList = new ArrayList();
        JAXBContext jc = JAXBContext.newInstance(clazz);
        Unmarshaller u = jc.createUnmarshaller();

        Schema schema;
        schema = sf.newSchema(new File(getChemaLocation(clazz)));

        Validator validator = schema.newValidator();
        for (String s : fragments) {
            try {
                validator.validate(new SAXSource(createInputSource(s)));
                A item = (A) u.unmarshal(new SAXSource(createInputSource(s)));
                resultList.add(item);
            } catch (Exception ex) {
                rs.add(s);
            }
        }
        return rs;
    }

    public static InputSource createInputSource(String s) {
        return new InputSource(new StringReader(s));
    }

    public List getResultList() {
        return resultList;
    }

}
