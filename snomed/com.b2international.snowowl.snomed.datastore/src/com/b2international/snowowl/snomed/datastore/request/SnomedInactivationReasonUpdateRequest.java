/*
 * Copyright 2011-2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.datastore.request;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.common.SnomedRf2Headers;
import com.b2international.snowowl.snomed.core.domain.refset.SnomedReferenceSetMember;
import com.b2international.snowowl.snomed.core.store.SnomedComponents;
import com.b2international.snowowl.snomed.datastore.SnomedDatastoreActivator;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedRefSetMemberIndexEntry;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

/**
 * Updates the inactivation reason on the {@link Inactivatable} component specified by identifier.
 * <p>
 * Existing members are <b>removed</b> when:
 * <ul>
 * <li>{@link #inactivationValueId} is set to an empty string, signalling that any existing inactivation reasons 
 * should be removed ("no reason given");
 * <li>The member is unreleased.
 * </ul>
 * <p>
 * Existing members are <b>inactivated</b> when:
 * <ul>
 * <li>{@link #inactivationValueId} is set to an empty string, signalling that any existing inactivation reasons 
 * should be removed ("no reason given");
 * <li>The member is already part of a release.
 * </ul>
 * <p>
 * The first existing member is <b>updated</b> with a new value identifier when:
 * <ul>
 * <li>{@link #inactivationValueId} is set to a non-empty string, and the value ID presented here does not match 
 * the member's currently set value ID.
 * </ul>
 * <p>
 * New members are <b>created</b> when:
 * <ul>
 * <li>No previous inactivation reason member exists;
 * <li>{@link #inactivationValueId} is set to a non-empty string.
 * </ul>
 * <p>
 * Multiple inactivation reason reference set members are always reduced to a single item; unused existing members 
 * will be removed or deactivated, depending on whether they were already released.
 * <p>
 * Whenever an existing released member is modified, it is compared to its most recently versioned representation, 
 * and its effective time is restored to the original value if the final state matches the most recently versioned 
 * state.
 * 
 * @since 4.5
 */
final class SnomedInactivationReasonUpdateRequest implements Request<TransactionContext, Void> {

	private static final Logger LOG = LoggerFactory.getLogger(SnomedInactivationReasonUpdateRequest.class);

	private static final String CLEAR = "";

	private final String referencedComponentId;
	private final String inactivationRefSetId;
	private final String moduleId;

	private final Function<TransactionContext, String> referenceBranchFunction = CacheBuilder.newBuilder().build(new CacheLoader<TransactionContext, String>() {
		@Override
		public String load(final TransactionContext context) throws Exception {
			final String latestReleaseBranch = SnomedComponentUpdateRequest.getLatestReleaseBranch(context);
			if (latestReleaseBranch == null) {
				return Branch.MAIN_PATH;
			}
			return latestReleaseBranch;
		}
	});

	private String inactivationValueId;
	
	SnomedInactivationReasonUpdateRequest(final String referencedComponentId, final String inactivationRefSetId, final String moduleId) {
		this.referencedComponentId = referencedComponentId;
		this.inactivationRefSetId = inactivationRefSetId;
		this.moduleId = moduleId;
	}

	void setInactivationValueId(final String inactivationValueId) {
		this.inactivationValueId = inactivationValueId;
	}

	@Override
	public Void execute(final TransactionContext context) {
		// Null leaves inactivation reason unchanged, empty string clears existing inactivation reason
		if (null == inactivationValueId) {
			return null;
		} else {
			updateInactivationReason(context);
			return null;
		}
	}

	private void updateInactivationReason(final TransactionContext context) {
		final List<SnomedReferenceSetMember> existingMembers = newArrayList(
			SnomedRequests.prepareSearchMember()
				.all()
				.filterByReferencedComponent(referencedComponentId)
				.filterByRefSet(inactivationRefSetId)
				.build()
				.execute(context)
				.getItems()
		);
		boolean firstMemberFound = false;
		
		// Check if there is at least one existing member
		for (SnomedReferenceSetMember existingMember : existingMembers) {
			
			final SnomedRefSetMemberIndexEntry.Builder updatedMember = SnomedRefSetMemberIndexEntry.builder(existingMember);
			final SnomedRefSetMemberIndexEntry oldRevision = updatedMember.build();
			
			if (firstMemberFound) {
				// If we got through the first iteration, all other members can be removed
				if (removeOrDeactivate(context, existingMember, updatedMember)) {
					context.update(oldRevision, updatedMember.build());
				}
				continue;
			}
			
			final String existingValueId = (String) existingMember.getProperties().get(SnomedRf2Headers.FIELD_VALUE_ID);
			if (Objects.equals(existingValueId, inactivationValueId)) {

				// Exact match, just make sure that the member is active
				if (ensureMemberActive(context, existingMember, updatedMember)) {
					context.update(oldRevision, updatedMember.build());
				}
				firstMemberFound = true;

			} else if (!CLEAR.equals(inactivationValueId)) {

				// Re-use, if the intention was not to remove the existing value
				if (LOG.isDebugEnabled()) { 
					LOG.debug("Changing attribute-value member {} with value identifier from {} to {}.", 
							existingMember.getId(), 
							existingValueId, 
							inactivationValueId);
				}

				updatedMember.field(SnomedRf2Headers.FIELD_VALUE_ID, inactivationValueId);
				ensureMemberActive(context, existingMember, updatedMember);
				context.update(oldRevision, updatedMember.build());
				
			} else /* if (CLEAR.equals(inactivationValueId) */ {
				
				// Inactivation value is "no reason given", remove this member
				if (removeOrDeactivate(context, existingMember, updatedMember)) {
					context.update(oldRevision, updatedMember.build());
				}
			}

			// If we get to the end of this loop, the first member has been processed
			firstMemberFound = true;
		}

		// Add the new member if the intention was not to remove the existing value (which had already happened if so)
		if (!firstMemberFound && !CLEAR.equals(inactivationValueId)) {
			SnomedComponents.newAttributeValueMember()
				.withReferencedComponent(referencedComponentId)
				.withRefSet(inactivationRefSetId)
				.withModule(moduleId)
				.withValueId(inactivationValueId)
				.addTo(context);
		}
	}

