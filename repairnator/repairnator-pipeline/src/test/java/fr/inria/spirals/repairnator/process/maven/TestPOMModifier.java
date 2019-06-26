package fr.inria.spirals.repairnator.process.maven;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;



public class TestPOMModifier {
    
    @Test
    public void testAddPluginRepoNoPrevious() {
        Path filePath = Paths.get("./src/test/resources/pom-examples", "javaparser-pom.xml");
        File tempDir = com.google.common.io.Files.createTempDir();
        
        Path tempFile = Paths.get(tempDir.toPath().toString(), filePath.getFileName().toString());
        
        try {
            Files.copy(filePath, tempFile);
        } catch (IOException e) {
            System.err.println("Something went wrong when copying the pom-file.");
        }
        
        String fileName = tempFile.toString();
        
        try {
            POMModifier.addPluginRepo(fileName, "test-plugin-id", "test-plugin-name", "test-plugin-url");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fileName);
                
            Node node = doc.getElementsByTagName("pluginRepositories").item(0);
            assertTrue(node != null);
            
            NodeList nodes = node.getChildNodes();
            nodes = nodes.item(0).getChildNodes();
            assertTrue(nodes.item(0).getTextContent().equals("test-plugin-id"));
            assertTrue(nodes.item(1).getTextContent().equals("test-plugin-name"));
            assertTrue(nodes.item(2).getTextContent().equals("test-plugin-url"));
//            
//            BufferedReader reader = new BufferedReader(new FileReader(fileName));
//            String line;
//            while((line = reader.readLine()) != null) {
//                System.out.println(line);
//            }
            
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Test
    public void testAddPluginRepoPrevious() {
        Path filePath = Paths.get("./src/test/resources/pom-examples", "test-plugin-repository.xml");
        File tempDir = com.google.common.io.Files.createTempDir();
        
        Path tempFile = Paths.get(tempDir.toPath().toString(), filePath.getFileName().toString());
        
        try {
            Files.copy(filePath, tempFile);
        } catch (IOException e) {
            System.err.println("Something went wrong when copying the pom-file.");
        }
        
        String fileName = tempFile.toString();
        
        try {
            POMModifier.addPluginRepo(fileName, "test-plugin-id", "test-plugin-name", "test-plugin-url");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fileName);
                
            Node node = doc.getElementsByTagName("pluginRepositories").item(0);
            assertTrue(node != null);
            
//            BufferedReader reader = new BufferedReader(new FileReader(fileName));
//            String line;
//            while((line = reader.readLine()) != null) {
//                System.out.println(line);
//            }
            
            NodeList nodes = node.getChildNodes();
            nodes = nodes.item(0).getChildNodes();
            System.out.println(nodes.item(0).getTextContent());
            assertTrue(nodes.item(0).getTextContent().equals("test-plugin-id"));
            assertTrue(nodes.item(1).getTextContent().equals("test-plugin-name"));
            assertTrue(nodes.item(2).getTextContent().equals("test-plugin-url"));
            

            
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
