package com.evandev.reliable_advancements.platform;

import com.evandev.reliable_advancements.platform.services.IPlatformHelper;

//? if fabric {
/*import com.evandev.reliable_advancements.fabric.platform.FabricPlatformHelper;
*///?} else {
import com.evandev.reliable_advancements.neoforge.platform.NeoForgePlatformHelper;
//?}

public class Services {
    //? if fabric {
    /*public static final IPlatformHelper PLATFORM = new FabricPlatformHelper();
    *///?} else {
    public static final IPlatformHelper PLATFORM = new NeoForgePlatformHelper();
    //?}
}
