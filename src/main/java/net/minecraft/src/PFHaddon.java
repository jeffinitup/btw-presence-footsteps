package net.minecraft.src;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import static com.jeffyjamzhd.BTWPresenceFootsteps.LOGGER;

import com.jeffyjamzhd.BTWPresenceFootsteps;
import eu.ha3.easy.EdgeModel;
import eu.ha3.easy.EdgeTrigger;
import eu.ha3.mc.convenience.Ha3StaticUtilities;
import eu.ha3.mc.haddon.Identity;
import eu.ha3.mc.haddon.SupportsFrameEvents;
import eu.ha3.mc.haddon.implem.Ha3Utility;
import eu.ha3.mc.haddon.implem.HaddonImpl;
import eu.ha3.mc.presencefootsteps.mcpackage.implem.AcousticsManager;
import eu.ha3.mc.presencefootsteps.mcpackage.implem.BasicBlockMap;
import eu.ha3.mc.presencefootsteps.mcpackage.implem.BasicPrimitiveMap;
import eu.ha3.mc.presencefootsteps.mcpackage.implem.NormalVariator;
import eu.ha3.mc.presencefootsteps.mcpackage.interfaces.BlockMap;
import eu.ha3.mc.presencefootsteps.mcpackage.interfaces.PrimitiveMap;
import eu.ha3.mc.presencefootsteps.mcpackage.interfaces.Variator;
import eu.ha3.mc.presencefootsteps.mod.UpdateNotifier;
import eu.ha3.mc.presencefootsteps.mod.UserConfigSoundPlayerWrapper;
import eu.ha3.mc.presencefootsteps.parsers.JasonAcoustics_Engine0;
import eu.ha3.mc.presencefootsteps.parsers.PropertyBlockMap_Engine0;
import eu.ha3.mc.presencefootsteps.parsers.PropertyPrimitiveMap_Engine0;
import eu.ha3.util.property.simple.ConfigProperty;
import org.lwjgl.input.Keyboard;

/*
            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE 
                    Version 2, December 2004 

 Copyright (C) 2004 Sam Hocevar <sam@hocevar.net> 

 Everyone is permitted to copy and distribute verbatim or modified 
 copies of this license document, and changing it is allowed as long 
 as the name is changed. 

            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE 
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION 

  0. You just DO WHAT THE FUCK YOU WANT TO. 
*/

public class PFHaddon extends HaddonImpl implements SupportsFrameEvents, ResourceManagerReloadListener
{
	public static final int VERSION = 1;
	public static final String FOR = "1.6.4";

	private boolean hasTickedOnce = false;
	
	private File presenceDir;
	private File packsFolder;
	
	private PFCacheRegistry cache;
	private EdgeTrigger debugButton;
	private EdgeTrigger reloadButton;
	private static boolean isDebugEnabled;
	
	private ConfigProperty config;
	
	private File currentPackFolder;
	private PFIsolator isolator;
	
	private static String DEFAULT_PACK_NAME = "pf_presence";
	private List<ResourcePack> resourcePacks;
	private int infoCount;
	private int showInfo;

	@SuppressWarnings("unchecked")
	@Override
	public void onLoad()
	{
		this.presenceDir = new File(util().getModsFolder(), "presencefootsteps/");
		this.packsFolder = new File(this.presenceDir, "packs/");
		
		if (!this.presenceDir.exists()) {
			this.presenceDir.mkdirs();
		}
		if (!this.packsFolder.exists()) {
			this.packsFolder.mkdirs();
		}
		this.cache = new PFCacheRegistry();
		
		this.debugButton = new EdgeTrigger(new EdgeModel() {
			@Override
			public void onTrueEdge()
			{
				toggleDebug();
				printChat("Verbose logging has been " + (isDebugEnabled ? "enabled." : "disabled."));
			}
			
			@Override
			public void onFalseEdge()
			{
			}
		});

		this.reloadButton = new EdgeTrigger(new EdgeModel() {
			@Override
			public void onTrueEdge()
			{
				reloadEverything(true);
				printChat("Quick reloading...");
			}

			@Override
			public void onFalseEdge()
			{
			}
		});

		this.resourcePacks = Minecraft.getMinecraft().defaultResourcePacks;
		for (File file : new File(this.presenceDir, "packs/").listFiles())
		{
			if (file.isDirectory())
			{
				PFHaddon.log("Adding resource pack at " + file.getAbsolutePath());
				this.resourcePacks.add(new FolderResourcePack(file));
			}
		}
	}

