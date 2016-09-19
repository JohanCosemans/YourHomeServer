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
package net.yourhome.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.base.rules.triggers.ITrigger;
import net.yourhome.server.base.rules.triggers.Trigger;
import net.yourhome.server.base.rules.triggers.TriggerTypes;

public abstract class AbstractController implements IController {

	protected Map<TriggerTypes, Map<String, List<ITrigger>>> triggerListeners = new ConcurrentHashMap<>();

	protected Logger log = Logger.getLogger("net.yourhome.server.Controller");

	@Override
	public void addTriggerListener(Trigger trigger) {
		Map<String, List<ITrigger>> triggersOfTheSameType = this.triggerListeners.get(trigger.getType());
		if (triggersOfTheSameType == null) {
			triggersOfTheSameType = new ConcurrentHashMap<>();
			this.triggerListeners.put(trigger.getType(), triggersOfTheSameType);
		}
		List<ITrigger> triggersOfSameTypeAndControlIds = triggersOfTheSameType.get(trigger.getIdentifiers().getKey());
		if (triggersOfSameTypeAndControlIds == null) {
			triggersOfSameTypeAndControlIds = new ArrayList<>();
		}
		triggersOfSameTypeAndControlIds.add(trigger);
		triggersOfTheSameType.put(trigger.getIdentifiers().getKey(), triggersOfSameTypeAndControlIds);
	}

	@Override
	public void removeTriggerListener(Trigger trigger) {
		Map<String, List<ITrigger>> triggersOfTheSameType = this.triggerListeners.get(trigger.getType());
		if (triggersOfTheSameType != null) {
			List<ITrigger> triggersOfSameTypeAndControlIds = triggersOfTheSameType.get(trigger.getIdentifiers().getKey());
			if (triggersOfSameTypeAndControlIds != null) {
				triggersOfSameTypeAndControlIds.remove(trigger);
			}
		}
	}

	@Override
	public void triggerValueChanged(ControlIdentifiers valueIdentifiers) {
		Map<String, List<ITrigger>> valueListeners = this.triggerListeners.get(TriggerTypes.VALUE);
		if (valueListeners != null) {
			List<ITrigger> valueListeningTriggers = valueListeners.get(valueIdentifiers.getKey());
			if (valueListeningTriggers != null) {
				for (ITrigger trigger : valueListeningTriggers) {
					((Trigger) trigger).trigger();
				}
			}
		}
	}

	@Override
	public void triggerEvent(String nodeIdentifier, String valueIdentifier) {
		Map<String, List<ITrigger>> activationListeners = this.triggerListeners.get(TriggerTypes.ACTIVATION);
		if (activationListeners != null) {
			List<ITrigger> activationListeningTriggers = activationListeners.get(new ControlIdentifiers(this.getIdentifier(), nodeIdentifier, valueIdentifier).getKey());
			if (activationListeningTriggers != null) {
				for (ITrigger trigger : activationListeningTriggers) {
					((Trigger) trigger).trigger();
				}
			}
		}
	}

	@Override
	public void init() {
		// log.info("Initializing controller");

	}

	@Override
	public void destroy() {
		this.triggerListeners.clear();
	}
}
