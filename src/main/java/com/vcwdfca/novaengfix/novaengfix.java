package com.vcwdfca.novaengfix;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vcwdfca.novaengfix.hypernet.HyperNetFixLifecycle;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class novaengfix {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    /**
     * <a href="https://cleanroommc.com/wiki/forge-mod-development/event#overview">
     *     Take a look at how many FMLStateEvents you can listen to via the @Mod.EventHandler annotation here
     * </a>
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new HyperNetFixLifecycle());
        LOGGER.info("Hello From {}!", Tags.MOD_NAME);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        HyperNetFixLifecycle.clearAll();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        HyperNetFixLifecycle.serverStarting();
    }

}
