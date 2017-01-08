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
package net.yourhome.server.zwave;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;
import org.zwave4j.ControllerCallback;
import org.zwave4j.ControllerCommand;
import org.zwave4j.ControllerError;
import org.zwave4j.ControllerState;
import org.zwave4j.Manager;
import org.zwave4j.NativeLibraryLoader;
import org.zwave4j.Notification;
import org.zwave4j.NotificationWatcher;
import org.zwave4j.Options;
import org.zwave4j.ValueGenre;
import org.zwave4j.ValueId;
import org.zwave4j.ValueType;
import org.zwave4j.ZWave4j;

import net.yourhome.common.base.enums.GeneralCommands;
import net.yourhome.common.base.enums.zwave.ZWaveCommandClassTypes;
import net.yourhome.common.net.messagestructures.zwave.ZWaveValue;
import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.base.Scheduler;
import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.net.rest.zwave.Commands;
import net.yourhome.server.zwave.enums.LogLevel;

public class ZWaveManager {
	private static Logger log = Logger.getLogger("net.yourhome.server.zwave.ZWave");

	private ZWaveNetController zwaveNetController;
	private DatabaseConnector dbConnector;
	private Options m_options = null;
	private Manager m_manager = null;

	private long m_homeId = 0;
	private List<Node> m_nodeList = new ArrayList<Node>();
	private static String m_driverPort = "";
	private volatile static ZWaveManager controller = null;
	private static Object lock = new Object();
	private boolean m_allNodesQueried = false;
	private boolean m_awakeNodesQueried = false;
	private boolean isEnabled = true;
	private Map<Integer, ControllerTransaction> controllerTransactions = new LRUMap(100);

	/* Initialization */
	private ZWaveManager() {
	}

	public static ZWaveManager getInstance() {
		ZWaveManager r = ZWaveManager.controller;
		if (r == null) {
			synchronized (ZWaveManager.lock) { // while we were waiting for
													// the lock, another
				r = ZWaveManager.controller; // thread may have instantiated
												// the object
				if (r == null) {
					r = new ZWaveManager();
					ZWaveManager.controller = r;
				}
			}
		}
		return ZWaveManager.controller;
	}

	public Manager getManager() {
		return this.m_manager;
	}

	public void writeConfig() {
		// Get homeid
		if (this.m_homeId != 0) {
			this.m_manager.writeConfig(this.m_homeId);
		} else {
			ZWaveManager.log.info("Could not save config - Controller not initialized yet?");
		}
	}

	public boolean isEnabled() {
		return this.isEnabled;
	}

	public void initialize() {

		String usbConnection = SettingsManager.getStringValue(this.zwaveNetController.getIdentifier(), ZWaveNetController.Settings.ZWAVE_COM.get());
		String networkKey = SettingsManager.getStringValue(this.zwaveNetController.getIdentifier(), ZWaveNetController.Settings.ZWAVE_KEY.get(), "1234567890123456");

		if (usbConnection != null && !usbConnection.equals("")) {

			NativeLibraryLoader.loadLibrary(ZWave4j.LIBRARY_NAME, ZWave4j.class);

			// Create the Options

			String configPath = new File(SettingsManager.getBasePath(), "/openZWave/config").getAbsolutePath();
			this.m_options = Options.create(configPath, "", "");

			// Add any app specific options here...
			File openZwaveFolder = new File(SettingsManager.getBasePath() + "/openZWave/");
			if (!openZwaveFolder.exists()) {
				openZwaveFolder.mkdirs();
			}

			this.m_options.addOptionString("UserPath", openZwaveFolder.getAbsolutePath() + "/", true); // Write
																										// all
																										// files
																										// zo
																										// openZwave
																										// folder
			this.m_options.addOptionInt("SaveLogLevel", LogLevel.DETAIL.ordinal()); // ordinarily,
			// just
			// write
			// "Detail"
			// level
			// messages
			// to
			// the
			// log
			this.m_options.addOptionInt("QueueLogLevel", LogLevel.DEBUG.ordinal()); // save
			// recent
			// messages
			// with
			// "Debug"
			// level
			// messages
			// to
			// be
			// dumped
			// if
			// an
			// error
			// occurs
			this.m_options.addOptionInt("DumpTriggerLevel", LogLevel.ERROR.ordinal()); // only
																						// "dump"
																						// Debug
																						// to
																						// the
																						// log
																						// emessages
																						// when
																						// an
																						// error-level
																						// message
																						// is
																						// logged
			this.m_options.addOptionBool("ConsoleOutput", false); // Do not
																	// print
																	// output on
																	// console
			this.m_options.addOptionString("LogFileName", "../logs/OZW_Log.txt", true);
			this.m_options.addOptionBool("AppendLogFile", false);

			this.m_options.addOptionString("NetworkKey", networkKey, false);

			// Lock the options
			this.m_options.lock();

			// Create the OpenZWave Manager
			this.m_manager = new Manager();
			Manager.create();

			// Add a driver
			if (usbConnection.startsWith("COM")) {
				ZWaveManager.m_driverPort = "\\\\.\\" + usbConnection;
			} else {
				ZWaveManager.m_driverPort = usbConnection;
			}
			this.m_manager.addDriver(ZWaveManager.m_driverPort);
			// m_manager.AddDriver(@"HID Controller",
			// ZWControllerInterface.Hid);

			this.m_manager.addWatcher(this.watcher, null);

			this.dbConnector = DatabaseConnector.getInstance();
			ZWaveManager.log.info("Initializing");
		} else {

			ZWaveManager.log.info("Could not find Z-Wave settings. Disabling Z-Wave.");
			this.isEnabled = false;
		}
	}

