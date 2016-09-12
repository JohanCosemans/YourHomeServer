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
		Map<String, List<ITrigger>> triggersOfTheSameType = triggerListeners.get(trigger.getType());
		if (triggersOfTheSameType == null) {
			triggersOfTheSameType = new ConcurrentHashMap<>();
			triggerListeners.put(trigger.getType(), triggersOfTheSameType);
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
		Map<String, List<ITrigger>> triggersOfTheSameType = triggerListeners.get(trigger.getType());
		if (triggersOfTheSameType != null) {
			List<ITrigger> triggersOfSameTypeAndControlIds = triggersOfTheSameType.get(trigger.getIdentifiers().getKey());
			if (triggersOfSameTypeAndControlIds != null) {
				triggersOfSameTypeAndControlIds.remove(trigger);
			}
		}
	}

	@Override
	public void triggerValueChanged(ControlIdentifiers valueIdentifiers) {
		Map<String, List<ITrigger>> valueListeners = triggerListeners.get(TriggerTypes.VALUE);
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
		Map<String, List<ITrigger>> activationListeners = triggerListeners.get(TriggerTypes.ACTIVATION);
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
		//log.info("Initializing controller");
		
	}

	@Override
	public void destroy() {
		this.triggerListeners.clear();
	}
}
