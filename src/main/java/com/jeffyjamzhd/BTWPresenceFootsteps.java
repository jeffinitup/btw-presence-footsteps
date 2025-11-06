package com.jeffyjamzhd;

import btw.BTWAddon;

import net.minecraft.src.PFHaddon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BTWPresenceFootsteps extends BTWAddon {
    public static final String MOD_ID = "presence-footsteps";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final PFHaddon ADDON = new PFHaddon();

    @Override
    public void initialize() {
    }

    public static String getShorthand() {
        return "BTWPF";
    }
}
