package net.yourhome.server.base.rules.scenes.actions.notifications;

import org.apache.log4j.Logger;

import com.nexmo.messaging.sdk.NexmoSmsClient;
import com.nexmo.messaging.sdk.SmsSubmissionResult;
import com.nexmo.messaging.sdk.messages.TextMessage;

import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.server.base.GeneralController;
import net.yourhome.server.base.SettingsManager;

public class NexmoSMSService {
	// SMS
	public static final String API_KEY = SettingsManager.getStringValue(ControllerTypes.GENERAL.convert(), GeneralController.Settings.SMS_KEY.get());// "c267e71c";
	public static final String API_SECRET = SettingsManager.getStringValue(ControllerTypes.GENERAL.convert(), GeneralController.Settings.SMS_PASSWORD.get());// "c267e71c";
	NexmoSmsClient smsClient = null;

	private static Logger log = Logger.getLogger(NexmoSMSService.class);

	private NexmoSMSService() {
		// Initialize the SMS manager (Nexmo)
		try {
			smsClient = new NexmoSmsClient(API_KEY, API_SECRET);
		} catch (Exception e) {
			log.error("[Scheduler] [SMS] Failed to instanciate sms client");
		}
	}

	private static volatile NexmoSMSService instance;
	private static Object lock = new Object();

	public static NexmoSMSService getInstance() {
		NexmoSMSService r = instance;
		if (r == null) {
			synchronized (lock) { // while we were waiting for the lock, another
				r = instance; // thread may have instantiated the object
				if (r == null) {
					r = new NexmoSMSService();
					instance = r;
				}
			}
		}
		return instance;
	}

	public boolean sendMessage(String message, String phoneNumber) {
		SmsSubmissionResult[] results = null;
		TextMessage smsMessage = new TextMessage("HOME", phoneNumber, message);
		try {
			results = smsClient.submitMessage(smsMessage);
			for (SmsSubmissionResult r : results) {
				if (r.getStatus() != SmsSubmissionResult.STATUS_OK) {
					throw new Exception("Return status " + r.getStatus() + ", " + r.getErrorText());
				}
			}
		} catch (Exception e) {
			log.error("[SMS] Could not send SMS " + e.getMessage(), e);
		}
		return false;
	}

}
