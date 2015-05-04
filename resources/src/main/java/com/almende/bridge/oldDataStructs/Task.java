/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.oldDataStructs;

import java.io.Serializable;

import com.almende.util.jackson.JOM;

/**
 * The Class Task.
 */
public class Task implements Serializable {
	private static final long	serialVersionUID	= 6783092535568614883L;
	
	/** The Constant NOTCONFIRMED. */
	public static final String	NOTCONFIRMED		= "not confirmed";
	
	/** The Constant CONFIRMED. */
	public static final String	CONFIRMED			= "confirmed";
	
	/** The Constant COMPLETE. */
	public static final String	COMPLETE			= "completed";
	
	/** The Constant POSTPONED. */
	public static final String	POSTPONED			= "postponed";
	
	private String				title;
	private String				text;
	private String				assigner;
	private String				assignmentDate;
	private String				status;
	private String				lat;
	private String				lon;
	
	private String				messageId;
	private String				incidentDescription;
	
	/**
	 * Instantiates a new task.
	 */
	public Task() {
	}
	
	/**
	 * Instantiates a new task.
	 *
	 * @param text
	 *            the text
	 * @param assigner
	 *            the assigner
	 * @param assignmentDate
	 *            the assignment date
	 * @param status
	 *            the status
	 * @param lat
	 *            the lat
	 * @param lon
	 *            the lon
	 */
	public Task(String text, String assigner, String assignmentDate,
			String status, String lat, String lon) {
		this.text = text;
		this.assigner = assigner;
		this.assignmentDate = assignmentDate;
		this.status = status;
		this.lat = lat;
		this.lon = lon;
	}
	
	/**
	 * Gets the title.
	 *
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 *
	 * @param title
	 *            the new title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gets the text.
	 *
	 * @return the text
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * Sets the text.
	 *
	 * @param text
	 *            the new text
	 */
	public void setText(String text) {
		this.text = text;
	}
	
	/**
	 * Gets the assigner.
	 *
	 * @return the assigner
	 */
	public String getAssigner() {
		return assigner;
	}
	
	/**
	 * Sets the assigner.
	 *
	 * @param assigner
	 *            the new assigner
	 */
	public void setAssigner(String assigner) {
		this.assigner = assigner;
	}
	
	/**
	 * Gets the assignment date.
	 *
	 * @return the assignment date
	 */
	public String getAssignmentDate() {
		return assignmentDate;
	}
	
	/**
	 * Sets the assignment date.
	 *
	 * @param assignmentDate
	 *            the new assignment date
	 */
	public void setAssignmentDate(String assignmentDate) {
		this.assignmentDate = assignmentDate;
	}
	
	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}
	
	/**
	 * Sets the status.
	 *
	 * @param status
	 *            the new status
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	
	/**
	 * Gets the lat.
	 *
	 * @return the lat
	 */
	public String getLat() {
		return lat;
	}
	
	/**
	 * Sets the lat.
	 *
	 * @param lat
	 *            the new lat
	 */
	public void setLat(String lat) {
		this.lat = lat;
	}
	
	/**
	 * Gets the lon.
	 *
	 * @return the lon
	 */
	public String getLon() {
		return lon;
	}
	
	/**
	 * Sets the lon.
	 *
	 * @param lon
	 *            the new lon
	 */
	public void setLon(String lon) {
		this.lon = lon;
	}
	
	/**
	 * Gets the message id.
	 *
	 * @return the message id
	 */
	public String getMessageId() {
		return messageId;
	}
	
	/**
	 * Sets the message id.
	 *
	 * @param messageId
	 *            the new message id
	 */
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	
	/**
	 * Gets the incident description.
	 *
	 * @return the incident description
	 */
	public String getIncidentDescription() {
		return incidentDescription;
	}

	/**
	 * Sets the incident description.
	 *
	 * @param incidentDescription
	 *            the new incident description
	 */
	public void setIncidentDescription(String incidentDescription) {
		this.incidentDescription = incidentDescription;
	}

	/**
	 * Compare field.
	 *
	 * @param left
	 *            the left
	 * @param right
	 *            the right
	 * @return true, if successful
	 */
	public boolean compareField(String left, String right) {
		boolean result = false;
		if (left == null && right == null) result = true;
		if (left != null && left.equals(right)) result = true;
		return result;
	}
	
	/**
	 * Eq.
	 *
	 * @param other
	 *            the other
	 * @return true, if successful
	 */
	public boolean eq(Task other) {
		return (compareField(text, other.text)
				&& compareField(title, other.title)
				&& compareField(assigner, other.assigner)
				&& compareField(assignmentDate, other.assignmentDate)
				&& compareField(lat, other.lat) && compareField(lon, other.lon));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		try {
			return JOM.getInstance().writeValueAsString(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return super.toString();
	}
}