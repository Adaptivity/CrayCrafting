/*
 * Copyright (c) 2014, DoubleDoorDevelopment
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the project nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.doubledoordev.craycrafting;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.doubledoordev.craycrafting.network.RecipeMessage;
import net.doubledoordev.craycrafting.network.ResetMessage;
import net.doubledoordev.craycrafting.recipes.*;
import net.doubledoordev.lib.DevPerks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.Logger;

import java.io.File;

import static net.doubledoordev.craycrafting.util.Constants.MODID;

/**
 * @author Dries007
 */
@Mod(modid = MODID)
public class CrayCrafting
{
    @Mod.Instance(MODID)
    public static CrayCrafting instance;

    private boolean                       debug       = false;
    public Logger logger;
    private SimpleNetworkWrapper snw;

    public static SimpleNetworkWrapper getSnw()
    {
        return instance.snw;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);

        Configuration configuration = new Configuration(event.getSuggestedConfigurationFile());

        debug = configuration.getBoolean("debug", MODID, debug, "Enable extra debug output.");
        if (configuration.getBoolean("sillyness", MODID, true, "Disable sillyness only if you want to piss off the developers XD")) MinecraftForge.EVENT_BUS.register(new DevPerks(debug));
        if (configuration.hasChanged()) configuration.save();

        new ShapedRecipesType();
        new ShapelessRecipesType();
        new ShapedOreRecipeType();
        new ShapelessOreRecipeType();

        int id = 0;
        snw = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        snw.registerMessage(RecipeMessage.Handler.class, RecipeMessage.class, id++, Side.CLIENT);
        snw.registerMessage(ResetMessage.Handler.class, ResetMessage.class, id++, Side.CLIENT);
    }

    @Mod.EventHandler()
    public void eventHandler(FMLServerStartingEvent event)
    {
        File recipeFile = new File(DimensionManager.getCurrentSaveRootDirectory(), MODID + ".dat");
        if (recipeFile.exists())
        {
            try
            {
                RecipeRegistry.loadRecipesFromNBT(CompressedStreamTools.read(recipeFile));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            RecipeRegistry.randomizeRecipes(recipeFile);
        }
    }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (MinecraftServer.getServer().isDedicatedServer()) RecipeRegistry.sendPacketTo(event.player);
    }
}