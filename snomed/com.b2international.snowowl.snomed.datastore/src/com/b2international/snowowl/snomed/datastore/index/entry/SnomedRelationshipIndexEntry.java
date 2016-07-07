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
package com.b2international.snowowl.snomed.datastore.index.entry;

import static com.b2international.index.query.Expressions.match;
import static com.b2international.index.query.Expressions.matchAny;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;

import com.b2international.index.Doc;
import com.b2international.index.query.Expression;
import com.b2international.snowowl.core.api.IStatement;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.datastore.cdo.CDOIDUtils;
import com.b2international.snowowl.snomed.Relationship;
import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.common.SnomedRf2Headers;
import com.b2international.snowowl.snomed.core.domain.CharacteristicType;
import com.b2international.snowowl.snomed.core.domain.ISnomedRelationship;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

/**
 * A transfer object representing a SNOMED CT description.
 */
@Doc
@JsonDeserialize(builder = SnomedRelationshipIndexEntry.Builder.class)
public final class SnomedRelationshipIndexEntry extends SnomedDocument implements IStatement<String> {

	private static final long serialVersionUID = -7873086925532169024L;

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builder(final ISnomedRelationship input) {
		final Builder builder = builder()
				.storageKey(input.getStorageKey())
				.id(input.getId())
				.sourceId(input.getSourceId())
				.typeId(input.getTypeId())
				.destinationId(input.getDestinationId())
				.characteristicTypeId(input.getCharacteristicType().getConceptId())
				.group(input.getGroup())
				.unionGroup(input.getUnionGroup())
				.active(input.isActive())
				.released(input.isReleased())
				.modifierId(input.getModifier().getConceptId())
				.destinationNegated(input.isDestinationNegated())
				.moduleId(input.getModuleId())
				.effectiveTime(EffectiveTimes.getEffectiveTime(input.getEffectiveTime()));
		
//		if (input.getScore() != null) {
//			builder.score(input.getScore());
//		}
		
		return builder;
	}
	
	public static Builder builder(Relationship relationship) {
		return builder()
				.storageKey(CDOIDUtils.asLong(relationship.cdoID()))
				.id(relationship.getId())
				.active(relationship.isActive())
				.sourceId(relationship.getSource().getId())
				.typeId(relationship.getType().getId())
				.destinationId(relationship.getDestination().getId())
				.characteristicTypeId(relationship.getCharacteristicType().getId())
				.group(relationship.getGroup())
				.unionGroup(relationship.getUnionGroup())
				.released(relationship.isReleased())
				.modifierId(relationship.getModifier().getId())
				.destinationNegated(relationship.isDestinationNegated())
				.moduleId(relationship.getModule().getId())
				.effectiveTime(relationship.isSetEffectiveTime() ? relationship.getEffectiveTime().getTime() : EffectiveTimes.UNSET_EFFECTIVE_TIME);
	}
	
	public static Collection<SnomedRelationshipIndexEntry> fromRelationships(Iterable<ISnomedRelationship> relationships) {
		return FluentIterable.from(relationships).transform(new Function<ISnomedRelationship, SnomedRelationshipIndexEntry>() {
			@Override
			public SnomedRelationshipIndexEntry apply(ISnomedRelationship input) {
				return builder(input).build();
			}
		}).toSet();
	}
	
	public static final class Expressions extends SnomedDocument.Expressions {
		
		private Expressions() {}
		
		public static Expression sourceId(String sourceId) {
			return sourceIds(Collections.singleton(sourceId));
		}

		public static Expression sourceIds(Collection<String> sourceIds) {
			return matchAny(Fields.SOURCE_ID, sourceIds);
		}
		
		public static Expression typeId(String typeId) {
			return typeIds(Collections.singleton(typeId));
		}

		public static Expression typeIds(Collection<String> typeIds) {
			return matchAny(Fields.TYPE_ID, typeIds);
		}
		
		public static Expression destinationId(String destinationId) {
			return destinationIds(Collections.singleton(destinationId));
		}

		public static Expression destinationIds(Collection<String> destinationIds) {
			return matchAny(Fields.DESTINATION_ID, destinationIds);
		}
		
		public static Expression characteristicTypeId(String characteristicTypeId) {
			return characteristicTypeIds(Collections.singleton(characteristicTypeId));
		}

		public static Expression characteristicTypeIds(Collection<String> characteristicTypeIds) {
			return matchAny(Fields.CHARACTERISTIC_TYPE_ID, characteristicTypeIds);
		}
		
