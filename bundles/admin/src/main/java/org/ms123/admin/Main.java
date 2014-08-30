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
package org.ms123.admin;


import com.beust.jcommander.*;
import java.util.*;
 

public class Main {

	private static String[] commands = { "ExecuteScript", "GenerateClasses", "CreateWorkspace" };

	public static void main(String args[]) {
		if (args.length < 2) {
			Main.glocalHelp();
			System.exit(1);
		}
		String[] argsNew = new String[args.length - 1];
		Main.copyArgs(args, argsNew);
		AbstractCommand esc = null;
		for( String command : Main.commands ){
			if (args[0].toLowerCase().equals(command.toLowerCase())) {
				try{
					Class c = Class.forName( "org.ms123.admin."+command+"Command" );
					esc = (AbstractCommand)c.newInstance();	
				}catch( Exception e){
					e.printStackTrace();
					System.exit(2);
				}
				break;
			}
		}
		if (esc != null) {
			JCommander jc = new JCommander(esc, argsNew);
			if (esc.help) {
				jc.usage();
			} else {
				esc.execute();
			}
		} else {
			Main.glocalHelp();
			System.exit(1);
		}
	}

	private static void glocalHelp(){
			StringBuffer sb = new StringBuffer();
			sb.append("[");
			String sep="";
			for( String command : Main.commands ){
				sb.append( sep + command.toLowerCase() );
				sep = "|";
			}
			sb.append("]");
			System.out.println("Usage:sw "+sb.toString()+"  options");
	}
	private static void copyArgs(String[] args, String[] argsNew) {
		for (int i = 0; i < argsNew.length; ++i) {
			argsNew[i] = args[i + 1];
		}
	}
}
