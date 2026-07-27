package com.vcwdfca.novaengfix.mixin.hypernet;

import com.vcwdfca.novaengfix.hypernet.HyperNetFixLifecycle;
import github.kasuminova.novaeng.common.hypernet.old.NetNode;
import github.kasuminova.novaeng.common.hypernet.old.NetNodeCache;
import github.kasuminova.novaeng.common.tile.TileHyperNetTerminal;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = NetNodeCache.class, remap = false)
public abstract class MixinNetNodeCache {
    @Shadow(remap = false)
    @Final
    private static Map<TileMultiblockMachineController, NetNode> CACHED_NODES;

    @Inject(method = "getCache", at = @At("HEAD"), cancellable = true, remap = false)
    private static void novaengfix$guardCacheCreation(TileMultiblockMachineController ctrl,
                                                       Class<?> type,
                                                       CallbackInfoReturnable<NetNode> cir) {
        if (type == null || !HyperNetFixLifecycle.canCreateCache(ctrl)) {
            cir.setReturnValue(null);
            return;
        }

        NetNode cached = CACHED_NODES.get(ctrl);
        if (cached != null && !type.isInstance(cached)) {
            novaengfix$removeCachedNode(ctrl, cached);
        }

        if (ctrl instanceof TileHyperNetTerminal) {
            if (cached != null) {
                novaengfix$removeCachedNode(ctrl, cached);
            }
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "removeCache(Lhellfirepvp/modularmachinery/common/tiles/base/TileMultiblockMachineController;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void novaengfix$disconnectBeforeRemoval(TileMultiblockMachineController controller, CallbackInfo ci) {
        if (controller != null) {
            NetNode node = CACHED_NODES.get(controller);
            if (node != null) {
                novaengfix$removeCachedNode(controller, node);
            }
        }
        ci.cancel();
    }

    @Unique
    private static void novaengfix$removeCachedNode(TileMultiblockMachineController controller, NetNode node) {
        try {
            node.disconnect();
        } catch (RuntimeException ignored) {
            // Removal must still happen when a stale owner is already invalid.
        } finally {
            CACHED_NODES.remove(controller, node);
            HyperNetFixLifecycle.untrack(node);
        }
    }
}
