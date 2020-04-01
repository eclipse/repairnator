package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.File;
import java.io.IOException;

/**
 * This class fetch build ids from ActiveMQ queue and run the pipeline with it.
 */
public class PipelineBuildListenerMainProcess implements MainProcess,MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineBuildListenerMainProcess.class);
    private static final RepairnatorConfig config = RepairnatorConfig.getInstance();
    private MainProcess onMessageMainProcess;

    public PipelineBuildListenerMainProcess(MainProcess onMessageMainProcess){
        this.onMessageMainProcess = onMessageMainProcess;
        LOGGER.warn("KUBERNETES MODE");
    }



    /**
     * Run this as a listener server and fetch one message as a time, 
     * Init default launcher mainprocess to run build
     */
    @Override
    public boolean run() {
        ActiveMQConnectionFactory connectionFactory = null;
        if (config.getActiveMQUsername() == null || config.getActiveMQUsername().isEmpty()) {
            connectionFactory = new ActiveMQConnectionFactory(config.getActiveMQUrl() + "?jms.prefetchPolicy.all=1");
        } else {
            connectionFactory = new ActiveMQConnectionFactory(config.getActiveMQUsername(), config.getActiveMQPassword(), config.getActiveMQUrl() + "?jms.prefetchPolicy.all=1");
        }

        Connection connection = null;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false,Session.CLIENT_ACKNOWLEDGE);
            Destination queue = session.createQueue(config.getActiveMQListenQueueName());

            MessageConsumer consumer = session.createConsumer(queue);
            consumer.setMessageListener(this);
            LOGGER.warn("Server is now listening for build ids");
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    /**
     * Method implemented from MessageListener and is called 
     * each time this is done with the previous message
     *
     * @param message ActiveMQ message object containing a string buildId.
     */
    @Override
    public void onMessage(Message message) {
        try {
            message.acknowledge();
            int buildId = this.extractBuiltId(message);
            LOGGER.info("A new buildId has arrived: " + buildId);
            config.setBuildId(buildId);
            this.onMessageMainProcess.run();

            /* Delete the folder when done*/
            this.deleteDir(String.valueOf(buildId));
            this.deleteDir(config.getWorkspacePath());

            LOGGER.warn("Done repairning. Awaiting for new build ... ");
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }


    /* Helpers */

    /**
     * extract the build id from a message
     * check the message in plain text or binary
     * check the message body is json string or just build id
     * @param message
     * @return build id
     */
    public int extractBuiltId(Message message) {
        String messageText = null;
        try {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                    messageText = textMessage.getText();
            } else if (message instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage) message;
                byte[] data = new byte[(int) bytesMessage.getBodyLength()];
                bytesMessage.readBytes(data);
                messageText = new String(data);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }

        try {
            JSONObject jsonObj = new JSONObject(messageText);
            messageText = jsonObj.getString("buildId");
        } catch (JSONException e) {
            // not json formated, which indicates that the message was pushed by BuildRainer / RepairnatorScanner
        }

        return Integer.parseInt(messageText);
    }

    public void deleteDir(String dirPath) {
        File directory = new File(dirPath);
        //make sure directory exists
        if(!directory.exists()){
 
           LOGGER.warn("Directory " + dirPath + " does not exist.");
 
        }else{
 
           try{
               
               this.delete(directory);
            
           }catch(IOException e){
               e.printStackTrace();
           }
        }
    }

    public void delete(File file) throws IOException{
        if(file.isDirectory()){
            //directory is empty, then delete it
            if(file.list().length==0){
               file.delete();
            }else{
                
               //list all the directory contents
               String files[] = file.list();
     
               for (String temp : files) {
                  //construct the file structure
                  File fileDelete = new File(file, temp);
                 
                  //recursive delete
                 delete(fileDelete);
               }
                
               //check the directory again, if empty then delete it
               if(file.list().length==0){
                 file.delete();
               }
            }
            
        }else{
            //if file, then delete it
            file.delete();
        }
    }

    public void submitBuild(String buildStr){}

    @Override
    public IDefineJSAPArgs getIDefineJSAPArgs() {
        return null;
    }

    @Override
    public IInitConfig getIInitConfig() {
        return null;
    }

    @Override
    public IInitNotifiers getIInitNotifiers() {
        return null;
    }

    @Override
    public IInitSerializerEngines getIInitSerializerEngines() {
        return null;
    }

    @Override
    public void setPatchNotifier(PatchNotifier patchNotifier) {}

    @Override
    public PatchNotifier getPatchNotifier() {
        return null;
    }
}
