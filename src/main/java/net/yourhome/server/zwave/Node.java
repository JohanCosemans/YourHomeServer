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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.zwave4j.ValueGenre;
import org.zwave4j.ValueId;
import org.zwave4j.ValueType;

import net.yourhome.server.base.DatabaseConnector;

public class Node {
	private short id;
	private long homeId;
	private String name;
	private String location;
	private String label;
	private String manufacturer;
	private String manufacturerId;
	private String product;
	private String productId;
	private String productType;
	private List<Value> values = new ArrayList<Value>();
	private List<Event> events = new ArrayList<Event>();
	private List<Association> associations = new ArrayList<Association>();
	private List<AssociationGroup> associationGroups;
	private boolean isAlive;
	private short[] neighbours;

	private ZWaveManager controller;

	public Node(ZWaveManager controller) {
		this.controller = controller;
	}

	public void readProperties() {
		this.setManufacturer(this.controller.getManager().getNodeManufacturerName(this.homeId, this.getId()));
		this.setManufacturerId(this.controller.getManager().getNodeManufacturerId(this.homeId, this.getId()));
		this.setProduct(this.controller.getManager().getNodeProductName(this.homeId, this.getId()));
		this.setProductId(this.controller.getManager().getNodeProductId(this.homeId, this.getId()));
		this.setProductType(this.controller.getManager().getNodeProductType(this.homeId, this.getId()));
		this.setLocation(this.controller.getManager().getNodeLocation(this.homeId, this.getId()));
		this.setName(this.controller.getManager().getNodeName(this.homeId, this.getId()));
		this.setAssociations(this.controller.getAssociations(this.getHomeId(), this.getId()));
		this.setAssociationGroups(this.controller.getAssociationGroups(this.getHomeId(), this.getId()));
		this.setAlive(!this.controller.getManager().isNodeFailed(this.getHomeId(), this.getId()));

		// Add/complete settings from DB
		String alias = DatabaseConnector.getInstance().getAlias(ZWaveNetController.getInstance().getIdentifier(), this.getControlId());
		if (alias != null) {
			this.setLabel(alias);
		}
	}

	/**
	 * @return the productId
	 */
	public String getProductId() {
		return this.productId;
	}

	/**
	 * @param productId
	 *            the productId to set
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}

	/**
	 * @return the productType
	 */
	public String getProductType() {
		return this.productType;
	}

	/**
	 * @param productType
	 *            the productType to set
	 */
	public void setProductType(String productType) {
		this.productType = productType;
	}

	public String getControlId() {
		return ZWaveManager.getNodeIdentifier(this.id, this.homeId);
	}

	/**
	 * @return the associationGroups
	 */
	public List<AssociationGroup> getAssociationGroups() {
		return this.associationGroups;
	}

	/**
	 * @param associationGroups
	 *            the associationGroups to set
	 */
	public void setAssociationGroups(List<AssociationGroup> associationGroups) {
		this.associationGroups = associationGroups;
	}

	/**
	 * @return the events
	 */
	public List<Event> getEvents() {
		return this.events;
	}

