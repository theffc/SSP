/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.ssp.transferobject.reference;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.jasig.ssp.model.reference.Category;
import org.jasig.ssp.transferobject.TransferObject;

import com.google.common.collect.Lists;

public class CategoryTO extends AbstractReferenceTO<Category>
		implements TransferObject<Category> {

	public CategoryTO() {
		super();
	}

	public CategoryTO(final UUID id, final String name,
			final String description) {
		super(id, name, description);
	}

	public CategoryTO(final Category model) {
		super();
		from(model);
	}

	public static List<CategoryTO> toTOList(
			final Collection<Category> models) {
		final List<CategoryTO> tObjects = Lists.newArrayList();
		for (Category model : models) {
			tObjects.add(new CategoryTO(model));
		}
		return tObjects;
	}
}
