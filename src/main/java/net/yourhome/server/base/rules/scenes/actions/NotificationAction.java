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
 * THIS SOFTWARE IS PROVIDED BY COTEQ AND CONTRIBUTORS
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
package net.yourhome.server.base.rules.scenes.actions;

import net.yourhome.common.base.enums.MobileNotificationTypes;
import net.yourhome.common.net.messagestructures.general.ClientNotificationMessage;
import net.yourhome.server.base.rules.scenes.Scene;
import net.yourhome.server.base.rules.scenes.actions.notifications.EmailService;
import net.yourhome.server.base.rules.scenes.actions.notifications.PushNotificationService;
import net.yourhome.server.base.rules.scenes.actions.notifications.NexmoSMSService;
import net.yourhome.server.ipcamera.IPCamera;
import net.yourhome.server.net.rest.IPCameraController;
import org.json.JSONException;
import org.json.JSONObject;

import javax.mail.MessagingException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

//Corresponds to Scene_Actions_Notification
public class NotificationAction extends Action {

	private NexmoSMSService smsService;
	private EmailService emailService;

	// Saved/loaded fields:
	private int notificationActionId;
	private String subject;
	private String message;
	private String email;
	private String phoneNumber;
	private String type; // sms,mobile,email
	private int includeSnapshotOfCamera;

	public NotificationAction(Scene parentScene, JSONObject actionObject) throws JSONException {
		super(parentScene, actionObject);
		JSONObject detailsObject = actionObject.getJSONObject("details");
		this.type = detailsObject.getString("type");
		this.subject = detailsObject.getString("subject");
		this.message = detailsObject.getString("message");
		this.email = detailsObject.getString("email");
		this.phoneNumber = detailsObject.getString("phoneNumber");
		try {
			// Can be empty
			this.includeSnapshotOfCamera = detailsObject.getInt("includeSnapshotOfCamera");
		} catch (JSONException e) {
		}
	}

	@Override
	public boolean perform() {

		// Email
		if (this.type.equals("email")) {
			this.emailService = EmailService.getInstance();
			if (this.emailService != null && !this.email.equals("")) {
				try {
					this.emailService.sendMessage(this.subject, this.message, this.email);
				} catch (MessagingException e) {
					Action.log.error("[NotificationAction] [Email] Failed to send e-mail to " + this.email, e);
				}
			}
			// SMS
		} else if (this.type.equals("sms")) {
			this.smsService = NexmoSMSService.getInstance();
			if (this.smsService != null && !this.phoneNumber.equals("")) {
				this.smsService.sendMessage(this.message, this.phoneNumber);
			}
		} else if (this.type.equals("mobile")) {

			ClientNotificationMessage notificationMessage = new ClientNotificationMessage();

			// Notification on connected clients
			notificationMessage.notificationType = MobileNotificationTypes.TEXT;
			String snapshotUrl = "";
			if (this.includeSnapshotOfCamera != 0) {
				IPCamera camera = net.yourhome.server.ipcamera.IPCameraController.getInstance().getIPCamera(this.includeSnapshotOfCamera);
				File snapshotFile = camera.saveAndGetSnapshot(false, "notification_camera-" + this.includeSnapshotOfCamera + "_" + new SimpleDateFormat("yyyMMdd_HHmmss").format(new Date()));
				if(snapshotFile != null) {
                    snapshotUrl = IPCameraController.getSnapshotUrl(camera.getId(), snapshotFile);
                    notificationMessage.imagePath = snapshotUrl;
                    notificationMessage.notificationType = MobileNotificationTypes.IMAGE;
                    notificationMessage.videoPath = camera.getVideoUrl();
                }else {
                    notificationMessage.notificationType = MobileNotificationTypes.TEXT;
                }
			}
			notificationMessage.title = this.subject;
			notificationMessage.message = this.message;
			PushNotificationService.getInstance().sendMessage(notificationMessage);
		}
		return true;
	}

	@Override
	public String toString() {
		return "[NotificationAction] NotificationAction triggered.";
	}

	/**
	 * @return the notificationActionId
	 */
	public int getNotificationActionId() {
		return this.notificationActionId;
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return this.subject;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return this.email;
	}

	/**
	 * @return the phoneNumber
	 */
	public String getPhoneNumber() {
		return this.phoneNumber;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * @return the includeSnapshotOfCamera
	 */
	public int getIncludeSnapshotOfCamera() {
		return this.includeSnapshotOfCamera;
	}

}
