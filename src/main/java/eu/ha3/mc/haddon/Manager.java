package eu.ha3.mc.haddon;

import net.minecraft.src.KeyBinding;

/* x-placeholder-wtfplv2 */

public interface Manager
{
	/**
	 * Gets the utility object dedicated to this manager.
	 * 
	 * @return
	 */
	public Utility getUtility();
	
	/**
	 * Adds a key binding with a certain localization.
	 * 
	 * @param keyBindingIn
	 * @param localization
	 */
	public void addKeyBinding(KeyBinding keyBindingIn, String localization);

}
