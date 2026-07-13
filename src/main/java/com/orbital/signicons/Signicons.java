package com.orbital.signicons;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SignIcons.MOD_ID)
public class SignIcons {
    public static final String MOD_ID = "signicons";
    public static final Logger LOGGER = LoggerFactory.getLogger("SignIcons");

    public SignIcons() {
        LOGGER.info("SignIcons loaded");
    }
}