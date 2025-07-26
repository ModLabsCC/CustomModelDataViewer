package cc.modlabs.custommodeldataviewer.gui

import net.minecraft.client.MinecraftClient
//? if >=1.21.8 {
import net.minecraft.client.gl.RenderPipelines
//?}
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.CreativeInventoryListener
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemGroups
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenTexts
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import java.util.function.Function

class CMDVScreen(
    private val allItems: List<ItemStack>
) : HandledScreen<CMDVScreenHandler>(
    CMDVScreenHandler(MinecraftClient.getInstance().player!!),
    MinecraftClient.getInstance().player!!.inventory,
    ScreenTexts.EMPTY
) {

    private val BACKGROUND_TEXTURE = Identifier.of("minecraft", "textures/gui/container/creative_inventory/tab_item_search.png")
    private val SCROLLER_TEXTURE: Identifier = Identifier.ofVanilla("container/creative_inventory/scroller")
    private val SCROLLER_DISABLED_TEXTURE: Identifier = Identifier.ofVanilla("container/creative_inventory/scroller_disabled")

    private var scrollPosition = 0f
    private var searchBox: TextFieldWidget? = null
    private var listener: CreativeInventoryListener? = null
    private var deleteItemSlot: Slot? = null
    private var lastClickOutsideBounds = false
    private var scrolling = false
    private var ignoreTypedCharacter = false
    
    val selectedTabType: ItemGroup.Type = ItemGroup.Type.SEARCH

    init {
        backgroundHeight = 136
        backgroundWidth = 195
        MinecraftClient.getInstance().player!!.currentScreenHandler = handler
        handler.itemList.addAll(allItems)
    }

    companion object {
        val INVENTORY: SimpleInventory = SimpleInventory(45)
    }

    override fun init() {
        super.init()
        searchBox = TextFieldWidget(MinecraftClient.getInstance().textRenderer, x + 82,  y + 6, 80, 9, Text.translatable("itemGroup.search"))
        searchBox!!.setMaxLength(50)
        searchBox!!.setDrawsBackground(false)
        searchBox!!.isVisible = true
        searchBox!!.setFocusUnlocked(false)
        searchBox!!.isFocused = true
        searchBox!!.text = ""
        //? if >=1.21.8 {
        searchBox!!.setEditableColor(-1)
        //?} else {
        /*searchBox!!.setEditableColor(16777215)
        *///?}

        addSelectableChild(searchBox)
        client!!.player!!.playerScreenHandler.removeListener(listener)
        listener = CreativeInventoryListener(client)
        client!!.player!!.playerScreenHandler.addListener(listener)
        search()
    }

    override fun resize(client: MinecraftClient, width: Int, height: Int) {
        val i = handler.getRow(scrollPosition)
        val string = searchBox!!.text
        init(client, width, height)
        searchBox!!.setText(string)
        if (searchBox!!.text.isNotEmpty()) {
            search()
        }

        scrollPosition = handler.getScrollPosition(i)
        handler.scrollItems(scrollPosition)
    }

    private fun search() {
        handler.itemList.clear()
        val query = searchBox!!.text
        if (query.isEmpty()) {
            handler.itemList.addAll(allItems)
        } else {
            handler.itemList.addAll(allItems.filter {
                it.name.string.contains(query, ignoreCase = true)
            })
        }
        scrollPosition = 0.0f
        handler.scrollItems(0.0f)
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        return if (this.ignoreTypedCharacter) {
            false
        } else {
            val string = this.searchBox!!.text
            if (this.searchBox!!.charTyped(chr, modifiers)) {
                if (string != this.searchBox!!.text) {
                    this.search()
                }

                true
            } else {
                false
            }
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        this.ignoreTypedCharacter = false
        val bl = !this.isCreativeInventorySlot(this.focusedSlot) || this.focusedSlot!!.hasStack()
        val bl2 = InputUtil.fromKeyCode(keyCode, scanCode).toInt().isPresent
        return if (bl && bl2 && this.handleHotbarKeyPressed(keyCode, scanCode)) {
            this.ignoreTypedCharacter = true
            true
        } else {
            val string = this.searchBox!!.text
            if (this.searchBox!!.keyPressed(keyCode, scanCode, modifiers)) {
                if (string != this.searchBox!!.text) {
                    this.search()
                }

                true
            } else {
                (this.searchBox!!.isFocused && this.searchBox!!.isVisible && keyCode != 256) || super.keyPressed(
                    keyCode,
                    scanCode,
                    modifiers
                )
            }
        }
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        this.ignoreTypedCharacter = false
        return super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        return if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            true
        } else {
            scrollPosition = handler.getScrollPosition(scrollPosition, verticalAmount)
            handler.scrollItems(scrollPosition)
            true
        }
    }

    protected fun isClickInScrollbar(mouseX: Double, mouseY: Double): Boolean {
        val i = x
        val j = y
        val k = i + 175
        val l = j + 18
        val m = k + 14
        val n = l + 112
        return mouseX >= k.toDouble() && mouseY >= l.toDouble() && mouseX < m.toDouble() && mouseY < n.toDouble()
    }

    private fun getTabX(group: ItemGroup): Int {
        val i = group.column
        var k = 27 * i
        if (group.isSpecial) {
            k = backgroundWidth - 27 * (7 - i) + 1
        }

        return k
    }

    private fun getTabY(group: ItemGroup): Int {
        var i = 0
        if (group.row == ItemGroup.Row.TOP) {
            i -= 32
        } else {
            i += backgroundHeight
        }

        return i
    }

    protected fun isClickInTab(group: ItemGroup, mouseX: Double, mouseY: Double): Boolean {
        val i: Int = getTabX(group)
        val j: Int = getTabY(group)
        return mouseX >= i.toDouble() && mouseX <= (i + 26).toDouble() && mouseY >= j.toDouble() && mouseY <= (j + 32).toDouble()
    }

    private fun hasScrollbar(): Boolean {
        return handler.shouldShowScrollbar()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val d = mouseX - x.toDouble()
            val e = mouseY - y.toDouble()

            for (itemGroup in ItemGroups.getGroupsToDisplay()) {
                if (isClickInTab(itemGroup, d, e)) {
                    return true
                }
            }

            if (isClickInScrollbar(
                    mouseX,
                    mouseY
                )
            ) {
                scrolling = hasScrollbar()
                return true
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (scrolling) {
            val i = y + 18
            val j = i + 112
            scrollPosition = (mouseY.toFloat() - i.toFloat() - 7.5f) / ((j - i).toFloat() - 15.0f)
            scrollPosition = MathHelper.clamp(scrollPosition, 0.0f, 1.0f)
            handler.scrollItems(scrollPosition)
            return true
        } else {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        }
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            scrolling = false
        }

        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun onMouseClick(slot: Slot?, slotId: Int, button: Int, actionType: SlotActionType?) {
        var actionType = actionType

        val bl = actionType == SlotActionType.QUICK_MOVE
        actionType = if (slotId == -999 && actionType == SlotActionType.PICKUP) SlotActionType.THROW else actionType
        if (actionType != SlotActionType.THROW || client!!.player!!.canDropItems()) {
            if (slot == null && actionType != SlotActionType.QUICK_CRAFT) {
                if (!handler.cursorStack!!
                        .isEmpty && lastClickOutsideBounds
                ) {
                    if (!client!!.player!!.canDropItems()) {
                        return
                    }

                    if (button == 0) {
                        client!!.player!!.dropItem(handler.cursorStack, true)
                        client!!.interactionManager!!.dropCreativeStack(handler.cursorStack)
                        handler.cursorStack = ItemStack.EMPTY
                    }

                    if (button == 1) {
                        val itemStack = handler.cursorStack!!.split(1)
                        client!!.player!!.dropItem(itemStack, true)
                        client!!.interactionManager!!.dropCreativeStack(itemStack)
                    }
                }
            } else {
                if (slot != null && !slot.canTakeItems(client!!.player)) {
                    return
                }

                if (slot === deleteItemSlot && bl) {
                    for (i in client!!.player!!.playerScreenHandler.stacks.indices) {
                        client!!.player!!.playerScreenHandler.getSlot(i).setStackNoCallbacks(ItemStack.EMPTY)
                        client!!.interactionManager!!.clickCreativeStack(ItemStack.EMPTY, i)
                    }
                } else if (selectedTabType == ItemGroup.Type.INVENTORY) {
                    if (slot === deleteItemSlot) {
                        handler.cursorStack = ItemStack.EMPTY
                    } else if (actionType == SlotActionType.THROW && slot != null && slot.hasStack()) {
                        val itemStack = slot.takeStack(if (button == 0) 1 else slot.stack.maxCount)
                        val itemStack2 = slot.stack
                        client!!.player!!.dropItem(itemStack, true)
                        client!!.interactionManager!!.dropCreativeStack(itemStack)
                        client!!.interactionManager!!.clickCreativeStack(
                            itemStack2,
                            (slot as CreativeSlot).slot.id
                        )
                    } else if (actionType == SlotActionType.THROW && slotId == -999 && !handler.cursorStack!!
                            .isEmpty
                    ) {
                        client!!.player!!.dropItem(handler.cursorStack, true)
                        client!!.interactionManager!!.dropCreativeStack(handler.cursorStack)
                        handler.cursorStack = ItemStack.EMPTY
                    } else {
                        client!!.player!!.playerScreenHandler.onSlotClick(
                            if (slot == null) slotId else (slot as CreativeSlot).slot.id,
                            button,
                            actionType,
                            client!!.player
                        )
                        client!!.player!!.playerScreenHandler.sendContentUpdates()
                    }
                } else if (actionType != SlotActionType.QUICK_CRAFT && slot!!.inventory === INVENTORY) {
                    val itemStack = handler.cursorStack
                    val itemStack2 = slot.stack
                    if (actionType == SlotActionType.SWAP) {
                        if (!itemStack2.isEmpty) {
                            client!!.player!!.getInventory()
                                .setStack(button, itemStack2.copyWithCount(itemStack2.getMaxCount()))
                            client!!.player!!.playerScreenHandler.sendContentUpdates()
                        }

                        return
                    }

                    if (actionType == SlotActionType.CLONE) {
                        if (handler.cursorStack!!.isEmpty && slot.hasStack()) {
                            val itemStack3 = slot.stack
                            handler.cursorStack = itemStack3.copyWithCount(itemStack3.maxCount)
                        }

                        return
                    }

                    if (actionType == SlotActionType.THROW) {
                        if (!itemStack2.isEmpty) {
                            val itemStack3 = itemStack2.copyWithCount(if (button == 0) 1 else itemStack2.maxCount)
                            client!!.player!!.dropItem(itemStack3, true)
                            client!!.interactionManager!!.dropCreativeStack(itemStack3)
                        }

                        return
                    }

                    if (!itemStack!!.isEmpty && !itemStack2.isEmpty && ItemStack.areItemsAndComponentsEqual(
                            itemStack,
                            itemStack2
                        )
                    ) {
                        if (button == 0) {
                            if (bl) {
                                itemStack.count = itemStack.maxCount
                            } else if (itemStack.count < itemStack.maxCount) {
                                itemStack.increment(1)
                            }
                        } else {
                            itemStack.decrement(1)
                        }
                    } else if (!itemStack2.isEmpty && itemStack.isEmpty) {
                        val j = if (bl) itemStack2.maxCount else itemStack2.count
                        handler.cursorStack = itemStack2.copyWithCount(j)
                    } else if (button == 0) {
                        handler.cursorStack = ItemStack.EMPTY
                    } else if (!handler.cursorStack!!.isEmpty) {
                        handler.cursorStack!!.decrement(1)
                    }
                } else if (handler != null) {
                    val itemStack =
                        if (slot == null) ItemStack.EMPTY else handler.getSlot(slot.id)
                            .stack
                    handler.onSlotClick(
                        slot?.id ?: slotId,
                        button,
                        actionType,
                        client!!.player
                    )
                    if (ScreenHandler.unpackQuickCraftStage(button) == 2) {
                        for (k in 0..8) {
                            client!!.interactionManager!!.clickCreativeStack(
                                handler.getSlot(
                                    45 + k
                                ).stack, 36 + k
                            )
                        }
                    } else if (slot != null && PlayerInventory.isValidHotbarIndex(slot.index)) {
                        if (actionType == SlotActionType.THROW && !itemStack.isEmpty && !handler.cursorStack!!.isEmpty
                        ) {
                            val k = if (button == 0) 1 else itemStack.getCount()
                            val itemStack3 = itemStack.copyWithCount(k)
                            itemStack.decrement(k)
                            client!!.player!!.dropItem(itemStack3, true)
                            client!!.interactionManager!!.dropCreativeStack(itemStack3)
                        }

                        client!!.player!!.playerScreenHandler.sendContentUpdates()
                    }
                }
            }
        }
    }

    private fun isCreativeInventorySlot(slot: Slot?): Boolean {
        return false
    }

    override fun isClickOutsideBounds(mouseX: Double, mouseY: Double, left: Int, top: Int, button: Int): Boolean {
        val bl = mouseX < left.toDouble() || mouseY < top.toDouble() || mouseX >= (left + backgroundWidth).toDouble() || mouseY >= (top + backgroundHeight).toDouble()
        lastClickOutsideBounds = bl
        return lastClickOutsideBounds
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(
            //? if >=1.21.8 {
            RenderPipelines.GUI_TEXTURED
            //?} else {
            /*{ texture: Identifier? -> RenderLayer.getGuiTextured(texture) }
            *///?}
            ,BACKGROUND_TEXTURE, x, y, 0.0F, 0.0F, backgroundWidth, backgroundHeight, 256, 256)

        searchBox!!.render(context, mouseX, mouseY, delta)

        val i = x + 175
        val j = y + 18
        val k = j + 112
        val identifier =
            if (hasScrollbar()) SCROLLER_TEXTURE else SCROLLER_DISABLED_TEXTURE
        context.drawGuiTexture(
            //? if >=1.21.8 {
            RenderPipelines.GUI_TEXTURED
            //?} else {
            /*{ texture: Identifier? -> RenderLayer.getGuiTextured(texture) }
            *///?}
            ,
            identifier,
            i,
            j + ((k - j - 17).toFloat() * scrollPosition).toInt(),
            12,
            15
        )
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            Text.literal("Custom Models"),
            8,
            6,
            //? if >=1.21.8 {
            -12566464
            //?} else {
            /*4210752
            *///?}
            ,
            false
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }
}