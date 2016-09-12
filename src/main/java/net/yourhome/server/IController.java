package net.yourhome.server;

import java.util.Collection;
import java.util.List;

import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.base.Setting;
import net.yourhome.server.base.rules.triggers.Trigger;

public interface IController {

	public String getIdentifier();

	public String getName();

	public String getValueName(ControlIdentifiers valueIdentifiers);

	public String getValue(ControlIdentifiers valueIdentifiers);

	public void init();

	public Collection<JSONMessage> initClient();

	public boolean isEnabled();

	public boolean isInitialized();

	public JSONMessage parseNetMessage(JSONMessage message);

	public Collection<ControllerNode> getNodes();

	public Collection<ControllerNode> getTriggers();

	public void addTriggerListener(Trigger trigger);

	public void removeTriggerListener(Trigger trigger);

	public void triggerValueChanged(ControlIdentifiers valueIdentifiers);

	public void triggerEvent(String nodeIdentifier, String valueIdentifier);

	public Collection<Setting> getSettings();

	public void destroy();

}
