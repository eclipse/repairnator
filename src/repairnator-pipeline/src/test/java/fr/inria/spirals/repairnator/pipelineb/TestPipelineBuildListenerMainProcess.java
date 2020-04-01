package fr.inria.spirals.repairnator.pipeline;

import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.util.ByteSequence;
import org.junit.Test;

import javax.jms.MessageNotWriteableException;

import static org.junit.Assert.assertEquals;

public class TestPipelineBuildListenerMainProcess {
    @Test
    public void testMessageExtractor() {
        PipelineBuildListenerMainProcess buildListener = (PipelineBuildListenerMainProcess) MainProcessFactory.getPipelineListenerMainProcess(new String[]{});
        ActiveMQTextMessage textMessage = new ActiveMQTextMessage();

        try {
            textMessage.setText("1");
        } catch (MessageNotWriteableException e) {
            e.printStackTrace();
        }
        // a plain-text message which only contains a build id
        assertEquals(1, buildListener.extractBuiltId(textMessage));

        try {
            textMessage.setText("{\"buildId\":\"2\",\"CI\":\"travis-ci.org\"}");
        } catch (MessageNotWriteableException e) {
            e.printStackTrace();
        }
        // a plain-text message which is in json
        assertEquals(2, buildListener.extractBuiltId(textMessage));

        ActiveMQBytesMessage bytesMessage = new ActiveMQBytesMessage();
        bytesMessage.setContent(new ByteSequence("3".getBytes()));
        bytesMessage.setReadOnlyBody(true);
        // a bytes message which only contains a build id
        assertEquals(3, buildListener.extractBuiltId(bytesMessage));

        ActiveMQBytesMessage bytesMessageInJson = new ActiveMQBytesMessage();
        bytesMessageInJson.setContent(new ByteSequence("{\"buildId\":\"4\",\"CI\":\"travis-ci.org\"}".getBytes()));
        bytesMessageInJson.setReadOnlyBody(true);
        // a bytes message whose content is formatted in json
        assertEquals(4, buildListener.extractBuiltId(bytesMessageInJson));
    }
}
