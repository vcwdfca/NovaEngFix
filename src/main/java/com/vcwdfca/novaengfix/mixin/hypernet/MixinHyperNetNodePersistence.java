package com.vcwdfca.novaengfix.mixin.hypernet;

import com.vcwdfca.novaengfix.hypernet.HyperNetFixLifecycle;
import github.kasuminova.novaeng.common.hypernet.old.DataProcessor;
import github.kasuminova.novaeng.common.hypernet.old.NetNode;
import github.kasuminova.novaeng.common.hypernet.old.NetNodeImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {DataProcessor.class, NetNodeImpl.class}, remap = false)
public abstract class MixinHyperNetNodePersistence {
    @Inject(method = "writeNBT()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void novaengfix$writeOnServerThread(CallbackInfo ci) {
        NetNode node = (NetNode) (Object) this;
        if (HyperNetFixLifecycle.isReloading()) {
            ci.cancel();
        } else if (!HyperNetFixLifecycle.isServerThread(node.getOwner())) {
            HyperNetFixLifecycle.scheduleNodeWrite(node);
            ci.cancel();
        }
    }
}