	final NotificationWatcher watcher = new NotificationWatcher() {

		@Override
		public void onNotification(Notification notification, Object context) {

			switch (notification.getType()) {
			case VALUE_ADDED: {
				Node node = ZWaveManager.this.getNode(notification.getHomeId(), notification.getNodeId());

				if (node != null) {
					node.addValue(new Value(ZWaveManager.controller, notification.getValueId()));
				}
				break;
			}

			case VALUE_REMOVED: {
				Node node = ZWaveManager.this.getNode(notification.getHomeId(), notification.getNodeId());
				if (node != null) {
					Value v = node.getValue(notification.getValueId());
					if (v != null) {
						node.removeValue(v);
					}
				}
				break;
			}

			case VALUE_CHANGED: {
				ValueId v = notification.getValueId();
				Object value = ZWaveManager.getValueOfValue(v);
				ZWaveManager.log.debug("Value change: ID=" + (ZWaveManager.this.getValueId(v)) + ", Node: " + v.getNodeId() + ", Instance: " + v.getInstance() + ", Commandclass: " + v.getCommandClassId() + ", value: " + value + " " + ZWaveManager.this.m_manager.getValueUnits(v));
				if (ZWaveManager.this.zwaveNetController != null) {
					ZWaveManager.this.zwaveNetController.ZWaveValueChanged(v);
				}
				break;
			}
			case GROUP: {
				break;
			}
			case NODE_ADDED: {
				// if this node was in zwcfg*.xml, this is the first node
				// notification
				// if not, the NodeNew notification should already have been
				// received
				if (ZWaveManager.this.getNode(notification.getHomeId(), notification.getNodeId()) == null) {
					Node node = new Node(ZWaveManager.controller);
					node.setId(notification.getNodeId());
					node.setHomeId(notification.getHomeId());
					ZWaveManager.this.m_nodeList.add(node);
				}
				break;
			}

			case NODE_NEW: {
				// Add the new node to our list (and flag as uninitialized)
				Node node = new Node(ZWaveManager.controller);
				node.setId(notification.getNodeId());
				node.setHomeId(notification.getHomeId());
				ZWaveManager.this.m_nodeList.add(node);
				break;
			}

			case NODE_REMOVED: {
				for (Node node : ZWaveManager.this.m_nodeList) {
					if (node.getId() == notification.getNodeId()) {
						ZWaveManager.this.m_nodeList.remove(node);
						break;
					}
				}
				break;
			}

			case NODE_PROTOCOL_INFO: {
				Node node = ZWaveManager.this.getNode(notification.getHomeId(), notification.getNodeId());
				if (node != null) {
					node.setLabel(ZWaveManager.this.m_manager.getNodeType(ZWaveManager.this.m_homeId, node.getId()));
				}
				break;
			}

			case NODE_NAMING: {
				Node node = ZWaveManager.this.getNode(notification.getHomeId(), notification.getNodeId());
				if (node != null) {
					node.readProperties();
				}
				break;
			}

			case NODE_EVENT: {
				ValueId v = notification.getValueId();

				Node node = ZWaveManager.this.getNode(notification.getHomeId(), notification.getNodeId());
				if (node != null) {
					Event event = node.getEvent(ZWaveManager.this.getValueId(v));
					if (event == null) {
						node.addEvent(ZWaveManager.this.getEventInformation(v));
					}
				}

				ZWaveManager.log.debug("Event! ID=" + (ZWaveManager.this.getValueId(v)) + ", Node: " + v.getNodeId() + ", Commandclass: " + "0x" + v.getCommandClassId() + ", value: " + ZWaveManager.getValueOfValue(v) + " " + ZWaveManager.this.m_manager.getValueUnits(v));
				// eventTriggered(v);

				break;
			}

			case POLLING_DISABLED: {
				ZWaveManager.log.debug("Polling disabled");
				break;
			}

			case POLLING_ENABLED: {
				ZWaveManager.log.debug("Polling enabled");
				break;
			}

			case DRIVER_READY: {
				ZWaveManager.this.m_homeId = notification.getHomeId();
				break;
			}
			case NODE_QUERIES_COMPLETE: {
				if (ZWaveManager.this.m_awakeNodesQueried) {
					ZWaveManager.log.info("Node queries complete (Node " + notification.getNodeId() + ")");
					Node node = ZWaveManager.this.getNode(notification.getHomeId(), notification.getNodeId());
					ZWaveManager.this.writeConfig();
				}
				break;
			}
			case ESSENTIAL_NODE_QUERIES_COMPLETE: {
				ZWaveManager.log.debug("Essential node queries complete (Node " + notification.getNodeId() + ")");
				// writeConfig();
				break;
			}
			case ALL_NODES_QUERIED: {
				ZWaveManager.this.m_allNodesQueried = true;
				ZWaveManager.this.writeConfig();

				ZWaveManager.log.info("");
				ZWaveManager.log.info("Network initialization finished. Here are the nodes in the network: ");

				ZWaveManager.log.info("" + String.format("%-7s %-30s %-15s %-30s %-20s", "nodeId", "Label", "Manufacturer", "Product", "Location"));
				ZWaveManager.log.info("" + "------------------------------------------------------------------------------------------------------");
				for (Node node : ZWaveManager.this.m_nodeList) {
					ZWaveManager.log.info("" + String.format("%-7s %-30s %-15s %-30s %-20s", node.getId(), node.getLabel(), node.getManufacturer(), node.getProduct(), node.getLocation()));
				}
				ZWaveManager.log.info("");

				break;
			}
			case AWAKE_NODES_QUERIED: {
				ZWaveManager.this.m_awakeNodesQueried = true;
				ZWaveManager.this.m_manager.writeConfig(notification.getHomeId());
				ZWaveManager.this.m_allNodesQueried = true;

				ZWaveManager.log.info("");
				ZWaveManager.log.info("Network initialization finished for awake nodes. Here are the nodes in the network: ");

				ZWaveManager.log.info("" + String.format("%-7s %-30s %-15s %-30s %-20s", "nodeId", "Label", "Manufacturer", "Product", "Location"));
				ZWaveManager.log.info("" + "------------------------------------------------------------------------------------------------------");
				for (Node node : ZWaveManager.this.m_nodeList) {
					ZWaveManager.log.info("" + String.format("%-7s %-30s %-15s %-30s %-20s", node.getId(), node.getLabel(), node.getManufacturer(), node.getProduct(), node.getLocation()));
				}
				ZWaveManager.log.info("");

				ZWaveManager.this.postProcessActivities();

				break;
			}
			case ALL_NODES_QUERIED_SOME_DEAD:
				ZWaveManager.log.info("All nodes queried - some dead");
				ZWaveManager.this.m_awakeNodesQueried = true;
				ZWaveManager.this.writeConfig();
				ZWaveManager.this.m_allNodesQueried = true;
				break;
			case BUTTON_OFF:
				break;
			case BUTTON_ON:
				break;
			case CREATE_BUTTON:
				break;
			case DELETE_BUTTON:
				break;
			case DRIVER_FAILED:
				break;
			case DRIVER_RESET:
				break;
			case NOTIFICATION:
				break;
			case SCENE_EVENT:
				ZWaveManager.log.info("Scene event for scene ID " + notification.getSceneId());
				ZWaveManager.this.zwaveNetController.triggerEvent("Scenes", notification.getSceneId() + "");
				break;
			case VALUE_REFRESHED:
				break;
			default:
				break;
			}
		}
	};

