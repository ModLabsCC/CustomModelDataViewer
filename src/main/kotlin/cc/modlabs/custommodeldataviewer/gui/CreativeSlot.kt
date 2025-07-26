package cc.modlabs.custommodeldataviewer.gui

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier

class CreativeSlot(val slot: Slot, invSlot: Int, x: Int, y: Int) : Slot(
    slot.inventory, invSlot, x, y
) {
    override fun onTakeItem(player: PlayerEntity?, stack: ItemStack?) {
        this.slot.onTakeItem(player, stack)
    }

    override fun canInsert(stack: ItemStack?): Boolean {
        return this.slot.canInsert(stack)
    }

    override fun getStack(): ItemStack {
        return this.slot.stack
    }

    override fun hasStack(): Boolean {
        return this.slot.hasStack()
    }

    override fun setStack(stack: ItemStack?, previousStack: ItemStack?) {
        this.slot.setStack(stack, previousStack)
    }

    override fun setStackNoCallbacks(stack: ItemStack?) {
        this.slot.setStackNoCallbacks(stack)
    }

    override fun markDirty() {
        this.slot.markDirty()
    }

    override fun getMaxItemCount(): Int {
        return this.slot.maxItemCount
    }

    override fun getMaxItemCount(stack: ItemStack?): Int {
        return this.slot.getMaxItemCount(stack)
    }

    override fun getBackgroundSprite(): Identifier? {
        return this.slot.backgroundSprite
    }

    override fun takeStack(amount: Int): ItemStack? {
        return this.slot.takeStack(amount)
    }

    override fun isEnabled(): Boolean {
        return this.slot.isEnabled
    }

    override fun canTakeItems(playerEntity: PlayerEntity?): Boolean {
        return this.slot.canTakeItems(playerEntity)
    }
}