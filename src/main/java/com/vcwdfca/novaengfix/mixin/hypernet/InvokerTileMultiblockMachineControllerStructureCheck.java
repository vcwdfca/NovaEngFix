package com.vcwdfca.novaengfix.mixin.hypernet;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = TileMultiblockMachineController.class, remap = false)
public interface InvokerTileMultiblockMachineControllerStructureCheck {
    @Invoker(value = "doStructureCheck", remap = false)
    boolean novaengfix$invokeDoStructureCheck();
}
