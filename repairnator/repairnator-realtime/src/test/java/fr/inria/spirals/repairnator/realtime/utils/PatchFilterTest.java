package fr.inria.spirals.repairnator.realtime.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PatchFilterTest {

    
    @Test
    public void correctSingleLinePatch() {
        PatchFilter filter = new PatchFilter();
        
        String patch = "@@ -39,8 +42,9 @@ public void testActiveMQSubmitter()\n"
                     + " \n"
                     + " ActiveMQBuildSubmitter submitter = new ActiveMQBuildSubmitter();\n"
                     + " submitter.initBroker();\n"
                     + "-submitter.submit(\"Test\");\n"
                     + "+submitter.submit(\"WrongTest\");\n"
                     + " assertEquals(submitter.receiveFromQueue(),\"Test\");\n"
                     + " submitter.submitBuild(config.getInstance().getJTravis().build().fromId(589911671).get());\n";
      
        assertTrue(filter.test(patch));
    }
    
    @Test
    public void correctSingleLinePatchNoContext() {
        PatchFilter filter = new PatchFilter();
        
        String patch = "@@ -39,8 +42,9 @@ public void testActiveMQSubmitter()\n"
                     + "-submitter.submit(\"Test\");\n"
                     + "+submitter.submit(\"WrongTest\");\n";
        assertTrue(filter.test(patch));
    }
    
    @Test
    public void incorrectMultiLinePatch1() {
        PatchFilter filter = new PatchFilter();
        
        String patch = "@@ -39,8 +42,9 @@ public void testActiveMQSubmitter()\n"
                     + " \n"
                     + " ActiveMQBuildSubmitter submitter = new ActiveMQBuildSubmitter();\n"
                     + "-submitter.initBroker();\n"
                     + "-submitter.submit(\"Test\");\n"
                     + "+submitter.submit(\"WrongTest\");\n"
                     + "+assertEquals(submitter.receiveFromQueue(),\"Test\");\n"
                     + " submitter.submitBuild(config.getInstance().getJTravis().build().fromId(589911671).get());\n";
      
        assertFalse(filter.test(patch));
    }
    
    @Test
    public void incorrectMultiLinePatch2() {
        PatchFilter filter = new PatchFilter();
        
        String patch = "@@ -39,8 +42,9 @@ public void testActiveMQSubmitter()\n"
                     + " \n"
                     + " ActiveMQBuildSubmitter submitter = new ActiveMQBuildSubmitter();\n"
                     + " submitter.initBroker();\n"
                     + "-submitter.submit(\"Test\");\n"
                     + "+submitter.submit(\"WrongTest\");\n"
                     + "+assertEquals(submitter.receiveFromQueue(),\"Test\");\n"
                     + " submitter.submitBuild(config.getInstance().getJTravis().build().fromId(589911671).get());\n";
      
        assertFalse(filter.test(patch));
    }
    
    @Test
    public void incorrectMultiLinePatch3() {
        PatchFilter filter = new PatchFilter();
        
        String patch = "@@ -39,8 +42,9 @@ public void testActiveMQSubmitter()\n"
                     + " \n"
                     + "-ActiveMQBuildSubmitter submitter = new ActiveMQBuildSubmitter();\n"
                     + "+submitter.initBroker();\n"
                     + " submitter.submit(\"Test\");\n"
                     + "-submitter.submit(\"WrongTest\");\n"
                     + "+assertEquals(submitter.receiveFromQueue(),\"Test\");\n"
                     + " submitter.submitBuild(config.getInstance().getJTravis().build().fromId(589911671).get());\n";
      
        assertFalse(filter.test(patch));
    }
    
    @Test
    public void incorrectMultiHunkPatch() {
        PatchFilter filter = new PatchFilter();
        
        String patch = "@@ -39,8 +42,9 @@ public void testActiveMQSubmitter()\n"
                     + " \n"
                     + "-ActiveMQBuildSubmitter submitter = new ActiveMQBuildSubmitter();\n"
                     + "+submitter.initBroker();\n"
                     + " submitter.submit(\"Test\");\n"
                     + " submitter.submit(\"WrongTest\");\n"
                     + " assertEquals(submitter.receiveFromQueue(),\"Test\");\n"
                     + "@@ -139,8 +142,9 @@ public void testActiveMQSubmitter()\n"
                     + " \n"
                     + "-ActiveMQBuildSubmitter submitter = new ActiveMQBuildSubmitter();\n"
                     + "+submitter.initBroker();\n"
                     + " submitter.submit(\"Test\");\n"
                     + " submitter.submitBuild(config.getInstance().getJTravis().build().fromId(589911671).get());\n";
      
        assertFalse(filter.test(patch));
    }
    
    @Test
    public void correctParse() {
        PatchFilter filter = new PatchFilter();
        
        String patch = "@@ -39,8 +42,9 @@ public void testActiveMQSubmitter()\n"
                     + " \n"
                     + " ActiveMQBuildSubmitter submitter = new ActiveMQBuildSubmitter();\n"
                     + " submitter.initBroker();\n"
                     + "-submitter.submit(\"Test\");\n"
                     + "+submitter.submit(\"WrongTest\");\n"
                     + " assertEquals(submitter.receiveFromQueue(),\"Test\");\n"
                     + " submitter.submitBuild(config.getInstance().getJTravis().build().fromId(589911671).get());\n";
      
        PatchFilter.PatchLines lines = filter.parse(patch);
        
        assertEquals("submitter.submit(\"Test\");", lines.removed);
        assertEquals("submitter.submit(\"WrongTest\");", lines.added);
    }
}
