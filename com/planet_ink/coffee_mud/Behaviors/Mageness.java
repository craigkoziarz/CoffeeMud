package com.planet_ink.coffee_mud.Behaviors;

import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
import com.planet_ink.coffee_mud.utils.*;
import java.util.*;

public class Mageness extends CombatAbilities
{
	public Mageness()
	{
		myID=this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.')+1);
	}

	public Behavior newInstance()
	{
		return new Mageness();
	}

	public void startBehavior(Environmental forMe)
	{
		super.startBehavior(forMe);
		if(!(forMe instanceof MOB)) return;
		MOB mob=(MOB)forMe;
		if(!mob.baseCharStats().getMyClass().ID().equals("Mage"))
		{
			mob.baseCharStats().setMyClass(CMClass.getCharClass("Mage"));
			mob.recoverCharStats();
		}
		// now equip character...
		newCharacter(mob);
		getSomeMoreMageAbilities(mob);
	}
}