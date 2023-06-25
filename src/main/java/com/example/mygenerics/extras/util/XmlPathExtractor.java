package io.credable.reconapi.util.pathextractor;

import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

public class XmlPathExtractor {

    //    public static void main(String[] args) throws Exception {
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder builder = factory.newDocumentBuilder();
//        InputStream is = new FileInputStream(new File("example.xml"));
//        Document document = builder.parse(is);
//        Set<String> paths = extractXmlPaths(document.getDocumentElement());
//        log.info(paths);
//    }
    public static Set<String> extract(MultipartFile file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream is = new FileInputStream(FileConverter.convertMultiPartToFile(file));
        Document document = builder.parse(is);
        return extractXmlPaths(document.getDocumentElement());
    }

    public static Set<String> extractXmlPaths(Element element) {
        Set<String> paths = new LinkedHashSet<>();
        String tagName = element.getTagName();
        tagName = "/" + tagName;
        paths.add(tagName);
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Set<String> childPaths = extractXmlPaths((Element) child);
                for (String childPath : childPaths) {
                    paths.add(tagName + childPath);
                }
            }
        }
        return paths;
    }
}

