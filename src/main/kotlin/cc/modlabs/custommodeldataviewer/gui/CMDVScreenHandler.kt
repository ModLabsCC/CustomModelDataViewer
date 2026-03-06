package cc.modlabs.custommodeldataviewer.gui

import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.core.NonNullList
import net.minecraft.util.Mth
import kotlin.math.max

class CMDVScreenHandler(player: LocalPlayer) : AbstractContainerMenu(null, 0) {
    val itemList: NonNullList<ItemStack> = NonNullList.create()
    private val parent: AbstractContainerMenu = player.inventoryMenu

    init {
        val playerInventory = player.inventory

        for (i in 0..4) {
            for (j in 0..8) {
                addSlot(LockableSlot(CMDVScreen.INVENTORY, i * 9 + j, 9 + j * 18, 18 + i * 18))
            }
        }

        addInventoryHotbarSlots(playerInventory, 9, 112)
        scrollItems(0.0f)
    }

    override fun stillValid(player: Player): Boolean = true

    protected val overflowRows: Int
        get() = Mth.positiveCeilDiv(itemList.size, 9) - 5

    fun getRow(scroll: Float): Int = max(((scroll * overflowRows.toFloat()) + 0.5f).toInt(), 0)

    fun getScrollPosition(row: Int): Float = Mth.clamp(row.toFloat() / overflowRows.toFloat(), 0.0f, 1.0f)

    fun getScrollPosition(current: Float, amount: Double): Float =
        Mth.clamp(current - (amount / overflowRows.toDouble()).toFloat(), 0.0f, 1.0f)

    fun scrollItems(position: Float) {
        val row = getRow(position)

        for (j in 0..4) {
            for (k in 0..8) {
                val idx = k + (j + row) * 9
                if (idx >= 0 && idx < itemList.size) {
                    CMDVScreen.INVENTORY.setItem(k + j * 9, itemList[idx])
                } else {
                    CMDVScreen.INVENTORY.setItem(k + j * 9, ItemStack.EMPTY)
                }
            }
        }
    }

    fun shouldShowScrollbar(): Boolean = itemList.size > 45

    override fun quickMoveStack(player: Player, slot: Int): ItemStack {
        if (slot >= slots.size - 9 && slot < slots.size) {
            val target = slots[slot]
            if (target.hasItem()) {
                target.setByPlayer(ItemStack.EMPTY)
            }
        }

        return ItemStack.EMPTY
    }

    override fun canTakeItemForPickAll(stack: ItemStack, slot: Slot): Boolean {
        return slot.container !== CMDVScreen.INVENTORY
    }

    override fun canDragTo(slot: Slot): Boolean {
        return slot.container !== CMDVScreen.INVENTORY
    }

    override fun getCarried(): ItemStack = parent.carried

    override fun setCarried(stack: ItemStack) {
        parent.setCarried(stack)
    }
}