	private String getLatestReleaseBranch(final TransactionContext context) {
		final String latestVersion = referenceBranchFunction.apply(context);
		return  latestVersion == Branch.MAIN_PATH ? null : latestVersion;
	}

	private boolean ensureMemberActive(final TransactionContext context, final SnomedReferenceSetMember existingMember, final SnomedRefSetMemberIndexEntry.Builder updatedMember) {

		if (!existingMember.isActive()) {

			if (LOG.isDebugEnabled()) { LOG.debug("Reactivating attribute-value member {}.", existingMember.getId()); }
			existingMember.setActive(true);
			updateEffectiveTime(context, getLatestReleaseBranch(context), existingMember, updatedMember);
			return true;
			
		} else {
			if (LOG.isDebugEnabled()) { LOG.debug("Attribute-value member {} already active, not updating.", existingMember.getId()); }
			return false;
		}
	}

	private boolean removeOrDeactivate(final TransactionContext context, final SnomedReferenceSetMember existingMember, final SnomedRefSetMemberIndexEntry.Builder updatedMember) {
		if (!existingMember.isReleased()) {

			if (LOG.isDebugEnabled()) { LOG.debug("Removing attribute-value member {}.", existingMember.getId()); }
			context.delete(updatedMember.build());
			
			return false;

		} else if (existingMember.isActive()) {

			if (LOG.isDebugEnabled()) { LOG.debug("Inactivating attribute-value member {}.", existingMember.getId()); }
			existingMember.setActive(false);
			updateEffectiveTime(context, getLatestReleaseBranch(context), existingMember, updatedMember);
			return true;
			
		} else {
			
			if (LOG.isDebugEnabled()) { LOG.debug("Attribute-value member {} already inactive, not updating.", existingMember.getId()); }
			return false;
			
		}
	}

	private boolean updateEffectiveTime(final TransactionContext context, final String referenceBranch, final SnomedReferenceSetMember existingMember, final SnomedRefSetMemberIndexEntry.Builder updatedMember) {

		if (existingMember.isReleased() &&  !Strings.isNullOrEmpty(referenceBranch)) {

			final SnomedReferenceSetMember referenceMember = SnomedRequests.prepareGetMember(existingMember.getId())
					.build(SnomedDatastoreActivator.REPOSITORY_UUID, referenceBranch)
					.execute(context.service(IEventBus.class))
					.getSync();

			final String referenceValueId = (String) referenceMember.getProperties().get(SnomedRf2Headers.FIELD_VALUE_ID);

			final SnomedRefSetMemberIndexEntry memberToCheck = updatedMember.build();
			
			boolean restoreEffectiveTime = true;
			restoreEffectiveTime = restoreEffectiveTime && memberToCheck.isActive() == referenceMember.isActive();
			restoreEffectiveTime = restoreEffectiveTime && memberToCheck.getModuleId().equals(referenceMember.getModuleId());
			restoreEffectiveTime = restoreEffectiveTime && memberToCheck.getValueId().equals(referenceValueId);

			if (restoreEffectiveTime) {

				if (LOG.isDebugEnabled()) { 
					LOG.debug("Restoring effective time on attribute-value member {} to reference value {}.", 
							existingMember.getId(), 
							EffectiveTimes.format(referenceMember.getEffectiveTime(), DateFormats.SHORT));
				}

				existingMember.setEffectiveTime(referenceMember.getEffectiveTime());
				return true;
			} else {
				return unsetEffectiveTime(existingMember, updatedMember);
			}

		} else {
			return unsetEffectiveTime(existingMember, updatedMember);
		}
	}

	private boolean unsetEffectiveTime(final SnomedReferenceSetMember existingMember, final SnomedRefSetMemberIndexEntry.Builder updatedMember) {

		if (existingMember.getEffectiveTime() != null) {
			if (LOG.isDebugEnabled()) { LOG.debug("Unsetting effective time on attribute-value member {}.", existingMember.getId()); }
			updatedMember.effectiveTime(EffectiveTimes.UNSET_EFFECTIVE_TIME);
			return true;
		} else {
			if (LOG.isDebugEnabled()) { LOG.debug("Effective time on attribute-value member {} already unset, not updating.", existingMember.getId()); }
			return false;
		}
	}
}
