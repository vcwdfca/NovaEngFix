package com.vcwdfca.novaengfix.mixin.hypernet;

import github.kasuminova.novaeng.common.hypernet.old.ComputationCenter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ComputationCenter.class, remap = false)
public interface InvokerComputationCenterCheckNodeConnection {
    @Invoker(value = "checkNodeConnection", remap = false)
    void novaengfix$invokeCheckNodeConnection();
}
