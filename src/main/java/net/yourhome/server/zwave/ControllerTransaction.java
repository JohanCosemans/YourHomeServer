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
 * THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
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

import org.zwave4j.ControllerCommand;
import org.zwave4j.ControllerError;
import org.zwave4j.ControllerState;

public class ControllerTransaction {
	private int id;
	private long homeId;
	private ControllerCommand command;
	private ControllerState state;
	private ControllerError error;

	private static int controllerTransactionCounter = 1;

	public ControllerTransaction(ControllerCommand command, long homeId) {
		this.id = ControllerTransaction.controllerTransactionCounter++;
		this.command = command;
		this.homeId = homeId;
	}

	public String getCommandTxt() {
		if (this.command != null) {
			switch (this.command) {
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
		if (this.state != null) {
			switch (this.state) {
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
		if (this.error != null) {
			switch (this.error) {
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
		return this.homeId;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * @return the command
	 */
	public ControllerCommand getCommand() {
		return this.command;
	}

	/**
	 * @param command
	 *            the command to set
	 */
	public void setCommand(ControllerCommand command) {
		this.command = command;
	}

	/**
	 * @return the state
	 */
	public ControllerState getState() {
		return this.state;
	}

	/**
	 * @param state
	 *            the state to set
	 */
	public void setState(ControllerState state) {
		this.state = state;
	}

	/**
	 * @return the error
	 */
	public ControllerError getError() {
		return this.error;
	}

	/**
	 * @param error
	 *            the error to set
	 */
	public void setError(ControllerError error) {
		this.error = error;
	}

}
