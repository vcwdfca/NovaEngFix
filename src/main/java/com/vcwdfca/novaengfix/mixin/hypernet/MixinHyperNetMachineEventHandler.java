package com.vcwdfca.novaengfix.mixin.hypernet;

import com.vcwdfca.novaengfix.hypernet.HyperNetFixLifecycle;
import crafttweaker.util.IEventHandler;
import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.mmce.common.event.machine.MachineTickEvent;
import github.kasuminova.novaeng.common.handler.HyperNetMachineEventHandler;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = HyperNetMachineEventHandler.class, remap = false)
public abstract class MixinHyperNetMachineEventHandler {
    @Unique
    private static final IEventHandler<MachineTickEvent> NOVAENGFIX_HANDLER =
        HyperNetMachineEventHandler::onMachineTick;

    @Inject(method = "onMachineTick(Lgithub/kasuminova/mmce/common/event/machine/MachineTickEvent;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void novaengfix$moveNodeTickToServerThread(MachineTickEvent event, CallbackInfo ci) {
        if (event.phase == Phase.START) {
            TileMultiblockMachineController controller = event.getController();
            if (controller != null && !HyperNetFixLifecycle.isReloading()) {
                if (!(controller instanceof github.kasuminova.novaeng.common.tile.TileHyperNetTerminal)) {
                    HyperNetFixLifecycle.scheduleNodeTick(controller);
                }
            }
        }
        ci.cancel();
    }

    //@SuppressWarnings({"rawtypes"})
    @Inject(method = "registerHandler()V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void novaengfix$registerOnce(CallbackInfo ci) {
        for (ResourceLocation machineName : RegistryHyperNet.getAllHyperNetSupportedMachinery()) {
            if (HyperNetFixLifecycle.isTerminalMachine(machineName)) {
                continue;
            }
            DynamicMachine machine = MachineRegistry.getRegistry().getMachine(machineName);
            if (machine == null) {
                continue;
            }

            List<?> handlers = machine.getMachineEventHandlers(MachineTickEvent.class);
            if (handlers != null) {
                handlers.removeIf(handler -> handler != NOVAENGFIX_HANDLER &&
                    handler.getClass().getName().contains("HyperNetMachineEventHandler"));
            }
            if (handlers == null || !handlers.contains(NOVAENGFIX_HANDLER)) {
                machine.addMachineEventHandler(MachineTickEvent.class, NOVAENGFIX_HANDLER);
            }
        }
        ci.cancel();
    }
}
