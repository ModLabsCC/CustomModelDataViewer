package cc.modlabs.custommodeldataviewer.gui

import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.screen.slot.Slot

class LockableSlot(inventory: Inventory?, i: Int, j: Int, k: Int) : Slot(inventory, i, j, k) {
    override fun canTakeItems(playerEntity: PlayerEntity): Boolean {
        val itemStack = this.stack
        return if (super.canTakeItems(playerEntity) && !itemStack.isEmpty) {
            itemStack.isItemEnabled(playerEntity.world.enabledFeatures) && !itemStack.contains(
                DataComponentTypes.CREATIVE_SLOT_LOCK
            )
        } else {
            itemStack.isEmpty
        }
    }
}