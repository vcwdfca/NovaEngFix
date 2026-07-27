package com.vcwdfca.novaengfix.mixin.hypernet;

import com.vcwdfca.novaengfix.hypernet.HyperNetFixLifecycle;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = TileMultiblockMachineController.class, remap = false)
public abstract class MixinTileMultiblockMachineController {

    @Shadow(remap = false)
    public abstract NBTTagCompound getCustomDataTag();

    @Unique
    private static final String[] NOVAENGFIX_PERSISTED_KEYS = {
        "centerPos",
        "storedResearchCognition",
        "researchingCognition",
        "researching",
        "completedPoints",
        "taskProvider",
        "consumption",
        "circuitDurability",
        "networkOwner",
        "storedHU",
        "overheat",
        "computationalLoad",
        "maxGeneration",
        "c",
        "overclocking",
        "cardInventory",
        "controllerStatus"
    };

    @Unique
    private Map<String, NBTBase> novaengfix$resetBackup;

    @Inject(method = "resetMachine(Z)V", at = @At("HEAD"), remap = false)
    private void novaengfix$prepareReset(boolean clearData, CallbackInfo ci) {
        HyperNetFixLifecycle.disconnectController((TileMultiblockMachineController) (Object) this);
        if (!clearData) {
            return;
        }

        NBTTagCompound tag = getCustomDataTag();
        Map<String, NBTBase> backup = new HashMap<>();
        for (String key : NOVAENGFIX_PERSISTED_KEYS) {
            if (tag.hasKey(key)) {
                backup.put(key, tag.getTag(key).copy());
            }
        }
        novaengfix$resetBackup = backup;
    }

    @Inject(method = "resetMachine(Z)V", at = @At("RETURN"), remap = false)
    private void novaengfix$restoreResetData(boolean clearData, CallbackInfo ci) {
        if (novaengfix$resetBackup == null) {
            return;
        }

        TileMultiblockMachineController controller = (TileMultiblockMachineController) (Object) this;
        NBTTagCompound tag = getCustomDataTag();
        for (Map.Entry<String, NBTBase> entry : novaengfix$resetBackup.entrySet()) {
            tag.setTag(entry.getKey(), entry.getValue().copy());
        }
        novaengfix$resetBackup = null;
        HyperNetFixLifecycle.markDirty(controller);
    }

    @Inject(method = "invalidate()V", at = @At("HEAD"), remap = false)
    private void novaengfix$disconnectOnInvalidate(CallbackInfo ci) {
        HyperNetFixLifecycle.disconnectController((TileMultiblockMachineController) (Object) this);
    }

    @Inject(method = "onChunkUnload()V", at = @At("HEAD"), remap = false)
    private void novaengfix$disconnectOnChunkUnload(CallbackInfo ci) {
        HyperNetFixLifecycle.disconnectController((TileMultiblockMachineController) (Object) this);
    }
}