	/**
	 * @param events
	 *            the events to set
	 */
	public void setEvents(List<Event> events) {
		this.events = events;
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
	 * @return the homeId
	 */
	public long getHomeId() {
		return this.homeId;
	}

	/**
	 * @param homeId
	 *            the homeId to set
	 */
	public void setHomeId(long homeId) {
		this.homeId = homeId;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the location
	 */
	public String getLocation() {
		return this.location;
	}

	/**
	 * @param location
	 *            the location to set
	 */
	public void setLocation(String location) {
		this.location = location;
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
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * @return the manufacturer
	 */
	public String getManufacturer() {
		return this.manufacturer;
	}

	/**
	 * @param manufacturer
	 *            the manufacturer to set
	 */
	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	/**
	 * @return the manufacturerId
	 */
	public String getManufacturerId() {
		return this.manufacturerId;
	}

	/**
	 * @param manufacturerId
	 *            the manufacturerId to set
	 */
	public void setManufacturerId(String manufacturerId) {
		this.manufacturerId = manufacturerId;
	}

	/**
	 * @return the product
	 */
	public String getProduct() {
		return this.product;
	}

	/**
	 * @param product
	 *            the product to set
	 */
	public void setProduct(String product) {
		this.product = product;
	}

	/**
	 * @return the values
	 */
	public List<Value> getValues() {
		return this.values;
	}

	/**
	 * @param values
	 *            the values to set
	 */
	public void setValues(List<Value> values) {
		this.values = values;
	}

	/**
	 * @return the associations
	 */
	public List<Association> getAssociations() {
		return this.associations;
	}

	/**
	 * @param associations
	 *            the associations to set
	 */
	public void setAssociations(List<Association> associations) {
		this.associations = associations;
	}

	/**
	 * @return the isAlive
	 */
	public boolean isAlive() {
		return this.isAlive;
	}

	/**
	 * @param isAlive
	 *            the isAlive to set
	 */
	public void setAlive(boolean isAlive) {
		this.isAlive = isAlive;
	}

	/**
	 * @return the neighbours
	 */
	public short[] getNeighbours() {
		return this.neighbours;
	}

	/**
	 * @param neighbours
	 *            the neighbours to set
	 */
	public void setNeighbours(short[] neighbours) {
		this.neighbours = neighbours;
	}

	public void addValue(Value value) {
		this.values.add(value);
	}

	public void addEvent(Event event) {
		this.events.add(event);
	}

	public void removeValue(Value value) {
		this.values.remove(value);
	}

	public void removeEvent(Event event) {
		this.events.remove(event);
	}

	public Value getValue(ValueId valueId) {
		return this.getValue(valueId.getType(), valueId.getGenre(), valueId.getInstance(), valueId.getIndex());
	}

	public Value getValue(BigInteger valueId, short instance) {
		for (Value value : this.values) {
			ValueId valueIdObj = value.getOriginalValueId();
			BigInteger currentValueId = ZWaveManager.getInstance().getValueId(valueIdObj);
			if (currentValueId.equals(valueId) && instance == valueIdObj.getInstance()) {
				return value;
			}
		}
		return null;
	}

	public Value getValue(ValueType type, ValueGenre genre, short instance, short index) {
		for (Value value : this.values) {
			ValueId valueIdObj = value.getOriginalValueId();
			if (valueIdObj.getType().equals(type) && valueIdObj.getType().equals(type) && valueIdObj.getGenre().equals(genre) && valueIdObj.getInstance() == instance && valueIdObj.getIndex() == index) {
				return value;
			}
		}
		return null;
	}

	public Event getEvent(BigInteger valueId) {
		for (Event event : this.events) {
			ValueId valueIdObj = event.getOriginalValueId();
			BigInteger currentValueId = ZWaveManager.getInstance().getValueId(valueIdObj);
			if (currentValueId.equals(valueId)) {
				return event;
			}
		}
		return null;
	}
	/*
	 * public ValueId getEvent(BigInteger valueId) { int i = 0; while (i <
	 * this.events.size()) { BigInteger currentValueId =
	 * ZWaveController.getInstance().getValueId(this.events.get(i)); if
	 * (currentValueId.equals(valueId)) { return this.events.get(i); } else {
	 * i++; } } return null; }
	 */

	/*
	 * public short id; public long homeId = 0; public String name; public
	 * String location; public String label; public String manufacturer; public
	 * String manufacturerId; public String product; public String productId;
	 * public String productType; public String controlId; private List<ValueId>
	 * values = new ArrayList<ValueId>(); private List<ValueId> events = new
	 * ArrayList<ValueId>();
	 * 
	 * public Node() { }
	 * 
	 * public void addValue(ValueId valueId) { values.add(valueId); }
	 * 
	 * public void addEvent(ValueId valueId) { events.add(valueId); }
	 * 
	 * public void removeValue(ValueId valueId) { values.remove(valueId); }
	 * 
	 * public void removeEvent(ValueId valueId) { events.remove(valueId); }
	 * 
	 * public List<ValueId> getValues() { return values; }
	 * 
	 * public List<ValueId> getEvents() { return events; }
	 * 
	 * public ValueId getValue(BigInteger valueId, short instance) { int i = 0;
	 * while (i < this.values.size()) { ValueId valueIdObj = this.values.get(i);
	 * BigInteger currentValueId =
	 * ZWaveController.getInstance().getValueId(valueIdObj); if
	 * (currentValueId.equals(valueId) && instance == valueIdObj.getInstance())
	 * { return this.values.get(i); } else { i++; } } return null; }
	 * 
	 * public ValueId getEvent(BigInteger valueId) { int i = 0; while (i <
	 * this.events.size()) { BigInteger currentValueId =
	 * ZWaveController.getInstance().getValueId(this.events.get(i)); if
	 * (currentValueId.equals(valueId)) { return this.events.get(i); } else {
	 * i++; } } return null; }
	 */

}
