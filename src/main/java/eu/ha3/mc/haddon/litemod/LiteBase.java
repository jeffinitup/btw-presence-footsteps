package eu.ha3.mc.haddon.litemod;

import java.io.File;

import net.minecraft.src.Minecraft;
import eu.ha3.mc.haddon.Haddon;
import eu.ha3.mc.haddon.OperatorCaster;
import eu.ha3.mc.haddon.SupportsFrameEvents;
import eu.ha3.mc.haddon.SupportsTickEvents;
import eu.ha3.mc.haddon.Utility;

import eu.ha3.mc.haddon.implem.HaddonUtilityImpl;

/*
--filenotes-placeholder 
*/
/* x-placeholder-wtfplv2 */

public class LiteBase implements OperatorCaster
{
	private Utility utility;
	protected final Haddon haddon;
	protected final boolean shouldTick;
	protected final boolean suTick;
	protected final boolean suFrame;
	
	protected int tickCounter;
	protected boolean enableTick = true;
	protected boolean enableFrame = true;
	protected boolean isLoaded = false;

	private long ticksRan;
	
	public LiteBase(Haddon haddon)
	{
		this.haddon = haddon;
		this.suTick = haddon instanceof SupportsTickEvents;
		this.suFrame = haddon instanceof SupportsFrameEvents;
		this.shouldTick = this.suTick || this.suFrame;
		
		this.haddon.setUtility(new HaddonUtilityImpl() {
			@Override
			public long getClientTick()
			{
				return getTicks();
			}
		});
		
		this.haddon.setOperator(this);

	}
	
	public String getName()
	{
		return this.haddon.getIdentity().getHaddonName();
	}
	
	public String getVersion()
	{
		return this.haddon.getIdentity().getHaddonHumanVersion();
	}
	
	public void onInitCompleted(Minecraft minecraft)
	{
		this.haddon.onLoad();
		this.isLoaded = true;
	}

	public Utility getUtility()
	{
		return this.utility;
		
	}
	
	public long bridgeTicksRan()
	{
		return this.ticksRan;
		
	}
	
	public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock)
	{
		if (!this.shouldTick)
			return;
		
		if (!inGame)
			return;
		
		if (clock && this.enableTick)
		{
			if (this.suTick)
			{
				((SupportsTickEvents) this.haddon).onTick();
			}
			this.tickCounter++;
		}

		if (this.suFrame)
		{
			((SupportsFrameEvents) this.haddon).onFrame(partialTicks);
		}
	}
	
	@Override
	public void setTickEnabled(boolean enabled)
	{
		this.enableTick = enabled;
	}
	
	@Override
	public void setFrameEnabled(boolean enabled)
	{
		this.enableFrame = enabled;
	}
	
	@Override
	public int getTicks()
	{
		return this.tickCounter;
	}

	public void init(File configPath) {
		// TODO Auto-generated method stub
		
	}

	public void upgradeSettings(String version, File configPath,
			File oldConfigPath) {
		// TODO Auto-generated method stub
		
	}

	public boolean isLoaded() {
		return this.isLoaded;
	}

	public Haddon getAddon() {
		return this.haddon;
	}
}