	public void setZWaveNetController(ZWaveNetController zwaveNetController) {
		this.zwaveNetController = zwaveNetController;
	}

	public void postProcessActivities() {
		/* Set values polled / not polled */
		DatabaseConnector dbConnector = DatabaseConnector.getInstance();
		for (Node node : this.m_nodeList) {

			List<DatabaseConnector.ValueSettings> valueSettingsList = dbConnector.getAllZWaveValueSettings(node.getHomeId(), node.getId());
			if (valueSettingsList != null) {
				for (DatabaseConnector.ValueSettings valueSetting : valueSettingsList) {
					// Set value polled
					// log.Debug("Set value polled/not polled: " +
					// valueSetting.valueId + ", node: "+ valueSetting.nodeId);
					this.setValuePolled(valueSetting.nodeId, valueSetting.nodeInstance, valueSetting.homeId, valueSetting.valueId, valueSetting.polled);

					// Set Associations?
				}
			}

		}

		ZWaveManager.log.info("Post process activities completed");
	}

	public boolean isNetworkInitialized() {
		return this.m_allNodesQueried;
	}

	/* ZWave node & value getters */
	public Node getNode(short nodeId) {
		for (Node node : this.m_nodeList) {
			if ((node.getId() == nodeId)) {
				return node;
			}
		}

		return null;
	}

	public Node getNode(long homeId, short nodeId) {
		for (Node node : this.m_nodeList) {
			if ((node.getId() == nodeId) && (node.getHomeId() == homeId)) {
				return node;
			}
		}

		return null;
	}

	public Value getValue(long homeId, short nodeId, short instance, BigInteger valueId) {
		Node node = this.getNode(nodeId);
		if (node != null) {
			return node.getValue(valueId, instance);
		}
		ZWaveManager.log.error("Incorrect valueId requested (id: " + valueId + ")");
		return null;
	}

	public Value getValue(String controlId) {
		ZWaveValue zwaveValue = new ZWaveValue(controlId);
		return this.getValue(zwaveValue.getHomeId(), zwaveValue.getNodeId(), zwaveValue.getInstance(), zwaveValue.getValueId());
	}

	public Value getValue(ValueId valueId) {
		Node node = this.getNode(valueId.getNodeId());
		if (node != null) {
			return node.getValue(valueId);
		}
		ZWaveManager.log.error("Incorrect valueId requested (id: " + valueId + ")");
		return null;
	}

	public List<String> getValueListItems(ValueId valueId) {
		List<String> returnStringList = new ArrayList<String>();
		this.m_manager.getValueListItems(valueId, returnStringList);
		return returnStringList;
	}

	public String getValueListSelection(ValueId valueId) {
		AtomicReference<String> listSelection = new AtomicReference<String>();
		this.m_manager.getValueListSelectionString(valueId, listSelection);
		return listSelection.get();
	}

	public static Object getValueOfValue(ValueId valueId) {
		switch (valueId.getType()) {
		case BOOL:
			AtomicReference<Boolean> b = new AtomicReference<Boolean>();
			Manager.get().getValueAsBool(valueId, b);
			return b.get();
		case BYTE:
			AtomicReference<Short> bb = new AtomicReference<Short>();
			Manager.get().getValueAsByte(valueId, bb);
			return bb.get();
		case DECIMAL:
			AtomicReference<Float> f = new AtomicReference<Float>();
			Manager.get().getValueAsFloat(valueId, f);
			return f.get();
		case INT:
			AtomicReference<Integer> i = new AtomicReference<Integer>();
			Manager.get().getValueAsInt(valueId, i);
			return i.get();
		case LIST:
			return null;
		case SCHEDULE:
			return null;
		case SHORT:
			AtomicReference<Short> s = new AtomicReference<Short>();
			Manager.get().getValueAsShort(valueId, s);
			return s.get();
		case STRING:
			AtomicReference<String> ss = new AtomicReference<String>();
			Manager.get().getValueAsString(valueId, ss);
			return ss.get();
		case BUTTON:
			return null;
		case RAW:
			AtomicReference<short[]> sss = new AtomicReference<short[]>();
			Manager.get().getValueAsRaw(valueId, sss);
			return sss.get();
		default:
			return null;
		}
	}

	public String getValueUnits(ValueId valueId) {
		return this.m_manager.getValueUnits(valueId);
	}

