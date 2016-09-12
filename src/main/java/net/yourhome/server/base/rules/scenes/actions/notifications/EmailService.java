package net.yourhome.server.base.rules.scenes.actions.notifications;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.yourhome.server.base.GeneralController;
import net.yourhome.server.base.SettingsManager;

public class EmailService {

	// Email
	private Session session;
	private String SMTP_ADDRESS = SettingsManager.getStringValue(GeneralController.getInstance().getIdentifier(), GeneralController.Settings.SMTP_ADDRESS.get());
	private String SMTP_PORT = SettingsManager.getStringValue(GeneralController.getInstance().getIdentifier(), GeneralController.Settings.SMTP_PORT.get());
	private String SMTP_USER = SettingsManager.getStringValue(GeneralController.getInstance().getIdentifier(), GeneralController.Settings.SMTP_USER.get());
	private String SMTP_PASSWORD = SettingsManager.getStringValue(GeneralController.getInstance().getIdentifier(), GeneralController.Settings.SMTP_PASSWORD.get());
	private String SMTP_SENDER = SettingsManager.getStringValue(GeneralController.getInstance().getIdentifier(), GeneralController.Settings.SMTP_SENDER.get());

	private EmailService() {
		// Initialize the mail manager
		Properties props = new Properties();
		props.put("mail.smtp.host", SMTP_ADDRESS);
		props.put("mail.smtp.socketFactory.port", SMTP_PORT);
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", SMTP_PORT);

		if (SMTP_USER != null && SMTP_PASSWORD != null) {
			session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
				}
			});
		}
	}

	private static volatile EmailService instance;
	private static Object lock = new Object();

	public static EmailService getInstance() {
		EmailService r = instance;
		if (r == null) {
			synchronized (lock) { // while we were waiting for the lock, another
				r = instance; // thread may have instantiated the object
				if (r == null) {
					r = new EmailService();
					instance = r;
				}
			}
		}
		return instance;
	}

	public boolean sendMessage(String subject, String message, String email) throws AddressException, MessagingException {
		Message emailMessage = new MimeMessage(session);
		emailMessage.setFrom(InternetAddress.parse(SMTP_SENDER)[0]);
		emailMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
		emailMessage.setSubject(subject);
		emailMessage.setText("On " + (new Date().toString()) + ", the following message was sent by your home: \n\n" + message);
		Transport.send(emailMessage);

		return true;
	}
}
