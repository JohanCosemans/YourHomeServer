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

import java.util.Collection;

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
