package fr.inria.spirals.repairnator.process.maven;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
            
            node.normalize();
            NodeList nodes = node.getChildNodes();
            
//            BufferedReader reader = new BufferedReader(new FileReader(fileName));
//            String line;
//            while((line = reader.readLine()) != null) {
//                System.out.println(line);
//            }
            nodes = nodes.item(1).getChildNodes();
            assertTrue(nodes.item(1).getTextContent().contains("test-plugin-id"));
            assertTrue(nodes.item(3).getTextContent().contains("test-plugin-name"));
            assertTrue(nodes.item(5).getTextContent().contains("test-plugin-url"));
            

            
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
            
            NodeList nodes = node.getChildNodes();

            // There are 3 nodes before this one
            nodes = nodes.item(3).getChildNodes();
            System.out.println(nodes.item(1).getTextContent());
            assertTrue(nodes.item(1).getTextContent().equals("test-plugin-id"));
            assertTrue(nodes.item(3).getTextContent().equals("test-plugin-name"));
            assertTrue(nodes.item(5).getTextContent().equals("test-plugin-url"));
            

            
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Test
    public void testRemovePluginRepoNoPrevious() {
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
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fileName);
            
            NodeList pluginRepos = doc.getElementsByTagName("pluginRepository");
            int nrRepos = pluginRepos.getLength(); //should be zero
            
            POMModifier.addPluginRepo(fileName, "test-plugin-id", "test-plugin-name", "test-plugin-url");
            POMModifier.removePluginRepo(fileName, "test-plugin-id");
            
            doc = dBuilder.parse(fileName);
            pluginRepos = doc.getElementsByTagName("pluginRepository");
            
            assertTrue(nrRepos == pluginRepos.getLength());
            
        }
        catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            e.printStackTrace();
        }
        
        
    }
    
    @Test
    public void testRemovePluginRepoPrevious() {
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
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fileName);
            
            NodeList pluginRepos = doc.getElementsByTagName("pluginRepository");
            int nrRepos = pluginRepos.getLength(); //should be zero
            
            POMModifier.addPluginRepo(fileName, "test-plugin-id", "test-plugin-name", "test-plugin-url");
            POMModifier.removePluginRepo(fileName, "test-plugin-id");
            
            doc = dBuilder.parse(fileName);
            pluginRepos = doc.getElementsByTagName("pluginRepository");
            
            assertTrue(nrRepos == pluginRepos.getLength());
            
        }
        catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            e.printStackTrace();
        }
    }
}
