package com.orbital.signicons;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignIcons implements ClientModInitializer {
    public static final String MOD_ID = "signicons";
    public static final Logger LOGGER = LoggerFactory.getLogger("SignIcons");

    @Override
    public void onInitializeClient() {
        LOGGER.info("SignIcons loaded");
    }
}