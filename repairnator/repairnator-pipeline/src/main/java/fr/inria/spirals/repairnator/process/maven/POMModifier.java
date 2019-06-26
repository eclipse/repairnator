package fr.inria.spirals.repairnator.process.maven;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer; 
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


/**
 * A class to modify some POM-file. 
 * @author btellstrom
 *
 */
public class POMModifier {
    
    public static void addPluginRepo(String fileName, String id, String name, String url) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(fileName);
        Element root = doc.getDocumentElement();
        //Node root = doc.getFirstChild();
        
        Node pluginRepos = doc.getElementsByTagName("pluginRepositories").item(0);
        
        // See so that pluginRepos is not null, if so, create the node
        if(pluginRepos == null) {
            pluginRepos = doc.createElement("pluginRepositories");
            root.appendChild(pluginRepos);
        }
        
        // Add the new plugin repo
        Node repairRepo = doc.createElement("pluginRepository");
        pluginRepos.appendChild(repairRepo);
        
        // id, name, url
        Node repoId = doc.createElement("id");
        repoId.setTextContent(id);
        Node repoName = doc.createElement("name");
        repoName.setTextContent(name);
        Node repoUrl = doc.createElement("url");
        repoUrl.setTextContent(url);
        
        repairRepo.appendChild(repoId);
        repairRepo.appendChild(repoName);
        repairRepo.appendChild(repoUrl);
        
        replaceFile(fileName, doc);
    }
    
    public static void removePluginRepo(String fileName, String id) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        Document doc = builder.parse(fileName);
        
        Node repoId = doc.getElementsByTagName(id).item(0);
        Node repairRepo = repoId.getParentNode();
        Node pluginRepos = repairRepo.getParentNode();
        pluginRepos.removeChild(repairRepo);

        replaceFile(fileName, doc);
    }
    
    private static void replaceFile(String fileName, Document doc) throws TransformerException {
        DOMSource source = new DOMSource(doc);
                
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        
        StreamResult result = new StreamResult(fileName);
        transformer.transform(source, result);
    }
}
