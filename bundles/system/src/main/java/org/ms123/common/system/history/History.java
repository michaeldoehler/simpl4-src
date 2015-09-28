/**
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ms123.common.system.history;

import java.util.UUID;
import java.util.Date;

import com.noorq.casser.mapping.annotation.Column;
import com.noorq.casser.mapping.annotation.PartitionKey;
import com.noorq.casser.mapping.annotation.ClusteringColumn;
import com.noorq.casser.mapping.annotation.Types;
import com.noorq.casser.mapping.annotation.Table;
import com.noorq.casser.mapping.annotation.Index;

@Table
public interface History{

	@PartitionKey(ordinal=0)
	String key();

	@PartitionKey(ordinal=1)
	String type();

	@ClusteringColumn
	@Types.Timeuuid
	Date time();

	@Column
	String hint();

	@Column
	String msg();
}
