package cc.modlabs.custommodeldataviewer.gui

import net.minecraft.client.Minecraft
//? if >=1.21.6 {
import net.minecraft.client.renderer.RenderPipelines
//?}
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
//?}
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.CreativeInventoryListener
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.components.EditBox
//? if <1.21.6 {
/*import net.minecraft.client.render.RenderLayer
*///?}
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.network.chat.CommonComponents
import net.minecraft.world.inventory.Slot
import net.minecraft.world.inventory.ClickType
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth

class CMDVScreen(
    private val allItems: List<ItemStack>
) : AbstractContainerScreen<CMDVScreenHandler>(
    CMDVScreenHandler(requirePlayer()),
    requirePlayer().inventory,
    CommonComponents.EMPTY
) {

    private val BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/creative_inventory/tab_item_search.png")
    private val SCROLLER_TEXTURE: Identifier = Identifier.withDefaultNamespace("container/creative_inventory/scroller")
    private val SCROLLER_DISABLED_TEXTURE: Identifier = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled")

    private var scrollPosition = 0f
    private var searchBox: EditBox? = null
    private var listener: CreativeInventoryListener? = null
    private var deleteItemSlot: Slot? = null
    private var lastClickOutsideBounds = false
    private var scrolling = false
    private var ignoreTypedCharacter = false
    
    val selectedTabType: CreativeModeTab.Type = CreativeModeTab.Type.SEARCH

    init {
        imageHeight = 136
        imageWidth = 195
        requirePlayer().containerMenu = menu
        menu.itemList.addAll(allItems)
    }

    companion object {
        private fun requirePlayer() = requireNotNull(Minecraft.getInstance().player) { "Player must be present to open CMDV screen" }
        val INVENTORY: SimpleContainer = SimpleContainer(45)
    }

    override fun init() {
        super.init()
        searchBox = EditBox(Minecraft.getInstance().font, leftPos + 82,  topPos + 6, 80, 9, Component.translatable("itemGroup.search"))
        searchBox!!.setMaxLength(50)
        searchBox!!.setBordered(false)
        searchBox!!.isVisible = true
        searchBox!!.setCanLoseFocus(false)
        searchBox!!.isFocused = true
        searchBox!!.setValue("")
        //? if >=1.21.6 {
        searchBox!!.setTextColor(-1)
        //?} else {
        /*searchBox!!.setEditableColor(16777215)
        *///?}

        addWidget(searchBox!!)
        val localPlayer = minecraft?.player ?: return
        listener?.let { localPlayer.inventoryMenu.removeSlotListener(it) }
        listener = CreativeInventoryListener(Minecraft.getInstance())
        localPlayer.inventoryMenu.addSlotListener(listener!!)
        search()
    }

    override fun resize(width: Int, height: Int) {
        val i = menu.getRow(scrollPosition)
        val string = searchBox!!.value
        init(width, height)
        searchBox!!.setValue(string)
        if (searchBox!!.value.isNotEmpty()) {
            search()
        }

        scrollPosition = menu.getScrollPosition(i)
        menu.scrollItems(scrollPosition)
    }

    private fun search() {
        menu.itemList.clear()
        val query = searchBox!!.value
        if (query.isEmpty()) {
            menu.itemList.addAll(allItems)
        } else {
            menu.itemList.addAll(allItems.filter {
                it.hoverName.string.contains(query, ignoreCase = true)
            })
        }
        scrollPosition = 0.0f
        menu.scrollItems(0.0f)
    }

    //? if >=1.21.9 {
    override fun charTyped(charInput: CharacterEvent): Boolean {
    //?} else {
    /*override fun charTyped(chr: Char, modifiers: Int): Boolean {
    *///?}
        return if (this.ignoreTypedCharacter) {
            false
        } else {
            val string = this.searchBox!!.value
            //? if >=1.21.9 {
            if (this.searchBox!!.charTyped(charInput)) {
            //?} else {
            /*if (this.searchBox!!.charTyped(chr, modifiers)) {
            *///?}
                if (string != this.searchBox!!.value) {
                    this.search()
                }

                true
            } else {
                false
            }
        }
    }

    //? if >=1.21.9 {
    override fun keyPressed(keyInput: KeyEvent): Boolean {
    //?} else {
    /*override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
    *///?}

        this.ignoreTypedCharacter = false
        val bl = !this.isCreativeInventorySlot(this.hoveredSlot) || this.hoveredSlot!!.hasItem()
        //? if >=1.21.9 {
        val bl2 = InputConstants.getKey(keyInput).numericKeyValue.isPresent
        return if (bl && bl2 && this.checkHotbarKeyPressed(keyInput)) {
        //?} else {
        /*val bl2 = InputUtil.fromKeyCode(keyCode, scanCode).toInt().isPresent
        return if (bl && bl2 && this.handleHotbarKeyPressed(keyCode, scanCode)) {
        *///?}
            this.ignoreTypedCharacter = true
            true
        } else {
            val string = this.searchBox!!.value
            //? if >=1.21.9 {
            if (this.searchBox!!.keyPressed(keyInput)) {
            //?} else {
            /*if (this.searchBox!!.keyPressed(keyCode, scanCode, modifiers)) {
            *///?}
                if (string != this.searchBox!!.value) {
                    this.search()
                }

                true
            } else {
                //? if >=1.21.9 {
                (this.searchBox!!.isFocused && this.searchBox!!.isVisible && keyInput.key != 256) || super.keyPressed(
                    keyInput
                )
                //?} else {
                /*(this.searchBox!!.isFocused && this.searchBox!!.isVisible && keyCode != 256) || super.keyPressed(
                    keyCode,
                    scanCode,
                    modifiers
                )
                *///?}
            }
        }
    }

    //? if >=1.21.9 {
    override fun keyReleased(keyInput: KeyEvent): Boolean {
    //?} else {
    /*override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
    *///?}
        this.ignoreTypedCharacter = false
        //? if >=1.21.9 {
        return super.keyReleased(keyInput)
        //?} else {
        /*return super.keyReleased(keyCode, scanCode, modifiers)
        *///?}
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
            scrollPosition = menu.getScrollPosition(scrollPosition, verticalAmount)
            menu.scrollItems(scrollPosition)
            true
        }
    }

    protected fun isClickInScrollbar(mouseX: Double, mouseY: Double): Boolean {
        val i = leftPos
        val j = topPos
        val k = i + 175
        val l = j + 18
        val m = k + 14
        val n = l + 112
        return mouseX >= k.toDouble() && mouseY >= l.toDouble() && mouseX < m.toDouble() && mouseY < n.toDouble()
    }

    private fun getTabX(group: CreativeModeTab): Int {
        val i = group.column()
        var k = 27 * i
        if (group.isAlignedRight) {
            k = imageWidth - 27 * (7 - i) + 1
        }

        return k
    }

    private fun getTabY(group: CreativeModeTab): Int {
        var i = 0
        if (group.row() == CreativeModeTab.Row.TOP) {
            i -= 32
        } else {
            i += imageHeight
        }

        return i
    }

    protected fun isClickInTab(group: CreativeModeTab, mouseX: Double, mouseY: Double): Boolean {
        val i: Int = getTabX(group)
        val j: Int = getTabY(group)
        return mouseX >= i.toDouble() && mouseX <= (i + 26).toDouble() && mouseY >= j.toDouble() && mouseY <= (j + 32).toDouble()
    }

    private fun hasScrollbar(): Boolean {
        return menu.shouldShowScrollbar()
    }

    //? if >=1.21.9 {
    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        if (click.button() == 0) {
            val d = click.x - leftPos.toDouble()
            val e = click.y - topPos.toDouble()

            for (itemGroup in CreativeModeTabs.tabs()) {
                if (isClickInTab(itemGroup, d, e)) {
                    return true
                }
            }

            if (isClickInScrollbar(
                    click.x,
                    click.y
                )
            ) {
                scrolling = hasScrollbar()
                return true
            }
        }

        return super.mouseClicked(click, doubled)
    }
    //?} else {
    /*
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
    *///?}

    //? if >=1.21.9 {
    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        if (scrolling) {
            val i = topPos + 18
            val j = i + 112
            scrollPosition = (click.y.toFloat() - i.toFloat() - 7.5f) / ((j - i).toFloat() - 15.0f)
            scrollPosition = Mth.clamp(scrollPosition, 0.0f, 1.0f)
            menu.scrollItems(scrollPosition)
            return true
        } else {
            return super.mouseDragged(click, offsetX, offsetY)
        }
    }
    //?} else {
    /*
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
    *///?}

    //? if >=1.21.9 {
    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        if (click.button() == 0) {
            scrolling = false
        }

        return super.mouseReleased(click)
    }
    //?} else {
    /*
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            scrolling = false
        }

        return super.mouseReleased(mouseX, mouseY, button)
    }
    *///?}

    override fun slotClicked(slot: Slot, slotId: Int, button: Int, actionType: ClickType) {
        val player = minecraft?.player ?: return
        var actionType = actionType

        val bl = actionType == ClickType.QUICK_MOVE
        actionType = if (slotId == -999 && actionType == ClickType.PICKUP) ClickType.THROW else actionType
        if (actionType != ClickType.THROW || player.canDropItems()) {
            if (slotId == -999 && actionType != ClickType.QUICK_CRAFT) {
                if (!menu.carried
                        .isEmpty() && lastClickOutsideBounds
                ) {
                    if (!player.canDropItems()) {
                        return
                    }

                    if (button == 0) {
                        player.drop(menu.carried, true)
                        minecraft!!.gameMode!!.handleCreativeModeItemDrop(menu.carried)
                        menu.carried = ItemStack.EMPTY
                    }

                    if (button == 1) {
                        val itemStack = menu.carried.split(1)
                        player.drop(itemStack, true)
                        minecraft!!.gameMode!!.handleCreativeModeItemDrop(itemStack)
                    }
                }
            } else {
                if (!slot.mayPickup(player)) {
                    return
                }

                if (slot === deleteItemSlot && bl) {
                    for (i in player.inventoryMenu.items.indices) {
                        player.inventoryMenu.getSlot(i).set(ItemStack.EMPTY)
                        minecraft!!.gameMode!!.handleCreativeModeItemAdd(ItemStack.EMPTY, i)
                    }
                } else if (selectedTabType == CreativeModeTab.Type.INVENTORY) {
                    if (slot === deleteItemSlot) {
                        menu.carried = ItemStack.EMPTY
                    } else if (actionType == ClickType.THROW && slot.hasItem()) {
                        val itemStack = slot.remove(if (button == 0) 1 else slot.item.maxStackSize)
                        val itemStack2 = slot.item
                        player.drop(itemStack, true)
                        minecraft!!.gameMode!!.handleCreativeModeItemDrop(itemStack)
                        minecraft!!.gameMode!!.handleCreativeModeItemAdd(
                            itemStack2,
                            (slot as CreativeSlot).slot.index
                        )
                    } else if (actionType == ClickType.THROW && slotId == -999 && !menu.carried
                            .isEmpty()
                    ) {
                        player.drop(menu.carried, true)
                        minecraft!!.gameMode!!.handleCreativeModeItemDrop(menu.carried)
                        menu.carried = ItemStack.EMPTY
                    } else {
                        player.inventoryMenu.clicked(
                            (slot as CreativeSlot).slot.index,
                            button,
                            actionType,
                            player
                        )
                        player.inventoryMenu.broadcastChanges()
                    }
                } else if (actionType != ClickType.QUICK_CRAFT && slot.container === INVENTORY) {
                    val itemStack = menu.carried
                    val itemStack2 = slot.item
                    if (actionType == ClickType.SWAP) {
                        if (!itemStack2.isEmpty()) {
                            player.inventory
                                .setItem(button, itemStack2.copyWithCount(itemStack2.maxStackSize))
                            player.inventoryMenu.broadcastChanges()
                        }

                        return
                    }

                    if (actionType == ClickType.CLONE) {
                        if (menu.carried.isEmpty() && slot.hasItem()) {
                            val itemStack3 = slot.item
                            menu.carried = itemStack3.copyWithCount(itemStack3.maxStackSize)
                        }

                        return
                    }

                    if (actionType == ClickType.THROW) {
                        if (!itemStack2.isEmpty()) {
                            val itemStack3 = itemStack2.copyWithCount(if (button == 0) 1 else itemStack2.maxStackSize)
                            player.drop(itemStack3, true)
                            minecraft!!.gameMode!!.handleCreativeModeItemDrop(itemStack3)
                        }

                        return
                    }

                    if (!itemStack!!.isEmpty() && !itemStack2.isEmpty() && ItemStack.isSameItemSameComponents(
                            itemStack,
                            itemStack2
                        )
                    ) {
                        if (button == 0) {
                            if (bl) {
                                itemStack.count = itemStack.maxStackSize
                            } else if (itemStack.count < itemStack.maxStackSize) {
                                itemStack.grow(1)
                            }
                        } else {
                            itemStack.shrink(1)
                        }
                    } else if (!itemStack2.isEmpty() && itemStack.isEmpty()) {
                        val j = if (bl) itemStack2.maxStackSize else itemStack2.count
                        menu.carried = itemStack2.copyWithCount(j)
                    } else if (button == 0) {
                        menu.carried = ItemStack.EMPTY
                    } else if (!menu.carried.isEmpty()) {
                        menu.carried.shrink(1)
                    }
                } else {
                    val itemStack =
                        menu.getSlot(slotId)
                            .item
                    menu.clicked(
                        slotId,
                        button,
                        actionType,
                        player
                    )
                    if (AbstractContainerMenu.getQuickcraftHeader(button) == 2) {
                        for (k in 0..8) {
                            minecraft!!.gameMode!!.handleCreativeModeItemAdd(
                                menu.getSlot(
                                    45 + k
                                ).item, 36 + k
                            )
                        }
                    } else if (Inventory.isHotbarSlot(slot.containerSlot)) {
                        if (actionType == ClickType.THROW && !itemStack.isEmpty() && !menu.carried.isEmpty()
                        ) {
                            val k = if (button == 0) 1 else itemStack.getCount()
                            val itemStack3 = itemStack.copyWithCount(k)
                            itemStack.shrink(k)
                            player.drop(itemStack3, true)
                            minecraft!!.gameMode!!.handleCreativeModeItemDrop(itemStack3)
                        }

                        player.inventoryMenu.broadcastChanges()
                    }
                }
            }
        }
    }

    private fun isCreativeInventorySlot(slot: Slot?): Boolean {
        return false
    }

    //? if >=1.21.9 {
    override fun hasClickedOutside(mouseX: Double, mouseY: Double, left: Int, top: Int): Boolean {
    //?} else {
    /*override fun isClickOutsideBounds(mouseX: Double, mouseY: Double, left: Int, top: Int, button: Int): Boolean {
    *///?}

        val bl = mouseX < left.toDouble() || mouseY < top.toDouble() || mouseX >= (left + imageWidth).toDouble() || mouseY >= (top + imageHeight).toDouble()
        lastClickOutsideBounds = bl
        return lastClickOutsideBounds
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        context.blit(
            //? if >=1.21.6 {
            RenderPipelines.GUI_TEXTURED
            //?} else {
            /*{ texture: Identifier? -> RenderLayer.getGuiTextured(texture) }
            *///?}
            ,BACKGROUND_TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256)

        searchBox!!.render(context, mouseX, mouseY, delta)

        val i = leftPos + 175
        val j = topPos + 18
        val k = j + 112
        val identifier =
            if (hasScrollbar()) SCROLLER_TEXTURE else SCROLLER_DISABLED_TEXTURE
        context.blitSprite(
            //? if >=1.21.6 {
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

    override fun renderLabels(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        context.drawString(
            Minecraft.getInstance().font,
            Component.literal("Custom Models"),
            8,
            6,
            //? if >=1.21.6 {
            -12566464
            //?} else {
            /*4210752
            *///?}
            ,
            false
        )
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        renderTooltip(context, mouseX, mouseY)
    }
}