	public List<Node> getNodeList() {
		return this.m_nodeList;
	}
	/*
	 * public List<Node> getExtendedNodeList(boolean includeConfigurationValues)
	 * { List<Node> textNodeList = new ArrayList<Node>();
	 * 
	 * for (Node node : getNodeList()) {
	 * textNodeList.add(getNodeInformation(node, includeConfigurationValues)); }
	 * 
	 * return textNodeList; }
	 */

	public String getValueLabel(ValueId valueId) {
		return this.m_manager.getValueLabel(valueId);
	}

	public String getValueHelp(ValueId valueId) {
		return this.m_manager.getValueHelp(valueId);
	}

	public Boolean getValueReadOnly(ValueId valueId) {
		return this.m_manager.isValueReadOnly(valueId);
	}

	public Boolean getValuePolled(ValueId valueId) {
		return this.m_manager.isValuePolled(valueId);
	}

	public void allOn(long homeId) {
		this.m_manager.switchAllOn(homeId);
		this.zwaveNetController.triggerEvent(GeneralCommands.getNodeIdentifier(), GeneralCommands.ALL_ON.convert());
		this.performSoftRefresh(true);
	}

	private boolean refreshingNodes = false;

	private void performSoftRefresh(boolean offNodes) {
		if (!this.refreshingNodes) {
			this.refreshingNodes = true;
			int secondsDelay = 0;

			// Update values for switches and dimmers : keep 6 seconds between
			// each node to avoid network saturation!
			for (Node node : this.m_nodeList) {
				for (final Value zwaveValue : node.getValues()) {
					ValueId valueId = zwaveValue.getOriginalValueId();
					if (valueId.getGenre() == ValueGenre.USER) {
						ZWaveCommandClassTypes commandClass = ZWaveCommandClassTypes.fromByte((byte) (valueId.getCommandClassId()));
						boolean refreshValueNeeded = false;
						switch (commandClass) {
						case SwitchBinary:
						case SwitchMultilevel:
							if (valueId.getType() == ValueType.INT) {
								Integer value = (Integer) ZWaveManager.getValueOfValue(valueId);
								if (value != 0) {
									refreshValueNeeded = true;
								}
							} else if (valueId.getType() == ValueType.BYTE) {
								short value = (Short) ZWaveManager.getValueOfValue(valueId);
								if (value != 0) {
									refreshValueNeeded = true;
								}
							} else if (valueId.getType() == ValueType.BOOL) {
								Boolean value = (Boolean) ZWaveManager.getValueOfValue(valueId);
								if (value) {
									refreshValueNeeded = true;
								}
							} else if (valueId.getType() == ValueType.DECIMAL) {
								Float value = (Float) ZWaveManager.getValueOfValue(valueId);
								if (value != 0) {
									refreshValueNeeded = true;
								}
							}
							break;
						default:
							break;
						}

						if (refreshValueNeeded) {
							try {
								Scheduler.getInstance().schedule(new TimerTask() {
									@Override
									public void run() {
										ZWaveManager.this.m_manager.refreshValue(valueId);
									}
								}, (new Date(new Date().getTime() + secondsDelay * 1000)), 0);
							} catch (Exception e) {
								ZWaveManager.log.error("Error in schedule");
							}
							secondsDelay += 6;
						}
					}
				}
			}
			this.refreshingNodes = false;
		}
	}

	public void allOff(long homeId) {
		this.m_manager.switchAllOff(homeId);
		this.zwaveNetController.triggerEvent(GeneralCommands.getNodeIdentifier(), GeneralCommands.ALL_OFF.convert());
		this.performSoftRefresh(false);
	}

	/* ZWave node & value setters */
	public void setNodeBasic(long homeId, short nodeId, int value) {
		Node node = this.getNode(homeId, nodeId);

		if (node != null) {
			this.m_manager.setNodeLevel(node.getHomeId(), node.getId(), (short) value);
		}
	}

	public boolean setValuePolled(int nodeId, short instance, long homeId, BigInteger valueId, boolean polled) {
		Node node = this.getNode(homeId, (byte) nodeId);
		Value value = node.getValue(valueId, instance);

		if (value != null && polled) {
			return this.m_manager.enablePoll(value.getOriginalValueId());
		} else if (value != null & !polled) {
			return this.m_manager.disablePoll(value.getOriginalValueId());
		}
		return false;
	}

	public Boolean setConfiguration(long homeId, short nodeId, Byte param, int value) {
		ZWaveManager.log.info("Set configuration: " + "homeId: " + homeId + ", nodeId:" + nodeId + ", param: " + param + ", Value: " + value);

		return this.m_manager.setConfigParam(homeId, nodeId, param, value);
	}

	public ValueId setValue(long homeId, short nodeId, short instance, BigInteger valueId, String val) {
		Node node = this.getNode(homeId, nodeId);
		if (node != null) {
			final Value valueObj = node.getValue(valueId, instance);
			ValueId valueIdObj = valueObj.getOriginalValueId();

			ZWaveManager.log.info("Set value: " + "homeId: " + homeId + ", nodeId:" + nodeId + ", instance: " + instance + ", valueid:" + valueId + ", Value: " + val);

			switch (valueIdObj.getType()) {
			case BOOL:
				boolean boolValue;
				if (val.equals("true") || val.equals("1")) {
					boolValue = true;
				} else {
					boolValue = false;
				}
				this.m_manager.setValueAsBool(valueIdObj, boolValue);
				break;
			case STRING:
				this.m_manager.setValueAsString(valueIdObj, val);
				break;
			case LIST:
				boolean result = this.m_manager.setValueListSelection(valueIdObj, val);
				break;
			default:
				this.m_manager.setValueAsString(valueIdObj, val);
				break;
			}
			return valueIdObj;
		}
		return null;
	}

