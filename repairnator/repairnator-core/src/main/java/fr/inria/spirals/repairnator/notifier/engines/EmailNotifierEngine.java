package fr.inria.spirals.repairnator.notifier.engines;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
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

    public EmailNotifierEngine(String[] receivers, String smtpServer) {
        this.properties = new Properties();
        this.properties.put("mail.smtp.host", smtpServer);
        this.session = Session.getDefaultInstance(this.properties, null);

        try {
            this.from = new InternetAddress("librepair@inria.fr");
        } catch (AddressException e) {
            logger.error("Error while creating from adresses, the notifier won't be usable.", e);
        }
        this.to = new ArrayList<>();

        for (String receiver : receivers) {
            try {
                this.to.add(new InternetAddress(receiver));
            } catch (AddressException e) {
                logger.error("Error while creating 'to' adress for the following email: "+receiver+". This user won't receive notifications.", e);
            }
        }
    }

    public void notify(String subject, String message) {
        if (this.from != null && !this.to.isEmpty()) {
            Message msg = new MimeMessage(this.session);

            try {
                msg.setFrom(this.from);
                msg.addRecipients(Message.RecipientType.TO, this.to.toArray(new Address[this.to.size()]));
                msg.setSubject(subject);
                msg.setText(message);

                Transport.send(msg);
            } catch (MessagingException e) {
                logger.error("Error while sending notification message '"+subject+"'", e);
            }
        } else {
            logger.warn("From is null or to is empty. Notification won't be send.");
        }

    }
}
