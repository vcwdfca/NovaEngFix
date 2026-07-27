package com.vcwdfca.novaengfix.mixin.hypernet;

import com.vcwdfca.novaengfix.hypernet.HyperNetFixLifecycle;
import com.vcwdfca.novaengfix.hypernet.ITerminalTickFix;
import github.kasuminova.mmce.common.event.Phase;
import github.kasuminova.novaeng.common.tile.TileHyperNetTerminal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileHyperNetTerminal.class, remap = false)
public abstract class MixinTileHyperNetTerminal implements ITerminalTickFix {
    @Inject(method = "doControllerTick()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void novaengfix$scheduleOnServerThread(CallbackInfo ci) {
        HyperNetFixLifecycle.scheduleTerminalTick((TileHyperNetTerminal) (Object) this);
        ci.cancel();
    }

    @Override
    @Unique
    public void novaengfix$runTerminalTick() {
        TileHyperNetTerminal terminal = (TileHyperNetTerminal) (Object) this;
        if (!((InvokerTileMultiblockMachineControllerStructureCheck) terminal)
            .novaengfix$invokeDoStructureCheck() || !terminal.isStructureFormed()) {
            return;
        }

        terminal.onMachineTick(Phase.START);
        if (terminal.consumeEnergy()) {
            terminal.getNodeProxy().onMachineTick();
        } else {
            terminal.getNodeProxy().disconnect();
        }
        terminal.onMachineTick(Phase.END);
        HyperNetFixLifecycle.markDirty(terminal);
    }
}
