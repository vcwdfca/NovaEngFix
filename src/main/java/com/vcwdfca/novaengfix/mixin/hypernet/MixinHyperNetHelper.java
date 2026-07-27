package com.vcwdfca.novaengfix.mixin.hypernet;

import github.kasuminova.mmce.common.helper.IMachineController;
import github.kasuminova.novaeng.common.crafttweaker.hypernet.HyperNetHelper;
import github.kasuminova.novaeng.common.hypernet.old.misc.HyperNetConnectCardInfo;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = HyperNetHelper.class, remap = false)
public abstract class MixinHyperNetHelper {
    @Inject(method = "readConnectCardInfo(Lgithub/kasuminova/mmce/common/helper/IMachineController;Lnet/minecraft/item/ItemStack;)Lgithub/kasuminova/novaeng/common/hypernet/old/misc/HyperNetConnectCardInfo;", at = @At("HEAD"), cancellable = true, remap = false)
    private static void novaengfix$keepUnloadedTarget(IMachineController controller,
                                                       ItemStack stack,
                                                       CallbackInfoReturnable<HyperNetConnectCardInfo> cir) {
        if (stack == null || stack.isEmpty()) {
            cir.setReturnValue(null);
            return;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("pos") || !tag.hasKey("owner")) {
            cir.setReturnValue(null);
            return;
        }

        final UUID networkOwner;
        try {
            networkOwner = UUID.fromString(tag.getString("owner"));
        } catch (IllegalArgumentException ignored) {
            cir.setReturnValue(null);
            return;
        }

        BlockPos pos = BlockPos.fromLong(tag.getLong("pos"));
        TileMultiblockMachineController source = controller == null ? null : controller.getController();
        World world = source == null ? null : source.getWorld();
        if (world == null) {
            cir.setReturnValue(null);
            return;
        }

        if (!world.isBlockLoaded(pos)) {
            cir.setReturnValue(new HyperNetConnectCardInfo(pos, networkOwner));
            return;
        }

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileMultiblockMachineController)) {
            cir.setReturnValue(null);
            return;
        }
        TileMultiblockMachineController center = (TileMultiblockMachineController) te;
        if (!HyperNetHelper.isComputationCenter(center)) {
            cir.setReturnValue(null);
            return;
        }
        cir.setReturnValue(new HyperNetConnectCardInfo(pos, networkOwner));
    }
}
