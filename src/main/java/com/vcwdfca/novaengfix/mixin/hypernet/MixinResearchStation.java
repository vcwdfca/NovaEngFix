package com.vcwdfca.novaengfix.mixin.hypernet;

import com.vcwdfca.novaengfix.hypernet.HyperNetFixLifecycle;
import github.kasuminova.novaeng.common.hypernet.old.research.ResearchCognitionData;
import github.kasuminova.novaeng.common.hypernet.old.research.ResearchStation;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = ResearchStation.class, remap = false)
public abstract class MixinResearchStation {
    @Shadow(remap = false)
    private ResearchCognitionData currentResearching;

    @Shadow(remap = false)
    private UUID taskProvider;

    @Unique
    private String novaengfix$unresolvedResearching;

    @Unique
    private double novaengfix$unresolvedCompletedPoints;

    @Unique
    private TileMultiblockMachineController novaengfix$getOwner() {
        return ((ResearchStation) (Object) this).getOwner();
    }

    @Inject(method = "readNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("RETURN"), remap = false)
    private void novaengfix$rememberUnresolvedTask(NBTTagCompound tag, CallbackInfo ci) {
        novaengfix$unresolvedResearching = null;
        novaengfix$unresolvedCompletedPoints = 0D;
        if (!tag.hasKey("taskProvider")) {
            taskProvider = null;
        }

        if (tag.hasKey("researching")) {
            String name = tag.getString("researching");
            if (!name.isEmpty() && RegistryHyperNet.getResearchCognitionData(name) == null) {
                novaengfix$unresolvedResearching = name;
                novaengfix$unresolvedCompletedPoints = tag.getDouble("completedPoints");
                github.kasuminova.novaeng.NovaEngineeringCore.log.warn(
                    "Keeping unresolved HyperNet research task '{}' in station NBT.", name
                );
            }
        }
    }

    @Redirect(method = "readNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At(value = "INVOKE", target = "Ljava/util/UUID;fromString(Ljava/lang/String;)Ljava/util/UUID;"), remap = false)
    private UUID novaengfix$readUuidSafely(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Inject(method = "writeNBT()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void novaengfix$pauseWriteDuringReload(CallbackInfo ci) {
        if (HyperNetFixLifecycle.isReloading()) {
            ci.cancel();
            return;
        }
        if (!HyperNetFixLifecycle.isServerThread(novaengfix$getOwner())) {
            HyperNetFixLifecycle.scheduleNodeWrite((github.kasuminova.novaeng.common.hypernet.old.NetNode) (Object) this);
            ci.cancel();
            return;
        }
        novaengfix$resolveAvailableTask();
    }

    @Inject(method = "writeNBT()V", at = @At("RETURN"), remap = false)
    private void novaengfix$clearOrRestoreTaskFields(CallbackInfo ci) {
        if (HyperNetFixLifecycle.isReloading()) {
            return;
        }

        ResearchStation station = (ResearchStation) (Object) this;
        NBTTagCompound tag = novaengfix$getOwner().getCustomDataTag();
        ResearchCognitionData current = station.getCurrentResearching();
        if (current == null && novaengfix$unresolvedResearching != null) {
            tag.setString("researching", novaengfix$unresolvedResearching);
            tag.setDouble("completedPoints", novaengfix$unresolvedCompletedPoints);
            if (taskProvider == null) {
                tag.removeTag("taskProvider");
            }
        } else if (current == null) {
            tag.removeTag("researching");
            tag.removeTag("completedPoints");
            tag.removeTag("taskProvider");
        } else {
            novaengfix$unresolvedResearching = null;
            if (taskProvider == null) {
                tag.removeTag("taskProvider");
            }
        }
        HyperNetFixLifecycle.markDirty(novaengfix$getOwner());
    }

    @Inject(method = "resetResearchTask()V", at = @At("HEAD"), remap = false)
    private void novaengfix$clearTaskProvider(CallbackInfo ci) {
        novaengfix$unresolvedResearching = null;
        taskProvider = null;
    }

    @Inject(method = "resetResearchTask()V", at = @At("RETURN"), remap = false)
    private void novaengfix$writeResetTask(CallbackInfo ci) {
        if (!HyperNetFixLifecycle.isReloading()) {
            ((ResearchStation) (Object) this).writeNBT();
        }
    }

    @Inject(method = "provideTask(Lgithub/kasuminova/novaeng/common/hypernet/old/research/ResearchCognitionData;Lnet/minecraft/entity/player/EntityPlayer;)V", at = @At("HEAD"), remap = false)
    private void novaengfix$replaceUnresolvedTask(ResearchCognitionData data, EntityPlayer provider, CallbackInfo ci) {
        novaengfix$unresolvedResearching = null;
    }

    @Unique
    private void novaengfix$resolveAvailableTask() {
        if (novaengfix$unresolvedResearching == null) {
            return;
        }
        ResearchCognitionData resolved = RegistryHyperNet.getResearchCognitionData(novaengfix$unresolvedResearching);
        if (resolved != null) {
            currentResearching = resolved;
            novaengfix$unresolvedResearching = null;
        }
    }
}
