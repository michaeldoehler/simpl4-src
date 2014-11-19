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
package org.ms123.common.team.api;
import java.util.Map;
import java.util.Collection;

public interface TeamService {
	public boolean checkTeams(String namespace, String userName, Map userProperties, Collection<Object> teams);
	public int getTeamStatus(String namespace, Map teamMap, String teamid, String user);
	public int checkTeamUserPermission(String namespace, String teamid, String userName );
	public boolean checkTeamDate(Map teamMap);
	public boolean canCreateTeam(String namespace, String teamid,String userName);
	public boolean canManageTeam(String namespace, String teamid,String userName);
}
