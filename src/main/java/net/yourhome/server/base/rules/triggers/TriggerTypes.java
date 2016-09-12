package net.yourhome.server.base.rules.triggers;

import net.yourhome.common.base.enums.EnumConverter;
import net.yourhome.common.base.enums.ReverseEnumMap;

public enum TriggerTypes implements EnumConverter<String, TriggerTypes> {
	GROUP("group", TriggerGroup.class), ACTIVATION("activation", Trigger.class), VALUE("value", ValueTrigger.class), CRON("cron", CronTrigger.class);

	private final Class<?> className;
	private final String value;

	TriggerTypes(String value, Class<?> className) {
		this.value = (String) value;
		this.className = className;
	}

	/* Reverse enum methods */
	private static ReverseEnumMap<String, TriggerTypes> map = new ReverseEnumMap<String, TriggerTypes>(TriggerTypes.class);

	public String convert() {
		return value;
	}

	public static TriggerTypes convert(String val) {
		return map.get(val);
	}

	public String getValue() {
		return this.value;
	}

}
