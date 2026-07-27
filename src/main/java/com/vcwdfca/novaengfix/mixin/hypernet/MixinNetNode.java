package com.vcwdfca.novaengfix.mixin.hypernet;

import com.vcwdfca.novaengfix.hypernet.HyperNetFixLifecycle;
import github.kasuminova.novaeng.common.hypernet.old.ComputationCenter;
import github.kasuminova.novaeng.common.hypernet.old.NetNode;
import github.kasuminova.novaeng.common.hypernet.old.misc.ConnectResult;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NetNode.class, remap = false)
public abstract class MixinNetNode {
    @Shadow(remap = false)
    protected ComputationCenter center;

    @Shadow(remap = false)
    protected BlockPos centerPos;

    @Shadow(remap = false)
    protected abstract ConnectResult connectToCenter();

    @Shadow(remap = false)
    public abstract void disconnect();

    @Shadow(remap = false)
    public abstract TileMultiblockMachineController getOwner();

    @Inject(method = "<init>(Lhellfirepvp/modularmachinery/common/tiles/base/TileMultiblockMachineController;)V", at = @At("RETURN"), remap = false)
    private void novaengfix$track(TileMultiblockMachineController owner, CallbackInfo ci) {
        HyperNetFixLifecycle.track((NetNode) (Object) this);
    }

    @Inject(method = "onMachineTick()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void novaengfix$reconnectConfiguredNode(CallbackInfo ci) {
        NetNode node = (NetNode) (Object) this;
        if (!HyperNetFixLifecycle.canTick(node)) {
            ci.cancel();
            return;
        }

        TileMultiblockMachineController owner = getOwner();
        if (owner.getTicksExisted() % 20 != 0) {
            ci.cancel();
            return;
        }

        if (center != null) {
            disconnect();
        }
        if (centerPos != null) {
            connectToCenter();
        }
        ci.cancel();
    }

    @Inject(method = "writeNBT()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void novaengfix$pauseWriteDuringReload(CallbackInfo ci) {
        if (HyperNetFixLifecycle.isReloading()) {
            ci.cancel();
        } else if (!HyperNetFixLifecycle.isServerThread(getOwner())) {
            HyperNetFixLifecycle.scheduleNodeWrite((NetNode) (Object) this);
            ci.cancel();
        }
    }

    @Inject(method = "writeNBT()V", at = @At("RETURN"), remap = false)
    private void novaengfix$finishWrite(CallbackInfo ci) {
        if (centerPos == null) {
            getOwner().getCustomDataTag().removeTag("centerPos");
        }
        HyperNetFixLifecycle.markDirty(getOwner());
    }

    @Inject(method = "writeNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void novaengfix$pauseTagWriteDuringReload(NBTTagCompound tag, CallbackInfo ci) {
        if (HyperNetFixLifecycle.isReloading()) {
            ci.cancel();
        }
    }

    @Inject(method = "writeNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("RETURN"), remap = false)
    private void novaengfix$finishTagWrite(NBTTagCompound tag, CallbackInfo ci) {
        if (centerPos == null) {
            tag.removeTag("centerPos");
        }
        HyperNetFixLifecycle.markDirty(getOwner());
    }

    @Inject(method = "isConnected()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void novaengfix$connectedMeansRegistered(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(center != null);
    }
}