	/* Associations */
	public void addAssociation(long homeId, short nodeId, int groupID, int targetnodeId) {
		this.m_manager.addAssociation(homeId, (byte) nodeId, (byte) groupID, (byte) targetnodeId);
	}

	public void removeAssociation(long homeId, short nodeId, int groupID, int targetnodeId) {
		this.m_manager.removeAssociation(homeId, (byte) nodeId, (byte) groupID, (byte) targetnodeId);
	}

	public int getMaxAssociations(long homeId, int nodeId, int groupID) {
		return this.m_manager.getMaxAssociations(homeId, (byte) nodeId, (byte) groupID);
	}

	/* ZWave Network commands */
	public void healNetwork() {
		ZWaveManager.log.info("Healing network");
		this.m_manager.healNetwork(this.m_homeId, true);
	}

	public void healNetworkNode(long homeId, short nodeId) {
		ZWaveManager.log.info("Healing network node " + nodeId + " in home " + homeId);
		this.m_manager.healNetworkNode(this.m_homeId, nodeId, true);
	}

	public void resetNetwork() {
		ZWaveManager.log.info("Resetting network controller in home " + this.m_homeId);
		this.m_manager.resetController(this.m_homeId);
	}

	public long getDefaultHomeId() {
		return this.m_homeId;
	}

	public ControllerTransaction getTransactionStatus(Integer transactionId) {
		return this.controllerTransactions.get(transactionId);
	}

	public boolean cancelTransaction(Integer transactionId) {
		ControllerTransaction transaction = this.controllerTransactions.get(transactionId);
		if (transaction != null) {
			return this.m_manager.cancelControllerCommand(transaction.getHomeId());
		}
		return false;
	}

	public ControllerTransaction addNode() {
		ControllerTransaction transaction = new ControllerTransaction(ControllerCommand.ADD_DEVICE, this.m_homeId);
		this.controllerTransactions.put(transaction.getId(), transaction);
		ZWaveManager.log.info("Start inclusion in network");
		this.m_manager.beginControllerCommand(this.m_homeId, ControllerCommand.ADD_DEVICE, new ControllerCallback() {
			@Override
			public void onCallback(ControllerState controllerState, ControllerError controllerError, Object arg2) {
				transaction.setState(controllerState);
				transaction.setError(controllerError);
				ZWaveManager.log.info("State: " + controllerState.name() + ", Error: " + controllerError.name());
			}
		});

		return transaction;
	}

	public ControllerTransaction removeNode(long homeId, short nodeId) {
		ControllerTransaction transaction = new ControllerTransaction(ControllerCommand.REMOVE_FAILED_NODE, homeId);
		this.controllerTransactions.put(transaction.getId(), transaction);
		ZWaveManager.log.info("Removing failed node " + nodeId + " in home " + homeId);
		this.m_manager.beginControllerCommand(homeId, ControllerCommand.REMOVE_FAILED_NODE, new ControllerCallback() {
			@Override
			public void onCallback(ControllerState controllerState, ControllerError controllerError, Object arg2) {
				transaction.setState(controllerState);
				transaction.setError(controllerError);
				ZWaveManager.log.info("State: " + controllerState.name() + ", Error: " + controllerError.name());
			}
		}, null, false, nodeId);
		return transaction;
	}

	public ControllerTransaction removeNode() {
		ZWaveManager.log.info("Start deletion of node in network in home " + this.m_homeId);
		ControllerTransaction transaction = new ControllerTransaction(ControllerCommand.REMOVE_DEVICE, this.m_homeId);
		this.controllerTransactions.put(transaction.getId(), transaction);
		this.m_manager.beginControllerCommand(this.m_homeId, ControllerCommand.REMOVE_DEVICE, new ControllerCallback() {
			@Override
			public void onCallback(ControllerState controllerState, ControllerError controllerError, Object arg2) {
				transaction.setState(controllerState);
				transaction.setError(controllerError);
				ZWaveManager.log.info("State: " + controllerState.name() + ", Error: " + controllerError.name());
			}
		});
		return transaction;
	}

	public short[] getNeighbours(long homeId, short nodeId) {
		AtomicReference<short[]> neighbours = new AtomicReference<short[]>();
		this.m_manager.getNodeNeighbors(homeId, nodeId, neighbours);
		return neighbours.get();
	}

	/* Network helpers */
	public List<Commands.ZWaveCommand> getCommandList(Commands requester) {
		List<Commands.ZWaveCommand> commandList = new ArrayList<Commands.ZWaveCommand>();
		commandList.add(requester.new ZWaveCommand("All off", "allOff"));
		commandList.add(requester.new ZWaveCommand("All on", "allOn"));

		return commandList;
	}

	/* Scene methods */
	public List<ZWaveScene> getAllScenes() {
		AtomicReference<short[]> allScenesRef = new AtomicReference<short[]>();
		this.m_manager.getAllScenes(allScenesRef);
		short[] allScenes = allScenesRef.get();
		List<ZWaveScene> allSceneList = new ArrayList<ZWaveScene>();
		for (short allScene : allScenes) {
			ZWaveScene s;
			try {
				s = new ZWaveScene(allScene, true);
				allSceneList.add(s);
			} catch (Exception e) {
			}
		}
		return allSceneList;
	}

	public ZWaveScene getScene(short sceneId) throws Exception {
		ZWaveScene scene = null;
		// if( this.m_manager.sceneExists(sceneId) ) {
		scene = new ZWaveScene(sceneId, true);
		// }
		return scene;
	}

	public ZWaveScene createScene() {
		short sceneId = this.m_manager.createScene();
		this.writeConfig(); // Only write the scene config
		try {
			return this.getScene(sceneId);
		} catch (Exception e) {
			return null;
		}
	}

	public boolean removeScene(short scene) {
		boolean result = this.m_manager.removeScene(scene);
		this.writeConfig(); // Only write the scene config
		return result;
	}

