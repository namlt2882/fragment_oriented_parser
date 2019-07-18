/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trinity.sample.library;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

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
        BufferedReader reader = new BufferedReader(new StringReader(xmlDoc));
        int tmp;
        String s;
        boolean isComment = false;
        boolean isXMLDeclaration = false;
        boolean isCharacterData = false;
        boolean isInTag = false;
        int xmlDeclarationStack = 0;
        int cdataStack = 0;
        while (true) {
            try {
                tmp = reader.read();
                if (tmp == -1) {
                    break;
                }
                // not read comments
                if (isComment) {
                    if (tmp == '-') {
                        reader.mark(2);
                        int c = reader.read();
                        //[case]: end of comment
                        if (c == '-' && (c = reader.read()) == '>') {
                            isComment = false;
                            continue;
                        } else {
                            reader.reset();
                        }
                    }
                    continue;
                }
                if (tmp == '<') {
                    reader.mark(3);
                    int c = reader.read();
                    if (c == '!') {
                        if ((c = reader.read()) == '-' && (c = reader.read()) == '-') {
                            //[case]: meet new comment
                            isComment = true;
                            continue;
                        } else {
                            //[case]: meet new xml declaration or CDATA
                            //check if a CDATA
                            if (!isXMLDeclaration) {
                                reader.reset();
                                reader.mark(7);
                                reader.read();
                                if ((c = reader.read()) == '[' && (c = reader.read()) == 'C'
                                        && (c = reader.read()) == 'D' && (c = reader.read()) == 'A'
                                        && (c = reader.read()) == 'T' && (c = reader.read()) == 'A') {
                                    if (!isInTag) {
                                        //[case]: meet a CDATA
                                        isCharacterData = true;
                                        reader.reset();
                                    } else {
                                        //[case]: skip CDATA in tag
                                        cdataStack = 1;
                                        do {
                                            c = reader.read();
                                            if (c == '>') {
                                                cdataStack--;
                                            } else if (c == '<') {
                                                cdataStack++;
                                            }
                                        } while (c != -1 && cdataStack > 0);
                                        continue;
                                    }
                                }
                            }
                            if (!isCharacterData) {
                                //[case]: meet new xml declaration
                                isXMLDeclaration = true;
                                xmlDeclarationStack++;
                                continue;
                            }
                        }
                    } else {
                        //if not a xml declaration or comment
                        reader.reset();
                        if (isXMLDeclaration) {
                            //[case]: xml declaration tag is not closed
                            xmlDeclarationStack = 0;
                            isXMLDeclaration = false;
                        }
                    }
                    if (localBuilder != null) {
                        if (!isCharacterData) {
                            //[case]: meet open tag
                            s = localBuilder.toString();
                            if (s != null && !s.trim().equals("")) {
                                rs.add(s);
                            }
                            isInTag = true;
                            localBuilder = new StringBuilder().append('<');
                        } else {
                            localBuilder.append('<');
                            cdataStack++;
                        }
                    } else {
                        //[case]: first time meet open tag
                        isInTag = true;
                        localBuilder = new StringBuilder().append('<');
                    }
                } else if (tmp == '>') {
                    if (isXMLDeclaration) {
                        xmlDeclarationStack--;
                        if (xmlDeclarationStack == 0) {
                            //[case]: xml declaration stack is empty
                            isXMLDeclaration = false;
                            continue;
                        }
                        if (xmlDeclarationStack > 0) {
                            //[case]: xml declaration stack is not empty
                            continue;
                        }
                    }
                    localBuilder.append('>');
                    if (isCharacterData) {
                        cdataStack--;
                        if (cdataStack == 0) {
                            isCharacterData = false;
                        } else {
                            continue;
                        }
                    }
                    if (isInTag) {
                        isInTag = false;
                    }
                    rs.add(localBuilder.toString());
                    localBuilder = new StringBuilder();
                } else if (localBuilder != null && !isXMLDeclaration) {
                    localBuilder.append((char) tmp);
                }
            } catch (IOException ex) {
                Logger.getLogger(NestedTagResolver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(NestedTagResolver.class.getName()).log(Level.SEVERE, null, ex);
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
            } else if (s.charAt(1) == '!') {
                return CHARACTERS;
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

    public static List<String> formatNestedTag(String s, String rootTag) {
        return formatNestedTag(splitXmlDocument(s), rootTag);
    }

    private static List<String> formatNestedTag(List<String> s, String rootTag) {
        LinkedList<String> formattedDoc = new LinkedList<>();
        LinkedList<String> queue = new LinkedList<>();
        String tagName;
        String lastTagName;
        for (String string : s) {
            int type = checkTagType(string);
            if (type == XML_DECLARATION) {
                continue;
            }
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
        //add root tag
        boolean addRootTag = true;
        if (rootTag == null) {
            rootTag = "root";
        } else {
            if (rootTag.equals("")) {
                rootTag = "root";
            }
            try {
                String start = formattedDoc.getFirst();
                String end = formattedDoc.getLast();
                if (checkTagType(start) == START_ELEMENT && checkTagType(end) == END_ELEMENT
                        && rootTag.equals(getLocalName(start)) && rootTag.equals(getLocalName(end))) {
                    addRootTag = false;
                }
            } catch (Exception e) {
            }
        }
        if (addRootTag) {
            formattedDoc.addFirst("<" + rootTag + ">");
            formattedDoc.addLast("</" + rootTag + ">");
        }
        return formattedDoc;
    }

    public static void main(String[] args) {
        String line = "<!DOCTYPE library[\n"
                + "<!ELEMENT library (book)*>\n"
                + "<!ELEMENT book (booktitle+, author, price)+>\n"
                + "<!ELEMENT booktitle ( p | PCDATA)*>\n"
                + "<!ELEMENT author (#PCDATA)>\n"
                + "<!ELEMENT price (#PCDATA)>\n"
                + "<!ELEMENT p (#PCDATA)>\n"
                + "<!ATTLIST book id ID #REQUIRED>\n"
                + "]>"
                + "<!--[if IEMobile 7]><html class=\"iem7\" lang=\"vi\" dir=\"ltr\"><![endif]-->"
                + "<xml><xm<![CDATA[<i>Comma</i>]]>l abc='<![CDATA[<i>Comma</i>]]>'>abc<a><![CDATA[<i>Comma</i>]]></a>xyz<![CDATA[<i>Comma</i>]]></xml><input>ksdhs</aaa>akjd<input>hskj<!--jhsadhjgdhgj<a>-->dhsakd<hbc/></xml>";
        List<String> docStr = splitXmlDocument(line);
        docStr.forEach(s -> {
            int tagType = checkTagType(s);
            switch (tagType) {
                case START_ELEMENT:
                    System.out.print("Start(" + getLocalName(s) + "): ");
                    break;
                case END_ELEMENT:
                    System.out.print("End(" + getLocalName(s) + "): ");
                    break;
                case EMPTY_ELEMENT:
                    System.out.print("Empty(" + getLocalName(s) + "): ");
                    break;
                case CHARACTERS:
                    System.out.print("Chars: ");
                    break;
            }
            System.out.println(s);
        });
        System.out.println("\nHandling------\n");
        docStr = formatNestedTag(docStr, "xml");
        docStr.forEach(System.out::println);
    }
}
