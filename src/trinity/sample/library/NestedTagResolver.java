/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trinity.sample.library;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;

/**
 *
 * @author ADMIN
 */
public class NestedTagResolver {

    public static final int EMPTY_ELEMENT = -1;
    public static final int XML_DECLARATION = -2;

    private static List<String> splitXmlDocument(String xmlDoc) {
        List<String> rs = new ArrayList<>();
        StringBuilder localBuilder = null;
        StringReader reader = new StringReader(xmlDoc);
        int tmp;
        String s;
        while (true) {
            try {
                tmp = reader.read();
                if (tmp == -1) {
                    break;
                } else if (tmp == '<') {
                    if (localBuilder != null) {
                        s = localBuilder.toString();
                        if (s != null && !s.trim().equals("")) {
                            rs.add(s);
                        }
                    }
                    localBuilder = new StringBuilder().append('<');
                } else if (tmp == '>') {
                    localBuilder.append('>');
                    rs.add(localBuilder.toString());
                    localBuilder = new StringBuilder();
                } else if (localBuilder != null) {
                    localBuilder.append((char) tmp);
                }
            } catch (IOException ex) {
                Logger.getLogger(NestedTagResolver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return rs;
    }

    private static int checkTagType(String s) {
        int length = s.length();
        if (s.charAt(0) == '<') {
            if (s.charAt(length - 2) == '/') {
                return EMPTY_ELEMENT;
            } else if (s.charAt(1) == '/') {
                return END_ELEMENT;
            } else if (s.charAt(1) == '?') {
                return XML_DECLARATION;
            }
            return START_ELEMENT;
        } else {
            return CHARACTERS;
        }
    }

    private static String getLocalName(String s) {
        if (s.charAt(0) != '<') {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        StringReader sr = new StringReader(s);
        int c;
        int pos = 0;
        while (true) {
            try {
                pos++;
                c = sr.read();
                if (c == -1 || c == '>' || c == ' ') {
                    break;
                }
                if (c == '/' && pos != 2) {
                    break;
                }
                if (c != '<' && c != '/') {
                    sb.append((char) c);
                }
            } catch (IOException ex) {
                Logger.getLogger(NestedTagResolver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return sb.toString();
    }

    public static List<String> formatNestedTag(List<String> s) {
        return formatNestedTag(s, null);
    }

    public static List<String> formatNestedTag(String s) {
        return formatNestedTag(splitXmlDocument(s), null);
    }

    public static List<String> formatNestedTag(String s, String rootTag) {
        return formatNestedTag(splitXmlDocument(s), rootTag);
    }

    public static List<String> formatNestedTag(List<String> s, String rootTag) {
        LinkedList<String> formattedDoc = new LinkedList<>();
        LinkedList<String> queue = new LinkedList<>();
        String tagName;
        String lastTagName;
        for (String string : s) {
            int type = checkTagType(string);
            if (type == START_ELEMENT) {
                tagName = getLocalName(string);
                queue.add(tagName);//add to queue
            } else if (type == END_ELEMENT) {
                tagName = getLocalName(string);
                lastTagName = queue.getLast();
                //thừa thẻ đóng
                if (!lastTagName.equals(tagName)) {
                    formattedDoc.add("<" + tagName + ">");
                } else {
                    queue.removeLast();
                }
            }
            formattedDoc.add(string);
        }
        while (!queue.isEmpty()) {
            formattedDoc.add("</" + queue.getLast() + ">");
            queue.removeLast();
        }
        if (rootTag != null) {
            if (formattedDoc.getFirst().contains("<?xml")) {
                String tmpStr = formattedDoc.removeFirst();
                formattedDoc.addFirst("<" + rootTag + ">");
                formattedDoc.addFirst(tmpStr);
            } else {
                formattedDoc.addFirst("<" + rootTag + ">");
            }
            formattedDoc.addLast("</" + rootTag + ">");
        }
        return formattedDoc;
    }

}
