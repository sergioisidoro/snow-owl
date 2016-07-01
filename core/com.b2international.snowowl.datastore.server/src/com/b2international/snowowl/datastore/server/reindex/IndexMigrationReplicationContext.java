/*
 * Copyright 2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.datastore.server.reindex;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchManager;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.lock.IDurableLockingManager.LockArea;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.internal.server.DelegatingRepository;
import org.eclipse.emf.cdo.server.StoreThreadLocal;
import org.eclipse.emf.cdo.spi.common.CDOReplicationContext;
import org.eclipse.emf.cdo.spi.common.revision.DelegatingCDORevisionManager;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionManager;
import org.eclipse.emf.cdo.spi.server.InternalRepository;
import org.eclipse.emf.cdo.spi.server.InternalSession;
import org.eclipse.emf.cdo.spi.server.InternalTransaction;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.net4j.util.om.monitor.Monitor;

import com.b2international.snowowl.core.domain.RepositoryContext;
import com.b2international.snowowl.datastore.replicate.BranchReplicator;
import com.b2international.snowowl.datastore.server.DelegatingTransaction;

/**
 * Replication context the delegates the actual work to the same repository the replications is reading from. 
 * During the replication, the change processors responsible for writing Lucene index documents 
 * are triggered but no actual records are written into the repository via the replaced CommitContext
 */
@SuppressWarnings("restriction")
class IndexMigrationReplicationContext implements CDOReplicationContext {

	private final RepositoryContext context;
	private final long initialLastCommitTime;
	private final int initialBranchId;
	private final InternalSession replicatorSession;
	
	private TreeMap<Long, CDOBranch> branchesByBasetimestamp = new TreeMap<>();

	IndexMigrationReplicationContext(final RepositoryContext context, final int initialBranchId, final long initialLastCommitTime, final InternalSession session) {
		this.context = context;
		this.initialBranchId = initialBranchId;
		this.initialLastCommitTime = initialLastCommitTime;
		this.replicatorSession = session;
	}

