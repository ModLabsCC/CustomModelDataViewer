package cc.modlabs.custommodeldataviewer.gui

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.Slot
import net.minecraft.resources.Identifier

class CreativeSlot(val slot: Slot, invSlot: Int, x: Int, y: Int) : Slot(slot.container, invSlot, x, y) {
    override fun onTake(player: Player, stack: ItemStack) {
        slot.onTake(player, stack)
    }

    override fun mayPlace(stack: ItemStack): Boolean = slot.mayPlace(stack)

    override fun getItem(): ItemStack = slot.item

    override fun hasItem(): Boolean = slot.hasItem()

    override fun setByPlayer(stack: ItemStack, previousStack: ItemStack) {
        slot.setByPlayer(stack, previousStack)
    }

    override fun set(stack: ItemStack) {
        slot.set(stack)
    }

    override fun setChanged() {
        slot.setChanged()
    }

    override fun getMaxStackSize(): Int = slot.maxStackSize

    override fun getMaxStackSize(stack: ItemStack): Int = slot.getMaxStackSize(stack)

    override fun getNoItemIcon(): Identifier? = slot.noItemIcon

    override fun remove(amount: Int): ItemStack = slot.remove(amount)

    override fun isActive(): Boolean = slot.isActive

    override fun mayPickup(playerEntity: Player): Boolean = slot.mayPickup(playerEntity)
}
