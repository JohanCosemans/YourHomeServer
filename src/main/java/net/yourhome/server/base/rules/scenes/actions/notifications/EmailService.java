/*-
 * Copyright (c) 2016 Coteq, Johan Cosemans
 * All rights reserved.
 *
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
		props.put("mail.smtp.host", this.SMTP_ADDRESS);
		props.put("mail.smtp.socketFactory.port", this.SMTP_PORT);
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", this.SMTP_PORT);

		if (this.SMTP_USER != null && this.SMTP_PASSWORD != null) {
			this.session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(EmailService.this.SMTP_USER, EmailService.this.SMTP_PASSWORD);
				}
			});
		}
	}

	private static volatile EmailService instance;
	private static Object lock = new Object();

	public static EmailService getInstance() {
		EmailService r = EmailService.instance;
		if (r == null) {
			synchronized (EmailService.lock) { // while we were waiting for the
												// lock, another
				r = EmailService.instance; // thread may have instantiated the
											// object
				if (r == null) {
					r = new EmailService();
					EmailService.instance = r;
				}
			}
		}
		return EmailService.instance;
	}

	public boolean sendMessage(String subject, String message, String email) throws AddressException, MessagingException {
		Message emailMessage = new MimeMessage(this.session);
		emailMessage.setFrom(InternetAddress.parse(this.SMTP_SENDER)[0]);
		emailMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
		emailMessage.setSubject(subject);
		emailMessage.setText("On " + (new Date().toString()) + ", the following message was sent by your home: \n\n" + message);
		Transport.send(emailMessage);

		return true;
	}
}
