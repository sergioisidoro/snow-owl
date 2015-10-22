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
package com.b2international.snowowl.snomed.datastore.internal.id.beans;

import java.util.Collection;

/**
 * Bean to represent bulk id publish data used by IHTSDO's
 * ID generation service.
 * Instances of this class are meant to be serialized and deserialized
 * to and from Json.
 * {
  "records": [
    {
      "sctid": "string",
      "systemId": "string"
    }
  ],
  "namespace": 0,
  "software": "string",
  "comment": "string"
}
 */
public class BulkRegisterData extends CisRequestData {
	
	public class Record {
		
		private String sctid;
		
		private String systemId;

		public String getSctid() {
			return sctid;
		}

		public void setSctid(String sctid) {
			this.sctid = sctid;
		}

		public String getSystemId() {
			return systemId;
		}

		public void setSystemId(String systemId) {
			this.systemId = systemId;
		}
	}
	
	private Collection<Record> records;

	/**
	 * @return the records
	 */
	public Collection<Record> getRecords() {
		return records;
	}

	/**
	 * @param sctids the sctids to set
	 */
	public void setRecords(Collection<Record> records) {
		this.records = records;
	}
	
	/**
	 * Adds a record to this request data
	 * @param record
	 */
	public void addRecord(Record record) {
		records.add(record);
	}
	
}
