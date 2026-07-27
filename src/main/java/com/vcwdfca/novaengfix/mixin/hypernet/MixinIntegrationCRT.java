package com.vcwdfca.novaengfix.mixin.hypernet;

import com.vcwdfca.novaengfix.hypernet.HyperNetFixLifecycle;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import youyihj.zenutils.api.reload.ScriptReloadEvent;
import github.kasuminova.novaeng.common.integration.IntegrationCRT;

@Mixin(value = IntegrationCRT.class, remap = false)
public abstract class MixinIntegrationCRT {
    @Inject(method = "onScriptsReloading(Lyouyihj/zenutils/api/reload/ScriptReloadEvent$Pre;)V", at = @At("HEAD"), remap = false)
    private void novaengfix$beginReload(ScriptReloadEvent.Pre event, CallbackInfo ci) {
        HyperNetFixLifecycle.beginReload();
    }

    @WrapMethod(method = "onScriptsReloadedPre(Lyouyihj/zenutils/api/reload/ScriptReloadEvent$Post;)V", remap = false)
    private void novaengfix$wrapReloadedPre(ScriptReloadEvent.Post event, Operation<Void> original) {
        try {
            original.call(event);
        } catch (RuntimeException | Error failure) {
            HyperNetFixLifecycle.finishReload();
            throw failure;
        }
    }

    // Keep the lifecycle paused through the HIGH and LOW initialization callbacks.
    // The finally block also handles failures in either the handler registration or
    // a machine-specific post initializer.
    @WrapMethod(method = "onScriptsReloadedPost(Lyouyihj/zenutils/api/reload/ScriptReloadEvent$Post;)V", remap = false)
    private void novaengfix$wrapReloadedPost(ScriptReloadEvent.Post event, Operation<Void> original) {
        try {
            original.call(event);
        } finally {
            HyperNetFixLifecycle.finishReload();
        }
    }
}
