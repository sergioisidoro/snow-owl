/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *///
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.5 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.03.01 at 10:42:55 AM CET 
//


package org.ihtsdo.sct;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * Element representing the author of the changes in a transaction.
 * 
 * <p>Java class for SifAuthor complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SifAuthor">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element name="person" type="{urn:ihtsdo-org/sct}SifPerson"/>
 *         &lt;element name="software" type="{urn:ihtsdo-org/sct}SifSoftware"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SifAuthor", propOrder = {
    "person",
    "software"
})
public class SifAuthor {

    protected SifPerson person;
    protected SifSoftware software;

    /**
     * Gets the value of the person property.
     * 
     * @return
     *     possible object is
     *     {@link SifPerson }
     *     
     */
    public SifPerson getPerson() {
        return person;
    }

    /**
     * Sets the value of the person property.
     * 
     * @param value
     *     allowed object is
     *     {@link SifPerson }
     *     
     */
    public void setPerson(SifPerson value) {
        this.person = value;
    }

    /**
     * Gets the value of the software property.
     * 
     * @return
     *     possible object is
     *     {@link SifSoftware }
     *     
     */
    public SifSoftware getSoftware() {
        return software;
    }

    /**
     * Sets the value of the software property.
     * 
     * @param value
     *     allowed object is
     *     {@link SifSoftware }
     *     
     */
    public void setSoftware(SifSoftware value) {
        this.software = value;
    }

}