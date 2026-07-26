package com.vcwdfca.novaengfix.mixin;

import github.kasuminova.novaeng.common.hypernet.old.Database;
import github.kasuminova.novaeng.common.hypernet.old.NetNode;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Database.class, remap = false)
public class MixinDatabase extends NetNode {

    public MixinDatabase(TileMultiblockMachineController owner) {
        super(owner);
    }

    @Inject(method = "writeNBT", at = @At("TAIL"))
    private void addMarkDirty(CallbackInfo ci) {
        owner.markDirty();
    }
}
