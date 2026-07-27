package com.vcwdfca.novaengfix.mixin

import appeng.api.AEApi
import appeng.api.config.Actionable
import appeng.api.networking.storage.IStorageGrid
import appeng.api.storage.IMEMonitor
import appeng.api.storage.channels.IFluidStorageChannel
import appeng.api.storage.channels.IItemStorageChannel
import appeng.api.storage.data.IAEFluidStack
import appeng.api.storage.data.IAEItemStack
import appeng.helpers.WirelessTerminalGuiObject
import appeng.me.helpers.PlayerSource
import com.circulation.random_complement.common.util.MEHandler
import github.kasuminova.novaeng.common.util.AssemblyBlockArray
import github.kasuminova.novaeng.common.util.AssemblyBlockArray.Companion.getMaterialList
import github.kasuminova.novaeng.common.util.AssemblyBlockArray.QueuedPlacement
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager.OperatingStatus
import hellfirepvp.modularmachinery.common.util.BlockArray.BlockInformation
import hellfirepvp.modularmachinery.common.util.MiscUtils
import ink.ikx.mmce.common.assembly.MachineAssembly
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentTranslation
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Overwrite
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique

@Mixin(value = [AssemblyBlockArray::class], remap = false)
abstract class MixinAssemblyBlockArray {

    @field:Shadow
    @JvmField
    var ignoreFluids: Boolean = false

    @field:Shadow
    @JvmField
    var usingAE: Boolean = false

    @field:Shadow
    @JvmField
    var missing: Int = 0

    @Shadow
    abstract fun pollPlacement(): QueuedPlacement?

    @Shadow
    abstract fun placeBlock(player: EntityPlayerMP, world: World, pos: BlockPos, state: IBlockState)

    @Shadow
    abstract fun isFluid(state: IBlockState): Boolean

    @Shadow
    abstract fun matchesState(info: BlockInformation, state: IBlockState): Boolean

    /**
     * @author vcwdfca
     * @reason 不会写kt直接重写了
     */
    @Overwrite
    fun assemblyBlock(world: World, player: EntityPlayerMP): OperatingStatus {
        val placement = pollPlacement() ?: return OperatingStatus.COMPLETE
        val pos = placement.pos
        val info = placement.info

        if (player.isCreative) {
            placeBlock(player, world, pos, info.sampleState)
            return OperatingStatus.SUCCESS
        }

        val oldState = world.getBlockState(pos)
        if (oldState != null &&
            (oldState.getBlock() !== Blocks.AIR
                    && !(ignoreFluids && isFluid(oldState)))
        ) {
            return if (matchesState(info, oldState)) {
                OperatingStatus.ALREADY_EXISTS
            } else {
                player.sendMessage(
                    TextComponentTranslation(
                        "message.assembly.tip.cannot_replace",
                        MiscUtils.posToString(pos)
                    )
                )
                OperatingStatus.FAILURE
            }
        }

        val list = getMaterialList(info)

        var hasAE = false
        var wobj: WirelessTerminalGuiObject? = null
        var items: IMEMonitor<IAEItemStack>? = null
        var fluids: IMEMonitor<IAEFluidStack>? = null

        if (usingAE) {
            wobj = MEHandler.getTerminalGuiObject(player)
            wobj?.actionableNode?.grid?.let {
                val grid = it.getCache<IStorageGrid>(IStorageGrid::class.java)
                items = grid.getInventory(
                    AEApi.instance().storage()
                        .getStorageChannel(IItemStorageChannel::class.java)
                )
                fluids = grid.getInventory(
                    AEApi.instance().storage()
                        .getStorageChannel(IFluidStorageChannel::class.java)
                )
                hasAE = true
            }
        }
        val invoker = companionInvoker()
        val itemInventory = player.inventory.mainInventory
        val fluidInventory = invoker.invokeGetFluidHandlerItems(itemInventory)
        for (ingredientAndIBlockState in list.sortedByDescending { it.first.isItem }) {
            val ingredient = ingredientAndIBlockState.first
            if (ingredient.isItem) {
                if (ingredient.itemStack.isEmpty) continue
                if (MachineAssembly.consumeInventoryItem(
                        ingredient.itemStack,
                        itemInventory
                    )
                ) {
                    placeBlock(player, world, pos, ingredientAndIBlockState.getSecond())
                    return OperatingStatus.SUCCESS
                }
            } else {
                if (invoker.invokeConsumeInventoryFluid(
                        ingredient.fluidStack,
                        fluidInventory,
                        player.inventory
                    )
                ) {
                    placeBlock(player, world, pos, ingredientAndIBlockState.getSecond())
                    return OperatingStatus.SUCCESS
                }
            }
        }
        if (hasAE) {
            for (ingredientAndIBlockState in list.sortedByDescending { it.first.isItem }) {
                val ingredient = ingredientAndIBlockState.first
                if (ingredient.isItem) {
                    if (ingredient.itemStack.isEmpty) continue
                    val item = items!!.extractItems(
                        ingredient.aEItemStack,
                        Actionable.MODULATE,
                        PlayerSource(player, wobj)
                    )
                    if (item == null || item.stackSize == 0L) continue
                    placeBlock(player, world, pos, ingredientAndIBlockState.getSecond())
                    return OperatingStatus.SUCCESS
                } else {
                    val fluid = fluids!!.extractItems(
                        ingredient.aEFluidStack,
                        Actionable.SIMULATE,
                        PlayerSource(player, wobj)
                    )
                    if (fluid == null || fluid.stackSize < 1000) continue
                    fluids.extractItems(
                        ingredient.aEFluidStack,
                        Actionable.MODULATE,
                        PlayerSource(player, wobj)
                    )
                    placeBlock(player, world, pos, ingredientAndIBlockState.getSecond())
                    return OperatingStatus.SUCCESS
                }
            }
        }
        if (oldState.block == Blocks.AIR && matchesState(info, oldState)) {
            return OperatingStatus.ALREADY_EXISTS
        }
        if (missing > 0) {
            --missing
            return OperatingStatus.SUCCESS
        }
        player.sendMessage(
            TextComponentTranslation(
                "message.assembly.tip.missing",
                MiscUtils.posToString(pos)
            )
        )
        return OperatingStatus.FAILURE
    }

    @Unique
    private fun companionInvoker(): InvokerAssemblyBlockArrayCompanion {
        val companion: Any = AssemblyBlockArray.Companion
        return companion as InvokerAssemblyBlockArrayCompanion
    }
}