	@Override
	public void handleCommitInfo(final CDOCommitInfo commitInfo) {
		final long commitTimestamp = commitInfo.getTimeStamp();
		
		Entry<Long, CDOBranch> branchToReplicate = branchesByBasetimestamp.floorEntry(commitTimestamp);
		if (branchToReplicate != null) {
			// replicate all branches created before the current commit
			do {
				final CDOBranch branch = branchToReplicate.getValue();
				System.err.println("Replicating branch: " + branch.getName() + " at " + branch.getBase().getTimeStamp());
				context.service(BranchReplicator.class).replicateBranch(branch);
				branchesByBasetimestamp.remove(branchToReplicate.getKey());
				
				// if there are more branches to create at this point
				branchToReplicate = branchesByBasetimestamp.floorEntry(commitTimestamp);
			} while (branchToReplicate != null);
			
			// optimize index after branch creations
			optimize();
		}
		
		System.err.println("Replicating commit: " + commitInfo.getComment() + " at " + commitTimestamp);
		
		final InternalRepository repository = replicatorSession.getManager().getRepository();
		final InternalCDORevisionManager revisionManager = repository.getRevisionManager();
		
		final InternalRepository delegateRepository = new DelegatingRepository() {
			
			@Override
			public InternalCDORevisionManager getRevisionManager() {
				
				return new DelegatingCDORevisionManager() {
				
					@Override
					protected InternalCDORevisionManager getDelegate() {
						return revisionManager;
					}

					@Override
					public EClass getObjectType(CDOID id, CDOBranchManager branchManagerForLoadOnDemand) {
						return revisionManager.getObjectType(id, branchManagerForLoadOnDemand);
					}
					
					/* (non-Javadoc)
					 * @see org.eclipse.emf.cdo.spi.common.revision.DelegatingCDORevisionManager#getRevision(org.eclipse.emf.cdo.common.id.CDOID, org.eclipse.emf.cdo.common.branch.CDOBranchPoint, int, int, boolean)
					 */
					@Override
					public InternalCDORevision getRevision(CDOID id, CDOBranchPoint branchPoint, int referenceChunk, int prefetchDepth, boolean loadOnDemand) {
						
						//future revisions are hidden (no -1)
						if (branchPoint.getTimeStamp() >= commitInfo.getTimeStamp()) {
							return null;
						}
						
						InternalCDORevision revision = super.getRevision(id, branchPoint, referenceChunk, prefetchDepth, loadOnDemand);
						InternalCDORevision copiedRevision = revision.copy();
						
						//we fake later revisions as brand new revision (revised=0)
						if (revision.getRevised() >= commitInfo.getTimeStamp() -1) {
							copiedRevision.setRevised(CDORevision.UNSPECIFIED_DATE);
						}
						return copiedRevision;
					}
					
					/* (non-Javadoc)
					 * @see org.eclipse.emf.cdo.spi.common.revision.DelegatingCDORevisionManager#getRevisionByVersion(org.eclipse.emf.cdo.common.id.CDOID, org.eclipse.emf.cdo.common.branch.CDOBranchVersion, int, boolean)
					 */
					@Override
					public InternalCDORevision getRevisionByVersion(CDOID id, CDOBranchVersion branchVersion, int referenceChunk, boolean loadOnDemand) {
						InternalCDORevision revisionByVersion = super.getRevisionByVersion(id, branchVersion, referenceChunk, loadOnDemand);
						
						InternalCDORevision copiedRevision = revisionByVersion.copy();
						
						//we fake later revisions as brand new revision
						if (revisionByVersion.getRevised() >= commitInfo.getTimeStamp()-1) {
							copiedRevision.setRevised(CDORevision.UNSPECIFIED_DATE);
						}
						return copiedRevision;
					}
					
				};
			}
			
			@Override
			protected InternalRepository getDelegate() {
				return repository;
			}
			
			@Override
			public void endCommit(long timeStamp) {
				//do nothing
			}
			
			@Override
			public void failCommit(long timeStamp) {
				//interrupt the replication process when a commit fails
				throw new RuntimeException("Commit with timestamp " + timeStamp +" failed.  Check the log file for details.");
			}
			
			@Override 
			public void sendCommitNotification(final InternalSession sender, final CDOCommitInfo commitInfo) {
				//do nothing, no post commit notifications are expected
			}
		};
		
		// this is not the actual HEAD of the particular branch!!
		CDOBranch branch = commitInfo.getBranch();
		CDOBranchPoint head = branch.getHead();

		InternalTransaction transaction = replicatorSession.openTransaction(InternalSession.TEMP_VIEW_ID, head);
		DelegatingTransaction delegatingTransaction = new DelegatingTransaction(transaction) {

			//Transaction needs to return the delegating repository as well
			@Override
			public InternalRepository getRepository() {
				return delegateRepository;
			}
		};

		NonWritingTransactionCommitContext commitContext = new NonWritingTransactionCommitContext(delegatingTransaction, commitInfo);

		commitContext.preWrite();
		boolean success = false;

		try {
			commitContext.write(new Monitor());
			commitContext.commit(new Monitor());
			success = true;
		} finally {
			commitContext.postCommit(success);
			transaction.close();
			StoreThreadLocal.setSession(replicatorSession);
		}
	}

	private void optimize() {
		OptimizeRequest.builder(context.id())
			.setMaxSegments(4)
			.build()
			.execute(context);
	}

	@Override
	public int getLastReplicatedBranchID() {
		return initialBranchId;
	}

	@Override
	public long getLastReplicatedCommitTime() {
		return initialLastCommitTime;
	}

	@Override
	public String[] getLockAreaIDs() {
		return new String[] {};
	}

	@Override
	public void handleBranch(CDOBranch branch) {
		branchesByBasetimestamp.put(branch.getBase().getTimeStamp(), branch);
	}

	@Override
	public boolean handleLockArea(LockArea area) {
		return false;
	}

}