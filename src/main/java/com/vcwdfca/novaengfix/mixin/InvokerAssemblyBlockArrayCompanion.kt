package com.vcwdfca.novaengfix.mixin

import github.kasuminova.novaeng.common.util.AssemblyBlockArray
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(value = [AssemblyBlockArray.Companion::class], remap = false)
interface InvokerAssemblyBlockArrayCompanion {

    @Invoker("getFluidHandlerItems")
    fun invokeGetFluidHandlerItems(inventory: List<ItemStack>): List<*>

    @Invoker("consumeInventoryFluid")
    fun invokeConsumeInventoryFluid(required: FluidStack,
                                    fluidHandlers: List<*>,
                                    player: InventoryPlayer?
    ): Boolean
}