		public static Expression modifierId(String modifierId) {
			return modifierIds(Collections.singleton(modifierId));
		}

		public static Expression modifierIds(Collection<String> modifierIds) {
			return matchAny(Fields.MODIFIER_ID, modifierIds);
		}
		
		public static Expression group(int group) {
			return match(Fields.GROUP, group);
		}
		
		public static Expression unionGroup(int unionGroup) {
			return match(Fields.UNION_GROUP, unionGroup);
		}
		
		public static Expression destinationNegated() {
			return match(Fields.DESTINATION_NEGATED, true);
		}
		
	}
	
	public static final class Fields extends SnomedDocument.Fields {
		public static final String SOURCE_ID = SnomedRf2Headers.FIELD_SOURCE_ID;
		public static final String TYPE_ID = SnomedRf2Headers.FIELD_TYPE_ID;
		public static final String DESTINATION_ID = SnomedRf2Headers.FIELD_DESTINATION_ID;
		public static final String CHARACTERISTIC_TYPE_ID = SnomedRf2Headers.FIELD_CHARACTERISTIC_TYPE_ID;
		public static final String MODIFIER_ID = SnomedRf2Headers.FIELD_MODIFIER_ID;
		public static final String GROUP = "group"; // XXX different than RF2 header
		public static final String UNION_GROUP = "unionGroup";
		public static final String DESTINATION_NEGATED = "destinationNegated";
	}

	@JsonPOJOBuilder(withPrefix="")
	public static class Builder extends SnomedDocumentBuilder<Builder> {

		private String sourceId;
		private String typeId;
		private String destinationId;
		private String characteristicTypeId;
		private String modifierId;

		private int group;
		private int unionGroup;

		private boolean destinationNegated;
		
		@JsonCreator
		private Builder() {
			// Disallow instantiation outside static method
		}
		
		@Override
		protected Builder getSelf() {
			return this;
		}

		public Builder sourceId(final String sourceId) {
			this.sourceId = sourceId;
			return getSelf();
		}

		public Builder typeId(final String typeId) {
			this.typeId = typeId;
			return getSelf();
		}

		public Builder destinationId(final String destinationId) {
			this.destinationId = destinationId;
			return getSelf();
		}

		public Builder characteristicTypeId(final String characteristicTypeId) {
			this.characteristicTypeId = characteristicTypeId;
			return getSelf();
		}

		public Builder modifierId(final String modifierId) {
			this.modifierId = modifierId;
			return getSelf();
		}

		public Builder group(final int group) {
			this.group = group;
			return getSelf();
		}

		public Builder unionGroup(final int unionGroup) {
			this.unionGroup = unionGroup;
			return getSelf();
		}

		public Builder destinationNegated(final boolean destinationNegated) {
			this.destinationNegated = destinationNegated;
			return getSelf();
		}

		public SnomedRelationshipIndexEntry build() {
			final SnomedRelationshipIndexEntry doc = new SnomedRelationshipIndexEntry(id,
					label,
					moduleId, 
					released, 
					active, 
					effectiveTime, 
					sourceId, 
					typeId, 
					destinationId, 
					characteristicTypeId, 
					modifierId, 
					group, 
					unionGroup, 
					destinationNegated);
			doc.setBranchPath(branchPath);
			doc.setCommitTimestamp(commitTimestamp);
			doc.setStorageKey(storageKey);
			doc.setReplacedIns(replacedIns);
			doc.setSegmentId(segmentId);
			return doc;
		}
	}

	private final String sourceId;
	private final String typeId;
	private final String destinationId;
	private final String characteristicTypeId;
	private final String modifierId;
	private final int group;
	private final int unionGroup;
	private final boolean destinationNegated;

