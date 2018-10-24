package fr.inria.spirals.repairnator.notifier.engines;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by urli on 30/03/2017.
 */
public class EmailNotifierEngine implements NotifierEngine {
    private Logger logger = LoggerFactory.getLogger(EmailNotifierEngine.class);
    private Properties properties;
    private Session session;
    private Address from;
    private List<Address> to;

    public EmailNotifierEngine(String[] receivers, String smtpServer, int smtpPort, boolean smtpTLS,
            String smtpUsername, String smtpPassword) {
        this.properties = new Properties();
        this.properties.put("mail.smtp.host", smtpServer);
        this.properties.put("mail.transport.protocol", "smtp");
        this.properties.put("mail.smtp.port", smtpPort);
        // In the case where a secure connection is wished for
        if (smtpTLS) {
            this.properties.put("mail.smtp.starttls.enable", smtpTLS);
        }
        if (smtpPassword.length() > 1) {
            this.properties.setProperty("mail.smtp.auth", "true");
            this.session = Session.getInstance(this.properties, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });
        } else {
            this.session = Session.getDefaultInstance(this.properties, null);
        }

        try {
            if(!smtpUsername.contains("@")) {
                /* apparently kths mailserver does not accept complete mailaddresses, but sender needs one. This assumes that 
                 * the host name after "smtp." is the actual internet address. Might need modifications.
                 */
                this.from = new InternetAddress(smtpUsername + "@" + this.properties.getProperty("mail.smtp.host").substring(5));
            } else {
                this.from = new InternetAddress(smtpUsername);
            }
        } catch (AddressException e) {
            logger.error("Error while creating from adresses, the notifier won't be usable.", e);
        }
        this.to = new ArrayList<>();

        for (String receiver : receivers) {
            try {
                this.to.add(new InternetAddress(receiver));
            } catch (AddressException e) {
                logger.error("Error while creating 'to' adress for the following email: " + receiver
                        + ". This user won't receive notifications.", e);
            }
        }
    }

    public void notify(String subject, String message) {
        if (this.from != null && !this.to.isEmpty()) {
            Message msg = new MimeMessage(this.session);

            try {
                Address[] recipients = this.to.toArray(new Address[this.to.size()]);
                Transport transport = this.session.getTransport();
                
                msg.setFrom(this.from);
                msg.addRecipients(Message.RecipientType.TO, recipients);
                msg.setSubject(subject);
                msg.setText(message);
                
                transport.connect();
                transport.sendMessage(msg, recipients);
                transport.close();
            } catch (MessagingException e) {
                logger.error("Error while sending notification message '" + subject + "'", e);
            }
        } else {
            logger.warn("From is null or to is empty. Notification won't be send.");
        }

    }
}
