package net.yourhome.server.zwave;

import org.zwave4j.ControllerCommand;
import org.zwave4j.ControllerError;
import org.zwave4j.ControllerState;

public class ControllerTransaction {
	private int id;
	private long homeId;
	private ControllerCommand command;
	private ControllerState state;
	private ControllerError error;
	
	private static int controllerTransactionCounter=1;
	public ControllerTransaction(ControllerCommand command, long homeId) {
		this.id = ControllerTransaction.controllerTransactionCounter++;
		this.command = command;
		this.homeId = homeId;
	}
	public String getCommandTxt() {
		if(command != null) {
			switch(command) {
			case ADD_DEVICE:
				return "Add a new device";
			case ASSIGN_RETURN_ROUTE:
				return "Assing return route";
			case CREATE_BUTTON:
				return "Create button";
			case CREATE_NEW_PRIMARY:
				return "Create new primary";
			case DELETE_ALL_RETURN_ROUTES:
				return "Delete all return routes";
			case DELETE_BUTTON:
				return "Delete button";
			case HAS_NODE_FAILED:
				return "Has node failed";
			case NONE:
				return "None";
			case RECEIVE_CONFIGURATION:
				return "Receive configuration";
			case REMOVE_DEVICE:
				return "Remove a device";
			case REMOVE_FAILED_NODE:
				return "Remove a failed node";
			case REPLACE_FAILED_NODE:
				return "Replace a failed node";
			case REPLICATION_SEND:
				return "Send replicatoin";
			case REQUEST_NETWORK_UPDATE:
				return "Request network update";
			case REQUEST_NODE_NEIGHBOR_UPDATE:
				return "Request node neighbour update";
			case SEND_NODE_INFORMATION:
				return "Send node information";
			case TRANSFER_PRIMARY_ROLE:
				return "Transfer primary role";
			default:
				break;
			
			}
		}
		return "Unknown";
	}
	
	/**
	 * @return the stateTxt
	 */
	public String getStateTxt() {
		if(state != null) {
			switch(state) {
			case CANCEL:
				return "Cancelled";
			case COMPLETED:
				return "Completed";
			case ERROR:
				return "Error";
			case FAILED:
				return "Failed";
			case IN_PROGRESS:
				return "In Progress";
			case NODE_FAILED:
				return "Node Failed";
			case NODE_OK:
				return "Node Ok";
			case NORMAL:
				return "Normal";
			case SLEEPING:
				return "Sleeping";
			case STARTING:
				return "Starting...";
			case WAITING:
				return "Waiting...";
			default:
				break;
			
			}
		}
		return "Unknown";
	}

	/**
	 * @return the errorTxt
	 */
	public String getErrorTxt() {
		if(error != null) {
			switch(error) {
			case BUSY:
				return "Controller busy";
			case BUTTON_NOT_FOUND:
				return "Button not found";
			case DISABLED:
				return "Controller disabled";
			case FAILED:
				return "Failed";
			case IS_PRIMARY:
				return "Is Primary";
			case NODE_NOT_FOUND:
				return "Node not found";
			case NONE:
				return "None";
			case NOT_BRIDGE:
				return "Controller is not a bridge";
			case NOT_FOUND:
				return "Not found";
			case NOT_PRIMARY:
				return "Controller is not primary";
			case NOT_SECONDARY:
				return "Controller is not secondary";
			case NOT_SUC:
				return "Not succeeded";
			case OVERFLOW:
				return "Overflow";
			default:
				break;
			
			}
		}
		return "Unknown";
	}

	public long getHomeId() {
		return homeId;
	}
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @return the command
	 */
	public ControllerCommand getCommand() {
		return command;
	}
	/**
	 * @param command the command to set
	 */
	public void setCommand(ControllerCommand command) {
		this.command = command;
	}
	/**
	 * @return the state
	 */
	public ControllerState getState() {
		return state;
	}
	/**
	 * @param state the state to set
	 */
	public void setState(ControllerState state) {
		this.state = state;
	}
	/**
	 * @return the error
	 */
	public ControllerError getError() {
		return error;
	}
	/**
	 * @param error the error to set
	 */
	public void setError(ControllerError error) {
		this.error = error;
	}
	
}