	private boolean isSceneActive(ZWaveScene scene) {
		try {
			// Loop through the scene values and check if the scene is currently
			// activated or not.
			for (ZWaveSceneValue v : scene.getValues()) {
				// Get current value of this valueId
				AtomicReference<String> currentValue = new AtomicReference<String>();
				this.m_manager.getValueAsString(v.getValueId(), currentValue);

				// If all values are the same as the scene values, revert the
				// scene.
				if (!currentValue.get().toLowerCase().equals(v.getValue().toLowerCase())) {
					return false;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return scene.getValues().isEmpty() ? false : true;
	}

	private String activateReversedScene(ZWaveScene sceneDetails) {

		for (ZWaveSceneValue v : sceneDetails.getValues()) {
			switch (v.getValueId().getType()) {
			case BOOL:
			case BYTE:
			case DECIMAL:
			case INT:
			case SHORT:
			case STRING:
				this.setValue(v.getValueId().getHomeId(), v.getValueId().getNodeId(), v.getValueId().getInstance(), this.getValueId(v.getValueId()), "0");
				break;
			default:
				// Do nothing
				break;
			}
		}
		ZWaveManager.log.info("Scene '" + sceneDetails.getLabel() + "' was active so has been deactivated");
		return "Scene " + sceneDetails.getLabel() + " was active so has been deactivated";
	}

	public String activateScene(short scene) {
		String result = "";

		if (this.m_manager.sceneExists(scene)) {
			ZWaveScene sceneDetails;
			try {
				sceneDetails = this.getScene(scene);
				boolean needReverse = this.isSceneActive(sceneDetails);
				if (!needReverse) {
					boolean resultBool = this.m_manager.activateScene(scene);
					if (resultBool) {
						ZWaveManager.log.info("Scene '" + sceneDetails.getLabel() + "' activated");
						result = "Scene " + sceneDetails.getLabel() + " activated";
					} else {
						ZWaveManager.log.error("Failed to start scene '" + sceneDetails.getLabel() + "' ");
						result = "Failed to start scene " + sceneDetails.getLabel();
					}
				} else {
					// Do not trigger scene listeners (?)
					return this.activateReversedScene(sceneDetails);
				}
			} catch (Exception e) {
				ZWaveManager.log.error(e.getMessage());
			}
		} else {
			ZWaveManager.log.error("Failed to start scene - Scene id " + scene + " does not exist!");
		}

		return result;
	}

	private String getSceneValue(short scene, ValueId valueId) {

		String resultString = null;
		switch (valueId.getType()) {
		case BOOL:
		case BUTTON:
			AtomicReference<Boolean> resultBool = new AtomicReference<Boolean>();
			this.m_manager.sceneGetValueAsBool(scene, valueId, resultBool);
			resultString = resultBool.get().toString();
			break;
		case BYTE:
		case SHORT:
			AtomicReference<Short> resultShort = new AtomicReference<Short>();
			this.m_manager.sceneGetValueAsByte(scene, valueId, resultShort);
			resultString = resultShort.get().toString();
			break;
		case INT:
			AtomicReference<Integer> resultInteger = new AtomicReference<Integer>();
			this.m_manager.sceneGetValueAsInt(scene, valueId, resultInteger);
			resultString = resultInteger.get() + "";
			break;
		case DECIMAL:
			AtomicReference<Float> resultFloat = new AtomicReference<Float>();
			this.m_manager.sceneGetValueAsFloat(scene, valueId, resultFloat);
			resultString = resultFloat.get().toString();
			break;
		case SCHEDULE:
		case RAW:
		case LIST:
		case STRING:
			AtomicReference<String> result = new AtomicReference<String>();
			this.m_manager.sceneGetValueAsString(scene, valueId, result);
			resultString = result.get();
			break;
		}
		return resultString;
	}

	public List<ValueId> getSceneValues(short scene) {
		List<ValueId> valueIdsInScene = new ArrayList<ValueId>();
		this.m_manager.sceneGetValues(scene, valueIdsInScene);
		return valueIdsInScene;
	}

	public class ZWaveScene {

		private short id;
		private String label;
		private String iconUrl;
		private List<ZWaveSceneValue> values;
		public boolean valuesRead = false;

		/**
		 * @return the iconUrl
		 */
		public String getIconUrl() {
			return this.iconUrl;
		}

		/**
		 * @param iconUrl
		 *            the iconUrl to set
		 */
		public void setIconUrl(String iconUrl) {
			this.iconUrl = iconUrl;
			this.updateLabelAndIcon();
		}

		public ZWaveScene(short sceneId, boolean readValues) throws Exception {
			if (ZWaveManager.this.m_manager.sceneExists(sceneId) != true) {
				throw new Exception("Scene " + sceneId + " does not exist.");
			}
			this.id = sceneId;
			String[] labelSplitted = ZWaveManager.this.m_manager.getSceneLabel(sceneId).split("##");
			if (labelSplitted.length == 1) {
				this.label = labelSplitted[0];
				this.iconUrl = "";
			} else if (labelSplitted.length > 1) {
				this.label = labelSplitted[0];
				this.iconUrl = labelSplitted[1];
			}
			this.values = new ArrayList<ZWaveSceneValue>();
			if (readValues) {
				this.readValues();
			}
		}

		public void readValues() {
			if (this.values != null) {
				this.values.clear();
			}
			for (ValueId valueIdInScene : ZWaveManager.this.getSceneValues(this.id)) {
				ZWaveSceneValue sceneValue = new ZWaveSceneValue(this.id, valueIdInScene);
				if (sceneValue != null) {
					this.values.add(sceneValue);
				}
			}
			this.valuesRead = true;
		}

		public boolean hasSceneValue(ValueId valueId) {
			if (!this.valuesRead) {
				this.readValues();
			}

			for (ZWaveSceneValue sceneValue : this.values) {
				ValueId sceneValueId = sceneValue.getValueId();
				if (sceneValueId.equals(valueId)) {
					return true;
				}
			}
			return false;
		}

		public boolean removeSceneValue(ValueId valueId) {
			boolean result = false;
			if (this.hasSceneValue(valueId)) {
				result = ZWaveManager.this.m_manager.removeSceneValue(this.id, valueId);
				if (result) {
					for (ZWaveSceneValue value : this.values) {
						if (value.getValueId().equals(valueId)) {
							this.values.remove(value);
						}
					}
					ZWaveManager.this.writeConfig(); // Only write the scene
														// config
				}
			}
			return result;
		}

		public boolean addSceneValue(short scene, ValueId valueId, String value) {
			boolean result = false;
			switch (valueId.getType()) {
			case BOOL:
			case BUTTON:
				result = ZWaveManager.this.m_manager.addSceneValueAsBool(scene, valueId, Boolean.parseBoolean(value));
				break;
			case BYTE:
				result = ZWaveManager.this.m_manager.addSceneValueAsByte(scene, valueId, Byte.parseByte(value));
				break;
			case SHORT:
				result = ZWaveManager.this.m_manager.addSceneValueAsShort(scene, valueId, Short.parseShort(value));
				break;
			case INT:
				result = ZWaveManager.this.m_manager.addSceneValueAsInt(scene, valueId, Integer.parseInt(value));
				break;
			case DECIMAL:
				result = ZWaveManager.this.m_manager.addSceneValueAsFloat(scene, valueId, Float.parseFloat(value));
				break;
			case SCHEDULE:
			case RAW:
			case LIST:
			case STRING:
				result = ZWaveManager.this.m_manager.addSceneValueAsString(scene, valueId, value);
				break;
			}

			if (result) {
				ZWaveManager.this.writeConfig(); // Only write the scene
													// config
				ZWaveManager.log.info("Value added to scene " + scene);
			}

			return result;
		}

		public boolean setSceneValue(short scene, ValueId valueId, String value) {
			boolean result = false;

			switch (valueId.getType()) {
			case BOOL:
			case BUTTON:
				result = ZWaveManager.this.m_manager.setSceneValueAsBool(scene, valueId, Boolean.parseBoolean(value));
				break;
			case BYTE:
				result = ZWaveManager.this.m_manager.setSceneValueAsInt(scene, valueId, Integer.parseInt(value));
				// result = m_manager.setSceneValueAsByte(scene, valueId,
				// Byte.parseByte(value));
				break;
			case SHORT:
				result = ZWaveManager.this.m_manager.setSceneValueAsShort(scene, valueId, Short.parseShort(value));
				break;
			case INT:
				result = ZWaveManager.this.m_manager.setSceneValueAsInt(scene, valueId, Integer.parseInt(value));
				break;
			case DECIMAL:
				result = ZWaveManager.this.m_manager.setSceneValueAsFloat(scene, valueId, Float.parseFloat(value));
				break;
			case SCHEDULE:
			case RAW:
			case LIST:
			case STRING:
				result = ZWaveManager.this.m_manager.setSceneValueAsString(scene, valueId, value);
				break;
			}

			if (result) {
				ZWaveManager.this.writeConfig(); // Only write the scene
													// config
				ZWaveManager.log.info("Value changed in scene " + scene);
			}

			return result;
		}

		/**
		 * @return the id
		 */
		public short getId() {
			return this.id;
		}

		/**
		 * @param id
		 *            the id to set
		 */
		public void setId(short id) {
			this.id = id;
		}

		/**
		 * @return the label
		 */
		public String getLabel() {
			return this.label;
		}

		/**
		 * @param label
		 *            the label to set
		 * @return
		 */
		public void setLabel(String label) {
			this.label = label;
			this.updateLabelAndIcon();
		}

		private void updateLabelAndIcon() {
			ZWaveManager.this.m_manager.setSceneLabel(this.id, this.label + "##" + this.iconUrl);
		}

		/**
		 * @return the values
		 */
		public List<ZWaveSceneValue> getValues() {
			return this.values;
		}

	}

	public class ZWaveSceneValue {
		private short sceneId;
		private ValueId valueId;
		private String value;

		public ZWaveSceneValue(short sceneId, ValueId valueId) {
			this.sceneId = sceneId;
			this.valueId = valueId;
			this.value = ZWaveManager.this.getSceneValue(sceneId, valueId);
		}

		/**
		 * @return the sceneId
		 */
		public short getSceneId() {
			return this.sceneId;
		}

		/**
		 * @param sceneId
		 *            the sceneId to set
		 */
		public void setSceneId(short sceneId) {
			this.sceneId = sceneId;
		}

		/**
		 * @return the valueId
		 */
		public ValueId getValueId() {
			return this.valueId;
		}

		/**
		 * @return the value
		 */
		public String getValue() {
			return this.value;
		}

		/**
		 * @param value
		 *            the value to set
		 */
		public void setValue(String value) {
			this.value = value;
		}
	}

	/*
	 * public Node getNodeInformation(Node node, boolean
	 * includeConfigurationValues) { /*Node textNode; try { textNode = new
	 * Node();
	 * 
	 * 
	 * textNode.setId(node.getId()); textNode.setHomeId(node.getHomeId());
	 * textNode.setName(node.getName()); textNode.setLocation(; = node.location;
	 * textNode.label = node.label; textNode.manufacturer = node.manufacturer;
	 * textNode.manufacturerId = node.manufacturerId; textNode.product =
	 * node.product; textNode.productType = node.productType; textNode.productId
	 * = node.productId;
	 * textNode.setAlive(!this.m_manager.isNodeFailed(node.getHomeId(),
	 * node.getId())); textNode.setNeighbours(getNeighbours(node.getHomeId(),
	 * node.getId()));
	 * 
	 * // Convert values for (Value value : node.getValues()) { if
	 * (!includeConfigurationValues && value.getOriginalValueId().getGenre() !=
	 * ValueGenre.CONFIG) { textNode.removeValue(value); } }
	 * 
	 * for (org.zwave4j.ValueId valueId : node.getEvents()) {
	 * textNode.events.add(getEventInformation(valueId)); }
	 */

	// Get associations and add info
	// textNode.maxAssociations = getMaxAssociations((int)node.homeId,
	// (int)node.id, 1);

	/*
	 * return textNode; } catch (CloneNotSupportedException e) { log.error(e); }
	 * return null; }
	 */

	public BigInteger getValueId(ValueId valueId) {
		BigInteger integer = BigInteger.valueOf((valueId.getNodeId() << 24) | (valueId.getGenre().ordinal() << 22) | (valueId.getCommandClassId() << 14) | (valueId.getIndex() << 4) | (valueId.getType().ordinal()));
		return integer;
	}

	public List<AssociationGroup> getAssociationGroups(long homeId, short nodeId) {

		List<AssociationGroup> associationGroupsList = new ArrayList<AssociationGroup>();
		short numberOfGroups = this.m_manager.getNumGroups(homeId, nodeId);
		for (short associationGroup = 1; associationGroup <= numberOfGroups; associationGroup++) {
			String groupLabel = this.m_manager.getGroupLabel(homeId, nodeId, associationGroup);
			int maxAssociations = this.m_manager.getMaxAssociations(homeId, nodeId, associationGroup);
			associationGroupsList.add(new AssociationGroup(associationGroup, maxAssociations, groupLabel));
		}
		return associationGroupsList;
	}

	public List<Association> getAssociations(long homeId, short nodeId) {
		List<Association> associationsList = new ArrayList<Association>();
		// short[] associations = getAssociations(homeId,nodeId, 1);
		short numberOfGroups = this.m_manager.getNumGroups(homeId, nodeId);
		// log.debug("Number of groups: "+numberOfGroups);

		AtomicReference<short[]> associations = new AtomicReference<short[]>();

		for (short i = 1; i <= numberOfGroups; i++) {

			// log.debug("Get group: "+i);
			this.m_manager.getAssociations(homeId, nodeId, i, associations);
			short[] associationsArray = associations.get();
			// log.debug("Group retrieved");
			if (associationsArray != null && associationsArray.length != 0) {
				// log.debug("" + associationsArray.length + "
				// associations found");
				for (short associationTo : associationsArray) {
					associationsList.add(new Association(nodeId, associationTo, i));
				}
			}
		}

		return associationsList;
	}

	/*
	 * public Value getValueInformation(ValueId valueId) { Value value = new
	 * Value(valueId);
	 * value.setValue(String.valueOf(ZWaveController.getValue(valueId)));
	 * value.setValueUnit(getValueUnits(valueId));
	 * value.setValueId(getValueId(valueId));
	 * value.setHelp(getValueHelp(valueId)); value.setCommandClass((byte)
	 * valueId.getCommandClassId()); value.setValueGenre(valueId.getGenre());
	 * value.setValueGenreTxt(valueId.getGenre().toString());
	 * value.setValueType(valueId.getType());
	 * value.setValueTypeTxt(valueId.getType().toString());
	 * value.setHomeId(valueId.getHomeId()); value.setValueIndex((byte)
	 * valueId.getIndex()); value.setValueLabel(getValueLabel(valueId));
	 * value.setPolled(getValuePolled(valueId));
	 * value.setReadOnly(getValueReadOnly(valueId));
	 * value.setInstance(valueId.getInstance());
	 * value.setMaxValue(this.m_manager.getValueMax(valueId));
	 * value.setNodeId(valueId.getNodeId());
	 * value.setHomeId(valueId.getHomeId());
	 * 
	 * // If the valuetype is LIST, also add the possible options: if
	 * (valueId.getType() == ValueType.LIST) {
	 * value.setValueList(getValueListItems(valueId));
	 * value.setValueListSelection(getValueListSelection(valueId)); } else {
	 * value.setValueList(null); }
	 * 
	 * // Read details from DB and overwrite // complete settings
	 * DatabaseConnector.ValueSettings valueSettings =
	 * dbConnector.getZWaveValueSettings(value.getHomeId(), valueId.getNodeId(),
	 * value.getValueId(), value.getInstance());
	 * value.setSubscribed(valueSettings.subscribed);
	 * 
	 * String alias =
	 * dbConnector.getAlias(this.zwaveNetController.getIdentifier(),
	 * ZWaveController.getNodeIdentifier(valueId.getNodeId(),
	 * valueId.getHomeId()), value.getControlId()); if (alias != null) {
	 * value.setValueLabel(alias); }
	 * value.setSubscribed(valueSettings.subscribed);
	 * 
	 * return value; }
	 */

	public static String getNodeIdentifier(short id, long homeId) {
		return "ZN_N" + id + "_H" + homeId;
	}

	public Node getNodeFromIdentifier(String nodeIdentifier) {
		String[] splittedNodeIdentifier = nodeIdentifier.split("_");
		if (splittedNodeIdentifier.length == 3) {
			return this.getNode((Short.valueOf(splittedNodeIdentifier[1].substring(1))));
		}
		return null;
	}

	public Event getEventInformation(ValueId valueId) {
		Event event = new Event(valueId);
		event.setValueId(this.getValueId(valueId).toString());
		event.setValueLabel(this.getValueLabel(valueId));
		event.setInstance(valueId.getInstance());
		event.setHomeId(valueId.getHomeId());
		return event;
	}

	public void destroy() {
		if (this.m_manager != null) {
			Manager.destroy();
		}
		this.m_allNodesQueried = false;
		if (this.m_nodeList != null) {
			this.m_nodeList.clear();
		}
		this.m_homeId = 0;
		this.m_options = null;
	}

}
