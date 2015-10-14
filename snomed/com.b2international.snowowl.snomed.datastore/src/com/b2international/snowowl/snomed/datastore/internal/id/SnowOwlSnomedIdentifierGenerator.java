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
import static com.google.common.base.Preconditions.checkNotNull;

import com.b2international.commons.VerhoeffCheck;
import com.b2international.snowowl.core.terminology.ComponentCategory;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifier;
import com.b2international.snowowl.snomed.datastore.id.ISnomedIdentifierGenerator;
import com.b2international.snowowl.snomed.datastore.id.gen.ItemIdGenerationStrategy;
import com.b2international.snowowl.snomed.datastore.id.gen.SingleItemIdGenerationStrategy;
import com.b2international.snowowl.snomed.datastore.id.reservations.ISnomedIdentiferReservationService;
import com.b2international.snowowl.snomed.datastore.internal.id.reservations.SnomedIdentifierReservationServiceImpl;
import com.google.common.base.Strings;

/**
 * Snow Owl's internal identifier generator.
 * SNOMED CT Identifiers v0.4:
 * <p />
 * <i>An item identifier can have a lowest permissible value of 100 (three digits) and a highest permissible value of 99999999 (8 digits) for short
 * format identifiers or 999999999999999 (15 digits) for long format identifiers. Leading zeros are not permitted in the item identifier.<//>
 * 
 * @since 4.0
 */
public class SnowOwlSnomedIdentifierGenerator implements ISnomedIdentifierGenerator {

	private ItemIdGenerationStrategy itemIdGenerationStrategy;
	private ISnomedIdentiferReservationService reservationService;

	/**
	 * Creates a new Snomed CT id generator with no reservation service 
	 * or generation strategy.  
	 */
	public SnowOwlSnomedIdentifierGenerator() {
		this(new SnomedIdentifierReservationServiceImpl(), ItemIdGenerationStrategy.RANDOM);
	}
	
	/**
	 * Creates a Snomed CT id generator for a reservation service and random id generation 
	 * strategy.
	 * @param reservationService
	 */
	public SnowOwlSnomedIdentifierGenerator(final ISnomedIdentiferReservationService reservationService) {
		this(reservationService, ItemIdGenerationStrategy.RANDOM);
	}
	
	/**
	 * Creates a Snomed CT id generator for a reservation service and strategy specified.
	 * @param reservationService
	 * @param itemIdGenerationStrategy id generation strategy
	 */
	public SnowOwlSnomedIdentifierGenerator(final ISnomedIdentiferReservationService reservationService, ItemIdGenerationStrategy itemIdGenerationStrategy) {
		this.setReservationService(checkNotNull(reservationService, "reservationService"));
		this.setItemIdGenerationStrategy(checkNotNull(itemIdGenerationStrategy, "itemIdGenerationStrategy"));
	}
	
	/**
	 * Generates a valid SNOMED CT Identifier from the given spec, which should be sufficient for a SNOMED CT Identifier.
	 * 
	 * @param itemId
	 *            - the itemId to use for the newly created SNOMED CT Identifier
	 * @param component
	 *            - the component type to use
	 * @return
	 */
	public static ISnomedIdentifier generateFrom(int itemId, ComponentCategory component) {
		return generateFrom(itemId, null, component);
	}

	/**
	 * Generates a valid SNOMED CT Identifier from the given spec, which should be sufficient for a SNOMED CT Identifier.
	 * 
	 * @param itemId
	 *            - the itemId to use for the newly created SNOMED CT Identifier
	 * @param namespace
	 *            - the namespace to use
	 * @param component
	 *            - the component type to use
	 * @return
	 */
	public static ISnomedIdentifier generateFrom(int itemId, String namespace, ComponentCategory component) {
		return SnomedIdentifier.of(new SnowOwlSnomedIdentifierGenerator(new SnomedIdentifierReservationServiceImpl(), new SingleItemIdGenerationStrategy(String.valueOf(itemId))).generateId(component, namespace));
	}
	
	public static String generateConceptId() {
		return generateConceptId(null);
	}

	public static String generateConceptId(String namespace) {
		return generateComponentIdViaSnowOwlIdGenerator(ComponentCategory.CONCEPT, namespace);
	}

	public static String generateRelationshipId() {
		return generateRelationshipId(null);
	}

	public static String generateRelationshipId(String namespace) {
		return generateComponentIdViaSnowOwlIdGenerator(ComponentCategory.RELATIONSHIP, namespace);
	}

	public static String generateDescriptionId() {
		return generateRelationshipId(null);
	}

	public static String generateDescriptionId(String namespace) {
		return generateComponentIdViaSnowOwlIdGenerator(ComponentCategory.DESCRIPTION, namespace);
	}

	private static String generateComponentIdViaSnowOwlIdGenerator(ComponentCategory component, String namespace) {
		return getSnomedIdentifierGenerator().generateId(component, namespace);
	}

	private static ISnomedIdentifierGenerator getSnomedIdentifierGenerator() {
		return new SnowOwlSnomedIdentifierGenerator(new SnomedIdentifierReservationServiceImpl());
	}

	@Override
	public String generateId(ComponentCategory component) {
		return generateId(component, null);
	}

	@Override
	public String generateId(ComponentCategory component, String namespace) {
		checkNotNull(component, "componentNature");
		checkCategory(component);
		String newId = generateComponentId(component, namespace);
		while (getReservationService().isReserved(newId)) {
			newId = generateComponentId(component, namespace);
		}
		return newId;
	}

	private void checkCategory(ComponentCategory component) {
		checkArgument(component == ComponentCategory.CONCEPT || component == ComponentCategory.DESCRIPTION || component == ComponentCategory.RELATIONSHIP, "Cannot generate ID for componentCategory %s", component);
	}

	private String generateComponentId(ComponentCategory component, String namespace) {
		final StringBuilder buf = new StringBuilder();
		// generate the SCT Item ID
		buf.append(getItemIdGenerationStrategy().generateItemId());
		// append namespace and the first part of the partition-identifier
		if (Strings.isNullOrEmpty(namespace)) {
			buf.append('0');
		} else {
			buf.append(namespace);
			buf.append('1');
		}
		// append the second part of the partition-identifier
		buf.append(component.ordinal());
		// calc check-digit
		buf.append(VerhoeffCheck.calculateChecksum(buf, false));
		return buf.toString();
	}

	/**
	 * Returns the id generation strategy of this generator.
	 * @return
	 */
	public ItemIdGenerationStrategy getItemIdGenerationStrategy() {
		return itemIdGenerationStrategy;
	}

	/**
	 * Sets the id generation strategy for this generator.
	 * @param itemIdGenerationStrategy
	 */
	public void setItemIdGenerationStrategy(ItemIdGenerationStrategy itemIdGenerationStrategy) {
		this.itemIdGenerationStrategy = itemIdGenerationStrategy;
	}

	/**
	 * Returns the reservation service of this generator.
	 * @return
	 */
	public ISnomedIdentiferReservationService getReservationService() {
		return reservationService;
	}

	/**
	 * Sets the reservation service for this generator.
	 * @param reservationService
	 */
	public void setReservationService(ISnomedIdentiferReservationService reservationService) {
		this.reservationService = reservationService;
	}
	
}