	@Override
	public Identity getIdentity() {
		return null;
	}

	public void initialReload() {
		// Register listener
		((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
				.registerReloadListener(this);
	}

	public void reloadEverything(boolean nested)
	{
		this.isolator = new PFIsolator(this);
		
		// Sets up the pack
		reloadConfig();
		
		if (!this.currentPackFolder.exists())
		{
			PFHaddon.log("The pack '" + this.currentPackFolder.getPath() + "'does not exist!");
			if (!nested)
			{
				this.currentPackFolder = new File(this.packsFolder, PFHaddon.DEFAULT_PACK_NAME + "/");
				PFHaddon.log("The pack '" + this.currentPackFolder.getPath() + "'does not exist!");
				
				reloadEverything(true);
			}
			else
				throw new RuntimeException(
					"Presence Footsteps cannot run because the default custom pack does not exist in the "
						+ new File(this.packsFolder, PFHaddon.DEFAULT_PACK_NAME + "/").getAbsolutePath() + " folder.");
		}
		
		reloadBlockMapFromFile();
		reloadPrimitiveMapFromFile();
		reloadAcousticsFromFile();
		this.isolator.setSolver(new PFSolver(this.isolator));
		reloadVariatorFromFile();
		loadSoundsFromPack(this.currentPackFolder);
		
		this.isolator.setGenerator(new PFReaderH(this.isolator));
	}
	
	private void reloadConfig()
	{
		this.config = new ConfigProperty();
		this.config.setProperty("user.volume.0-to-100", 50);
		this.config.setProperty("user.packname.r0", PFHaddon.DEFAULT_PACK_NAME);
		this.config.setProperty("user.boot.show", true);
		this.config.setProperty("user.boot.count", 3);
		this.config.commit();
		
		boolean fileExisted = new File(this.presenceDir, "userconfig.cfg").exists();
		
		try
		{
			this.config.setSource(new File(this.presenceDir, "userconfig.cfg").getCanonicalPath());
			this.config.load();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			throw new RuntimeException("Error caused config not to work: " + e.getMessage());
		}
		
		if (!fileExisted)
		{
			this.config.save();
		}
		
		this.currentPackFolder =
			new File(this.packsFolder, (this.config.getAllProperties().containsKey("user.packname.r0")
				? this.config.getString("user.packname.r0") : PFHaddon.DEFAULT_PACK_NAME)
				+ "/");
	}
	
	private void reloadVariatorFromFile()
	{
		Variator var = new NormalVariator();
		
		File configFile = new File(this.currentPackFolder, "variator.cfg");
		if (configFile.exists())
		{
			try
			{
				ConfigProperty config = new ConfigProperty();
				config.setSource(configFile.getCanonicalPath());
				config.load();
				
				var.loadConfig(config);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				PFHaddon.log("Loading default configuration failed: " + e.getMessage());
			}
		}
		
		this.isolator.setVariator(var);
	}
	
	private void reloadBlockMapFromFile()
	{
		BlockMap blockMap = new BasicBlockMap();
		
		try
		{
			ConfigProperty blockSound = new ConfigProperty();
			blockSound.setSource(new File(this.currentPackFolder, "blockmap.cfg").getCanonicalPath());
			blockSound.load();
			
			new PropertyBlockMap_Engine0().setup(blockSound, blockMap);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			PFHaddon.log("Loading default blockmap failed: " + e.getMessage());
		}
		
		this.isolator.setBlockMap(blockMap);
	}
	
	private void reloadPrimitiveMapFromFile()
	{
		PrimitiveMap primitiveMap = new BasicPrimitiveMap();
		
		try
		{
			ConfigProperty primitiveSound = new ConfigProperty();
			primitiveSound.setSource(new File(this.currentPackFolder, "primitivemap.cfg").getCanonicalPath());
			primitiveSound.load();
			
			new PropertyPrimitiveMap_Engine0().setup(primitiveSound, primitiveMap);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			PFHaddon.log("Loading default primitivemap failed: " + e.getMessage());
		}
		
		this.isolator.setPrimitiveMap(primitiveMap);
	}
	
	private void reloadAcousticsFromFile()
	{
		AcousticsManager acoustics = new AcousticsManager(this.isolator);
		
		try
		{
			String jasonString =
				new Scanner(new File(this.currentPackFolder, "acoustics.json")).useDelimiter("\\Z").next();
			
			new JasonAcoustics_Engine0("").parseJSON(jasonString, acoustics);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			PFHaddon.log("Loading default acoustics failed: " + e.getMessage());
		}
		
		this.isolator.setAcoustics(acoustics);
		this.isolator.setSoundPlayer(new UserConfigSoundPlayerWrapper(acoustics, this.config));
		this.isolator.setDefaultStepPlayer(acoustics);
	}

	private void loadSoundsFromPack(File pack)
	{
		File soundFolder = new File(pack, "assets/minecraft/sound/");
		if (soundFolder.exists())
		{
			int count = loadResource(soundFolder, "");
			log("Successfully loaded %d resources.".formatted(count));
		}
	}

	@Override
	public void onFrame(float semi)
	{
		EntityPlayer ply = Minecraft.getMinecraft().thePlayer;
		if (ply == null)
			return;
		
		this.isolator.onFrame();

		// Check first tick
		if (!this.hasTickedOnce) {
			int bootCount = this.config.getInteger("user.boot.count");
			boolean shouldShowHelp = this.config.getBoolean("user.boot.show");

			if (shouldShowHelp && bootCount > 0) {
				int remainingInfo = bootCount - 1;
				printChat("Thank you for installing BTWPF! You can open the settings GUI by pressing F9.");
				if (remainingInfo > 0)
					printChat("This message will only show %d more time%s.".formatted(remainingInfo, remainingInfo > 1 ? "s" : ""));
				this.config.setProperty("user.boot.count", remainingInfo);
			}

			this.config.commit();
			this.config.save();
			this.hasTickedOnce = true;
		}

		// Debug mode
		boolean debugKeys = util().areKeysDown(29, 42, 33);
		this.debugButton.signalState(debugKeys); // CTRL SHIFT F

		// Hot reload for PF
		boolean hotReloadKeys = util().areKeysDown(Keyboard.KEY_LCONTROL, Keyboard.KEY_P);
		this.reloadButton.signalState(hotReloadKeys);

		// Check for menu
		boolean confKeyDown = util().areKeysDown(Keyboard.KEY_F9);
		if (confKeyDown && !hotReloadKeys) {
			if (util().isCurrentScreen(null)) {
				Minecraft.getMinecraft().displayGuiScreen(new PFGuiMenu((GuiScreen) util().getCurrentScreen(), this));
			}
		}

		ply.nextStepDistance = Integer.MAX_VALUE;
	}
	
	/**
	 * Loads a resource and passes it to Minecraft to install.
	 */
	private int loadResource(File par1File, String root)
	{
		File[] filesInThisDir = par1File.listFiles();
		int fileCount = filesInThisDir.length;
		int successCount = 0;

		for (int i = 0; i < fileCount; ++i)
		{
			File file = filesInThisDir[i];
			
			if (file.isDirectory())
			{
				successCount += loadResource(file, root + file.getName() + "/");
			}
			else
			{
				try
				{
					this.cache.cacheSound(root + file.getName());
					successCount++;
				}
				catch (Exception var9)
				{
					warn("Failed to add " + root + file.getName());
				}
			}
		}
		return successCount;
	}
	
	public ConfigProperty getConfig()
	{
		return this.config;
	}
	
	public void printChat(Object... args)
	{
		printChat(new Object[] { Ha3Utility.COLOR_GOLD, "<%s> ".formatted(BTWPresenceFootsteps.getShorthand()), Ha3Utility.COLOR_WHITE }, args);
	}
	
	public void printChatShort(Object... args)
	{
		printChat(new Object[] { Ha3Utility.COLOR_WHITE, "" }, args);
	}
	
	protected void printChat(final Object[] in, Object... args)
	{
		Object[] dest = new Object[in.length + args.length];
		System.arraycopy(in, 0, dest, 0, in.length);
		System.arraycopy(args, 0, dest, in.length, args.length);
		
		util().printChat(dest);
	}
	
	public static void log(String contents)
	{
        LOGGER.info(contents);
	}

	public static void fatal(String contents)
	{
		LOGGER.fatal(contents);
	}

	public static void warn(String contents) {
		LOGGER.warn(contents);
	}

	public static void toggleDebug() {
		setDebugEnabled(!isDebugEnabled);
	}

	public static void setDebugEnabled(boolean enable)
	{
		isDebugEnabled = enable;
	}
	
	public static void debug(String contents)
	{
		if (!isDebugEnabled)
			return;

		LOGGER.info(contents);
	}
	
	public void saveConfig()
	{
		// If there were changes...
		if (this.config.commit())
		{
			PFHaddon.log("Saving configuration...");
			
			// Write changes on disk.
			this.config.save();
		}
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		this.reloadEverything(true);
	}
}
