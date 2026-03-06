package cc.modlabs.custommodeldataviewer.gui

import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.inventory.Slot

class LockableSlot(inventory: Container, i: Int, j: Int, k: Int) : Slot(inventory, i, j, k) {
    override fun mayPickup(playerEntity: Player): Boolean {
        val itemStack = this.item
        return if (super.mayPickup(playerEntity) && !itemStack.isEmpty()) {
            !itemStack.has(DataComponents.CREATIVE_SLOT_LOCK)
        } else {
            itemStack.isEmpty()
        }
    }
}
