package com.planet_ink.coffee_mud.Items.Software;
import com.planet_ink.coffee_mud.Items.Basic.StdItem;
import com.planet_ink.coffee_mud.Items.BasicTech.GenElecItem;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.Technical.TechType;
import com.planet_ink.coffee_mud.Libraries.interfaces.GenericBuilder;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.*;

/* 
   Copyright 2000-2013 Bo Zimmerman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
public class LifeScanProgram extends GenSoftware
{
	public String ID(){	return "LifeScanProgram";}
	
	protected final static short AUTO_TICKDOWN=4;
	
	protected boolean activated=false;
	protected short activatedTickdown=AUTO_TICKDOWN;
	
	public LifeScanProgram()
	{
		super();
		setName("a lifescan minidisk");
		setDisplayText("a small disk sits here.");
		setDescription("Lifescan software, for small computer/scanners, will reveal life in the surrounding area.");
		super.setCurrentScreenDisplay("LIFESCAN: Activate for continual scanning, type for on-demand.\n\r");
		basePhyStats().setWeight(1); // the higher the weight, the wider the scan
		recoverPhyStats();
	}
	
	@Override public String getParentMenu() { return ""; }
	@Override public String getInternalName() { return "";}
	
	public boolean isAlive(MOB M)
	{
		// there you have it, the definition of "life" -- is biological, and can reproduce
		return ((M!=null)&&(!CMLib.flags().isGolem(M)) && (M.charStats().getMyRace().canBreedWith(M.charStats().getMyRace())));
	}
	
	public CMMsg getScanMsg(Room R)
	{
		return CMClass.getMsg(CMLib.map().getFactoryMOB(R), null, this, CMMsg.MASK_CNTRLMSG|CMMsg.MSG_LOOK, null); // cntrlmsg is important
	}

	public int getScanMsg(Room R, Set<Room> roomsDone, String dirBuilder, int depthLeft, CMMsg scanMsg, StringBuilder str)
	{
		if((R==null)||(roomsDone.contains(R))) return 0;
		roomsDone.add(R);
		int numFound=0;
		final boolean useShipDirs=(R instanceof SpaceShip)||(R.getArea() instanceof SpaceShip);
		for(int m=0;m<R.numInhabitants();m++)
		{
			MOB M=R.fetchInhabitant(m);
			if(isAlive(M))
			{
				scanMsg.setTarget(M);
				if(R.okMessage(scanMsg.source(), scanMsg))
				{
					numFound++;
					str.append("A "+M.charStats().getMyRace().name());
					int numDone=0;
					int numTotal=0;
					for(int d=0;d<dirBuilder.length();d++)
						numTotal+=(Character.isLowerCase(dirBuilder.charAt(d))?1:0);
					if(dirBuilder.length()==0)
						str.append(" here");
					else
					for(int d=0;d<dirBuilder.length();d++)
					{
						String locDesc="";
						if(dirBuilder.charAt(d)=='D')
						{
							locDesc="behind a door ";
							d++;
						}
						if(dirBuilder.charAt(d)=='I')
						{
							locDesc="inside a room ";
							d++;
						}
						if(dirBuilder.charAt(d)=='o')
						{
							locDesc="outdoors ";
							d++;
						}
						int dir=dirBuilder.charAt(d)-'a';
						if(numDone==0)
							str.append(" ").append(locDesc).append(useShipDirs?Directions.getShipDirectionName(dir):Directions.getDirectionName(dir));
						else
						if(numDone<numTotal-1)
							str.append(", ").append(locDesc).append(useShipDirs?Directions.getShipDirectionName(dir):Directions.getDirectionName(dir));
						else
							str.append(", and then ").append(locDesc).append(useShipDirs?Directions.getShipInDirectionName(dir):Directions.getInDirectionName(dir));
						numDone++;
					}
					str.append(".\n\r");
				}
			}
		}
		if(depthLeft>0)
		{
			boolean isIndoors=(R.domainType()&Room.INDOORS)==Room.INDOORS;
			for(int d=0;d<Directions.NUM_DIRECTIONS();d++)
			{
				Room R2=R.getRoomInDir(d);
				Exit E2=R.getExitInDir(d);
				if((R2==null)||(E2==null)) continue;
				boolean willIndoors=(R.domainType()&Room.INDOORS)==Room.INDOORS;
				boolean willADoor=E2.hasADoor() && !E2.isOpen();
				String dirBCode=willADoor?"D":
								(isIndoors && (!willIndoors))?"O":
								(!isIndoors && (willIndoors))?"I":
								"";
				numFound+=getScanMsg(R2, new HashSet<Room>(), dirBuilder+dirBCode+((char)(((int)'a')+d)), depthLeft-1, scanMsg, str);
			}
		}
		return numFound;
	}
	
	public String getScanMsg()
	{
		final Room R=CMLib.map().roomLocation(this);
		if(R==null) return "";
		StringBuilder str=new StringBuilder("");
		int numFound=getScanMsg(R,new HashSet<Room>(), "",phyStats().weight()+1,getScanMsg(R),str);
		if(activated)
			super.setCurrentScreenDisplay("LIFESCAN: Activated: "+numFound+" found last scan.\n\r");
		else
			super.setCurrentScreenDisplay("LIFESCAN: Activate for continual scanning, type for on-demand.\n\r");
		if(str.length()==0)
			return "No life signs detected.";
		return str.toString().toLowerCase();
	}
	
	@Override 
	public boolean isActivationString(String word) 
	{ 
		return "lifescan".startsWith(word.toLowerCase()); 
	}
	
	@Override 
	public boolean isDeActivationString(String word) 
	{ 
		return "lifescan".startsWith(word.toLowerCase()); 
	}
	
	@Override 
	public boolean isCommandString(String word, boolean isActive) 
	{ 
		return "lifescan".startsWith(word.toLowerCase());
	}

	@Override 
	public String getActivationMenu() 
	{ 
		return super.getActivationMenu(); 
	}
	
	@Override 
	public boolean checkActivate(MOB mob, String message)
	{
		return super.checkActivate(mob, message);
	}
	
	@Override 
	public boolean checkDeactivate(MOB mob, String message)
	{
		return super.checkDeactivate(mob, message);
	}
	
	@Override 
	public boolean checkTyping(MOB mob, String message)
	{
		return super.checkTyping(mob, message);
	}
	
	@Override 
	public boolean checkPowerCurrent(int value)
	{
		return super.checkPowerCurrent(value);
	}
	
	@Override 
	public void onActivate(MOB mob, String message)
	{
		super.onActivate(mob, message);
		this.activated=true;
		activatedTickdown=AUTO_TICKDOWN;
		//TODO: lifescan for particular races? Is that a special version of lifescan?
		String scan=getScanMsg();
		if(scan.length()>0)
			super.addScreenMessage(scan);
	}
	
	@Override 
	public void onDeactivate(MOB mob, String message)
	{
		super.onDeactivate(mob, message);
		if(activated)
			super.addScreenMessage("Life scanning deactivated.");
		this.activated=false;
	}
	
	@Override 
	public void onTyping(MOB mob, String message)
	{
		super.onTyping(mob, message);
		String scan=getScanMsg();
		if(scan.length()>0)
			super.addScreenMessage(scan);
	}
	
	@Override 
	public void onPowerCurrent(int value)
	{
		super.onPowerCurrent(value);
		if((value != 0)&&(activated)&&(--activatedTickdown>=0)) // means there was power to give, 2 means is active menu, which doesn't apply 
		{
			String scan=getScanMsg();
			if(scan.length()>0)
				super.addScreenMessage(scan);
		}
	}
}