package cc.modlabs.custommodeldataviewer.gui

import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.MathHelper
import kotlin.math.max

class CMDVScreenHandler(player: ClientPlayerEntity) : ScreenHandler(null, 0) {
    val itemList: DefaultedList<ItemStack?> = DefaultedList.of<ItemStack?>()
    private val parent: ScreenHandler = player.playerScreenHandler

    init {
        val playerInventory = player.getInventory()

        for (i in 0..4) {
            for (j in 0..8) {
                this.addSlot(LockableSlot(CMDVScreen.INVENTORY, i * 9 + j, 9 + j * 18, 18 + i * 18))
            }
        }

        this.addPlayerHotbarSlots(playerInventory, 9, 112)
        this.scrollItems(0.0f)
    }

    override fun canUse(player: PlayerEntity?): Boolean {
        return true
    }

    protected val overflowRows: Int
        get() = MathHelper.ceilDiv(this.itemList.size, 9) - 5

    fun getRow(scroll: Float): Int {
        return max(((scroll * this.overflowRows.toFloat()).toDouble() + 0.5).toInt(), 0)
    }

    fun getScrollPosition(row: Int): Float {
        return MathHelper.clamp(row.toFloat() / this.overflowRows.toFloat(), 0.0f, 1.0f)
    }

    fun getScrollPosition(current: Float, amount: Double): Float {
        return MathHelper.clamp(current - (amount / this.overflowRows.toDouble()).toFloat(), 0.0f, 1.0f)
    }

    fun scrollItems(position: Float) {
        val i = this.getRow(position)

        for (j in 0..4) {
            for (k in 0..8) {
                val l = k + (j + i) * 9
                if (l >= 0 && l < this.itemList.size) {
                    CMDVScreen.INVENTORY.setStack(k + j * 9, this.itemList.get(l))
                } else {
                    CMDVScreen.INVENTORY.setStack(k + j * 9, ItemStack.EMPTY)
                }
            }
        }
    }

    fun shouldShowScrollbar(): Boolean {
        return this.itemList.size > 45
    }

    override fun quickMove(player: PlayerEntity?, slot: Int): ItemStack {
        if (slot >= this.slots.size - 9 && slot < this.slots.size) {
            val slot2 = this.slots[slot]
            if (slot2.hasStack()) {
                slot2.stack = ItemStack.EMPTY
            }
        }

        return ItemStack.EMPTY
    }

    override fun canInsertIntoSlot(stack: ItemStack?, slot: Slot): Boolean {
        return slot.inventory !== CMDVScreen.INVENTORY
    }

    override fun canInsertIntoSlot(slot: Slot): Boolean {
        return slot.inventory !== CMDVScreen.INVENTORY
    }

    override fun getCursorStack(): ItemStack? {
        return this.parent.cursorStack
    }

    override fun setCursorStack(stack: ItemStack?) {
        this.parent.cursorStack = stack
    }
}