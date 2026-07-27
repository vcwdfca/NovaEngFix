package com.vcwdfca.novaengfix.mixin.hypernet;

import com.vcwdfca.novaengfix.hypernet.HyperNetFixLifecycle;
import github.kasuminova.novaeng.common.hypernet.old.ComputationCenter;
import github.kasuminova.novaeng.common.hypernet.old.NetNode;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(value = ComputationCenter.class, remap = false)
public abstract class MixinComputationCenter {
    @Shadow(remap = false)
    @Final
    private Map<Class<?>, Map<BlockPos, NetNode>> nodes;

    @Shadow(remap = false)
    public abstract TileMultiblockMachineController getOwner();

    @Shadow(remap = false)
    public abstract UUID getNetworkOwner();

    @Inject(method = "onDisconnect(Lhellfirepvp/modularmachinery/common/tiles/base/TileMultiblockMachineController;Lgithub/kasuminova/novaeng/common/hypernet/old/NetNode;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void novaengfix$removeOnlyMatchingNode(TileMultiblockMachineController machinery,
                                                    NetNode node,
                                                    CallbackInfo ci) {
        if (machinery != null && node != null) {
            Map<BlockPos, NetNode> connected = nodes.get(node.getClass());
            if (connected != null) {
                connected.remove(machinery.getPos(), node);
            }
        }
        ci.cancel();
    }

    @Inject(method = "checkNodeConnection()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void novaengfix$checkNodeConnectionByIdentity(CallbackInfo ci) {
        TileMultiblockMachineController centerOwner = getOwner();
        if (!HyperNetFixLifecycle.isServerThread(centerOwner)) {
            HyperNetFixLifecycle.scheduleCenterNodeCheck((ComputationCenter) (Object) this);
            ci.cancel();
            return;
        }

        if (centerOwner.getTicksExisted() % 50 != 0) {
            ci.cancel();
            return;
        }

        World world = centerOwner.getWorld();
        if (!HyperNetFixLifecycle.isWorldActive(world) || centerOwner.isInvalid()) {
            ci.cancel();
            return;
        }

        for (Map<BlockPos, NetNode> connected : nodes.values()) {
            for (Map.Entry<BlockPos, NetNode> entry : connected.entrySet()) {
                BlockPos pos = entry.getKey();
                NetNode node = entry.getValue();
                TileMultiblockMachineController owner = node.getOwner();
                if (owner == null || owner.isInvalid()) {
                    novaengfix$removeNode(connected, pos, node);
                    continue;
                }

                // An unloaded chunk is not evidence that a node disappeared. The node's
                // own unload hook handles normal unloads; validate the tile only once the
                // chunk is available again.
                if (!world.isBlockLoaded(pos)) {
                    continue;
                }

                TileEntity tile = world.getTileEntity(pos);
                if (tile != owner) {
                    novaengfix$removeNode(connected, pos, node);
                }
            }
        }
        ci.cancel();
    }

    @Inject(method = "writeNBT()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void novaengfix$pauseWriteDuringReload(CallbackInfo ci) {
        if (HyperNetFixLifecycle.isReloading()) {
            ci.cancel();
        } else if (!HyperNetFixLifecycle.isServerThread(getOwner())) {
            HyperNetFixLifecycle.scheduleCenterWrite((ComputationCenter) (Object) this);
            ci.cancel();
        }
    }

    @Inject(method = "writeNBT()V", at = @At("RETURN"), remap = false)
    private void novaengfix$markCenterDirty(CallbackInfo ci) {
        NBTTagCompound tag = getOwner().getCustomDataTag();
        if (getNetworkOwner() == null) {
            tag.removeTag("networkOwner");
        }
        HyperNetFixLifecycle.markDirty(getOwner());
    }

    @Unique
    private void novaengfix$removeNode(Map<BlockPos, NetNode> connected, BlockPos pos, NetNode node) {
        if (!connected.remove(pos, node)) {
            return;
        }
        try {
            node.disconnect();
        } catch (RuntimeException ignored) {
            // The center entry is already removed; stale owners must not stop cleanup.
        }
        HyperNetFixLifecycle.untrack(node);
    }
}
