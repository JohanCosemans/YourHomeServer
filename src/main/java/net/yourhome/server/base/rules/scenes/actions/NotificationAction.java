package net.yourhome.server.base.rules.scenes.actions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.MessagingException;

import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.base.enums.MobileNotificationTypes;
import net.yourhome.common.net.messagestructures.general.ClientNotificationMessage;
import net.yourhome.server.base.rules.scenes.Scene;
import net.yourhome.server.base.rules.scenes.actions.notifications.EmailService;
import net.yourhome.server.base.rules.scenes.actions.notifications.GoogleCloudMessagingService;
import net.yourhome.server.base.rules.scenes.actions.notifications.NexmoSMSService;
import net.yourhome.server.ipcamera.IPCamera;
import net.yourhome.server.ipcamera.IPCameraController;
import net.yourhome.server.net.rest.IPCameras;

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
		type = detailsObject.getString("type");
		subject = detailsObject.getString("subject");
		message = detailsObject.getString("message");
		email = detailsObject.getString("email");
		phoneNumber = detailsObject.getString("phoneNumber");
		try {
			// Can be empty
			includeSnapshotOfCamera = detailsObject.getInt("includeSnapshotOfCamera");
		} catch (JSONException e) {
		}
	}

	public boolean perform() {

		// Email
		if (this.type.equals("email")) {
			emailService = EmailService.getInstance();
			if (emailService != null && !this.email.equals("")) {
				try {
					emailService.sendMessage(subject, message, email);
				} catch (MessagingException e) {
					log.error("[NotificationAction] [Email] Failed to send e-mail to " + this.email, e);
				}
			}
			// SMS
		} else if (this.type.equals("sms")) {
			smsService = NexmoSMSService.getInstance();
			if (smsService != null && !this.phoneNumber.equals("")) {
				smsService.sendMessage(message, phoneNumber);
			}
		} else if (this.type.equals("mobile")) {

			ClientNotificationMessage notificationMessage = new ClientNotificationMessage();

			// Notification on connected clients
			notificationMessage.notificationType = MobileNotificationTypes.TEXT;
			String snapshotUrl = "";
			if (includeSnapshotOfCamera != 0) {
				IPCamera camera = IPCameraController.getInstance().getIPCamera(includeSnapshotOfCamera);
				File snapshotFile = camera.saveAndGetSnapshot(false, "notification_camera-" + includeSnapshotOfCamera + "_" + new SimpleDateFormat("yyyMMdd_HHmmss").format(new Date()));
				snapshotUrl = IPCameras.getSnapshotUrl(camera.getId(), snapshotFile);

				notificationMessage.imagePath = snapshotUrl;
				notificationMessage.videoPath = camera.getVideoUrl();
				notificationMessage.notificationType = MobileNotificationTypes.IMAGE;
			}
			notificationMessage.title = subject;
			notificationMessage.message = message;
			GoogleCloudMessagingService.getInstance().sendMessage(notificationMessage);
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
		return notificationActionId;
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @return the phoneNumber
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the includeSnapshotOfCamera
	 */
	public int getIncludeSnapshotOfCamera() {
		return includeSnapshotOfCamera;
	}

}
