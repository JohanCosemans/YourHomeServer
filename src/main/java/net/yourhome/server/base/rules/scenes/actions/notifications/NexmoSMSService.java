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
			this.smsClient = new NexmoSmsClient(NexmoSMSService.API_KEY, NexmoSMSService.API_SECRET);
		} catch (Exception e) {
			NexmoSMSService.log.error("[Scheduler] [SMS] Failed to instanciate sms client");
		}
	}

	private static volatile NexmoSMSService instance;
	private static Object lock = new Object();

	public static NexmoSMSService getInstance() {
		NexmoSMSService r = NexmoSMSService.instance;
		if (r == null) {
			synchronized (NexmoSMSService.lock) { // while we were waiting for
													// the lock, another
				r = NexmoSMSService.instance; // thread may have instantiated
												// the object
				if (r == null) {
					r = new NexmoSMSService();
					NexmoSMSService.instance = r;
				}
			}
		}
		return NexmoSMSService.instance;
	}

	public boolean sendMessage(String message, String phoneNumber) {
		SmsSubmissionResult[] results = null;
		TextMessage smsMessage = new TextMessage("HOME", phoneNumber, message);
		try {
			results = this.smsClient.submitMessage(smsMessage);
			for (SmsSubmissionResult r : results) {
				if (r.getStatus() != SmsSubmissionResult.STATUS_OK) {
					throw new Exception("Return status " + r.getStatus() + ", " + r.getErrorText());
				}
			}
		} catch (Exception e) {
			NexmoSMSService.log.error("[SMS] Could not send SMS " + e.getMessage(), e);
		}
		return false;
	}

}