	private SnomedRelationshipIndexEntry(final String id, 
			final String label,
			final String moduleId, 
			final boolean released,
			final boolean active, 
			final long effectiveTimeLong,
			final String sourceId,
			final String typeId,
			final String destinationId,
			final String characteristicTypeId,
			final String modifierId,
			final int group,
			final int unionGroup,
			final boolean destinationNegated) {

		super(id, 
				label,
				typeId, // XXX: iconId is the same as typeId 
				moduleId, 
				released, 
				active, 
				effectiveTimeLong);

		checkArgument(group >= 0, String.format("Group number '%s' may not be negative (relationship ID: %s).", group, id));
		checkArgument(unionGroup >= 0, String.format("Union group number '%s' may not be negative (relationship ID: %s).", unionGroup, id));

		this.sourceId = checkNotNull(sourceId, "Relationship source identifier may not be null.");
		this.typeId = checkNotNull(typeId, "Relationship type identifier may not be null.");
		this.destinationId = checkNotNull(destinationId, "Relationship destination identifier may not be null.");
		this.characteristicTypeId = checkNotNull(characteristicTypeId, "Relationship characteristic type identifier may not be null.");
		this.modifierId = checkNotNull(modifierId, "Relationship modifier identifier may not be null.");
		this.group = group;
		this.unionGroup = unionGroup;
		this.destinationNegated = destinationNegated;
	}

	@Override
	public String getContainerId() {
		return getSourceId();
	}
	
	@Override
	@JsonIgnore
	public String getIconId() {
		return super.getIconId();
	}
	
	@Override
	public String getSourceId() {
		return sourceId;
	}

	@Override
	public String getTypeId() {
		return typeId;
	}

	@Override
	public String getDestinationId() {
		return destinationId;
	}

	/**
	 * @return the characteristic type identifier of this relationship
	 */
	public String getCharacteristicTypeId() {
		return characteristicTypeId;
	}
	
	/**
	 * @return {@code true} if the characteristic type id is equal to {@link Concepts#DEFINING_RELATIONSHIP}, {@code false} otherwise
	 */
	@JsonIgnore
	public boolean isDefining() {
		return Concepts.DEFINING_RELATIONSHIP.equals(characteristicTypeId);
	}

	/**
	 * @return {@code true} if the characteristic type id is equal to {@link Concepts#INFERRED_RELATIONSHIP}, {@code false} otherwise
	 */
	@JsonIgnore
	public boolean isInferred() {
		return Concepts.INFERRED_RELATIONSHIP.equals(characteristicTypeId);
	}
	
	/**
	 * @return {@code true} if the characteristic type id is equal to {@link Concepts#STATED_RELATIONSHIP}, {@code false} otherwise
	 */
	@JsonIgnore
	public boolean isStated() {
		return Concepts.STATED_RELATIONSHIP.equals(characteristicTypeId);
	}

	/**
	 * @return {@code true} if the characteristic type id is equal to {@link Concepts#ADDITIONAL_RELATIONSHIP}, {@code false} otherwise
	 */
	@JsonIgnore
	public boolean isAdditional() {
		return Concepts.ADDITIONAL_RELATIONSHIP.equals(characteristicTypeId);
	}

	/**
	 * @return the modifier identifier of this relationship
	 */
	public String getModifierId() {
		return modifierId;
	}

	/**
	 * @return {@code true} if the modifier id is equal to {@link Concepts#UNIVERSAL_RESTRICTION_MODIFIER}, {@code false} otherwise
	 */
	@JsonIgnore
	public boolean isUniversal() {
		return Concepts.UNIVERSAL_RESTRICTION_MODIFIER.equals(modifierId);
	}
	
	/**
	 * @return {@code true} if the modifier id is equal to {@link Concepts#EXISTENTIAL_RESTRICTION_MODIFIER}, {@code false} otherwise
	 */
	@JsonIgnore
	public boolean isExistential() {
		return Concepts.EXISTENTIAL_RESTRICTION_MODIFIER.equals(modifierId);
	}
	
	/**
	 * @return the {@link CharacteristicType} value for this relationship, based on the stored characteristic type identifier
	 */
	@JsonIgnore
	public CharacteristicType getCharacteristicType() {
		return CharacteristicType.getByConceptId(characteristicTypeId);
	}

	/**
	 * @return the relationship group
	 */
	public int getGroup() {
		return group;
	}

	/**
	 * @return the relationship union group
	 */
	public int getUnionGroup() {
		return unionGroup;
	}

	/**
	 * @return {@code true} if the destination concept should be negated, {@code false} otherwise
	 */
	public boolean isDestinationNegated() {
		return destinationNegated;
	}

	@Override
	public String toString() {
		return toStringHelper()
				.add("sourceId", sourceId)
				.add("typeId", getTypeId())
				.add("destinationId", destinationId)
				.add("characteristicTypeId", characteristicTypeId)
				.add("modifierId", modifierId)
				.add("group", group)
				.add("unionGroup", unionGroup)
				.add("destinationNegated", destinationNegated)
				.toString();
	}

}