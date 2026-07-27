package com.vcwdfca.novaengfix.mixin.hypernet;

import com.vcwdfca.novaengfix.hypernet.HyperNetFixLifecycle;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import net.minecraft.command.ICommandSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RegistryHyperNet.class, remap = false)
public abstract class MixinRegistryHyperNet {
    @Inject(method = "clearRegistry()V", at = @At("HEAD"), remap = false)
    private static void novaengfix$beginReload(CallbackInfo ci) {
        HyperNetFixLifecycle.beginReload();
    }

    @Inject(method = "clearRegistry(Lnet/minecraft/command/ICommandSender;)V", at = @At("HEAD"), remap = false)
    private static void novaengfix$beginReloadWithSender(ICommandSender sender, CallbackInfo ci) {
        HyperNetFixLifecycle.beginReload();
    }
}
