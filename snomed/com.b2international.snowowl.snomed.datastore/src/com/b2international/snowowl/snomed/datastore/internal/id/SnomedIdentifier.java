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
 */
package com.b2international.snowowl.snomed.datastore.internal.id;

import static com.google.common.base.Preconditions.checkArgument;

import com.b2international.commons.VerhoeffCheck;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifier;
import com.google.common.base.Strings;

/**
 * Representing a SNOMED CT Identifier in a Java POJO form, extracts parts of the SNOMED CT Identifier and stores them for later use.
 * 
 * @since 4.0
 */
public final class SnomedIdentifier implements ISnomedIdentifier {

	private String id;
	private long itemId;
	private String namespace;
	private int partitionIdentifier;
	private int componentIdentifier;
	private int checkDigit;

	public SnomedIdentifier(final long itemId, final String namespace, final int partitionIdentifier, final int componentIdentifier,
			final int checkDigit) {
		this.itemId = itemId;
		this.namespace = namespace;
		this.partitionIdentifier = partitionIdentifier;
		this.componentIdentifier = componentIdentifier;
		this.checkDigit = checkDigit;
	}
	
	/**
	 * Creates a {@link SnomedIdentifier} from the given {@link String} componentId.
	 * 
	 * @param componentId
	 * @return
	 */
	public static ISnomedIdentifier of(String componentId) {
		validate(componentId);
		final int checkDigit = Character.getNumericValue(componentId.charAt(componentId.length() - 1));
		final int componentIdentifier = Character.getNumericValue(componentId.charAt(componentId.length() - 2));
		final int partitionIdentifier = Character.getNumericValue(componentId.charAt(componentId.length() - 3));
		final String namespace = partitionIdentifier == 0 ? null : componentId.substring(componentId.length() - 10, componentId.length() - 3);
		final long itemId = partitionIdentifier == 0 ? Long.parseLong(componentId.substring(0, componentId.length() - 3)) : Long
				.parseLong(componentId.substring(0, componentId.length() - 10));
		return new SnomedIdentifier(itemId, namespace, partitionIdentifier, componentIdentifier, checkDigit);
	}
	
	/**
	 * Validates the given componentId by using the rules defined in the latest SNOMED CT Identifier specification, which are the following constraints:
	 * <ul>
	 * 	<li>Can't start with leading zeros</li>
	 * 	<li>Lengths should be between 6 and 18 characters</li>
	 * 	<li>Should parse to a long value</li>
	 * 	<li>Should pass the Verhoeff check-digit test</li>
	 * </ul>
	 * 
	 * @param componentId
	 * @see VerhoeffCheck
	 * @throws IllegalArgumentException - if the given componentId is invalid according to the SNOMED CT Identifier specification
	 */
	public static void validate(String componentId) throws IllegalArgumentException {
		checkArgument(!Strings.isNullOrEmpty(componentId), "ComponentId must be defined");
		checkArgument(!componentId.startsWith("0"), "ComponentId can't start with leading zeros");
		checkArgument(componentId.length() >= 6 && componentId.length() <= 18, "ComponentId's length should be between 6-18 character length");
		try {
			Long.parseLong(componentId);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("ComponentId should parse to a Long");
		}
		checkArgument(VerhoeffCheck.validateLastChecksumDigit(componentId), "ComponentId should pass Verhoeff check-digit test");
	}
	
	
	public long getItemId() {
		return itemId;
	}

	public String getNamespace() {
		return namespace;
	}

	public int getPartitionIdentifier() {
		return partitionIdentifier;
	}

	public int getComponentIdentifier() {
		return componentIdentifier;
	}

	public int getCheckDigit() {
		return checkDigit;
	}

	public ComponentCategory getComponentCategory() {
		return ComponentCategory.getByOrdinal(getComponentIdentifier());
	}
	
	@Override
	public String toString() {
		if (id == null) {
			id = String.format("%s%s%s%s%s", itemId, Strings.nullToEmpty(namespace), partitionIdentifier, componentIdentifier, checkDigit);
		}
		return id;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + checkDigit;
		result = prime * result + componentIdentifier;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + (int) (itemId ^ (itemId >>> 32));
		result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
		result = prime * result + partitionIdentifier;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SnomedIdentifier other = (SnomedIdentifier) obj;
		if (checkDigit != other.checkDigit)
			return false;
		if (componentIdentifier != other.componentIdentifier)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (itemId != other.itemId)
			return false;
		if (namespace == null) {
			if (other.namespace != null)
				return false;
		} else if (!namespace.equals(other.namespace))
			return false;
		if (partitionIdentifier != other.partitionIdentifier)
			return false;
		return true;
	}

}
