package com.vcwdfca.novaengfix.mixin.hypernet;

import github.kasuminova.novaeng.common.hypernet.old.NetNode;
import github.kasuminova.novaeng.common.hypernet.old.NetNodeCache;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = NetNodeCache.class, remap = false)
public interface AccessorNetNodeCache {
    @Accessor(value = "CACHED_NODES", remap = false)
    static Map<TileMultiblockMachineController, NetNode> novaengfix$getCachedNodes() {
        return null;
    }
}
