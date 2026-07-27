package com.vcwdfca.novaengfix.mixin.hypernet;

import github.kasuminova.novaeng.common.hypernet.old.ComputationCenter;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = ComputationCenter.class, remap = false)
public interface AccessorComputationCenterCache {
    @Accessor(value = "CACHED_COMPUTATION_CENTER", remap = false)
    static Map<TileMultiblockMachineController, ComputationCenter> novaengfix$getCachedComputationCenters() {
        return null;
    }
}
