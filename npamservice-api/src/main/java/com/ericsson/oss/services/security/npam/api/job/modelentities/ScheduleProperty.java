/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.api.job.modelentities;

import java.io.Serializable;

public class ScheduleProperty implements Serializable {

	public static final String SCHEDULE_PROPERTY_NAME = "name";
	public static final String SCHEDULE_PROPERTY_VALUE = "value";

	private String name;
	private String value;
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}
	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "ScheduleProperty{" +
				attributeToString(SCHEDULE_PROPERTY_NAME, getName()) +
				attributeToString(SCHEDULE_PROPERTY_VALUE, getValue()) +
				'}';
	}

	protected String attributeToString(final String attributeName, final Object attributeValue) {
		return  new StringBuilder()
				.append(attributeName)
				.append("=")
				.append(attributeValue)
				.append(",")
				.toString();
	}
}
