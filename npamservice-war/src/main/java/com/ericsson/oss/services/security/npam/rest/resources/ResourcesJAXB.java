/*------------------------------------------------------------------------------
*******************************************************************************
* COPYRIGHT Ericsson 2022
*
* The copyright to the computer program(s) herein is the property of
* Ericsson Inc. The programs may be used and/or copied only with written
* permission from Ericsson Inc. or in accordance with the terms and
* conditions stipulated in the agreement/contract under which the
* program(s) have been supplied.
*******************************************************************************
*----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.rest.resources;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Set;

/**
 * @author DespicableUs
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Resources")
public class ResourcesJAXB {

    @XmlElement
    private Set<String> resources;

    /**
     * @param resources
     *            ENM resources
     */
    public ResourcesJAXB(final Set<String> resources) {
        this.resources = resources; //NOSONAR
    }

    /**
     *
     */
    public ResourcesJAXB() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @return the resources
     */
    public Set<String> getResources() {
        return resources; //NOSONAR
    }

    /**
     * @param resources the resources to set
     */
    public void setResources(final Set<String> resources) {
        this.resources = resources; //NOSONAR
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ResourcesJAXB [resources=" + resources + "]";
    }
}
