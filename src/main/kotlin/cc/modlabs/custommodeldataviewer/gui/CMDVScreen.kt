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
        val gameMode = minecraft?.gameMode ?: return
        var clickType = actionType

        if (isCreativeInventorySlot(slot)) {
            searchBox?.moveCursorToEnd(false)
            searchBox?.setHighlightPos(0)
        }

        val quickMove = clickType == ClickType.QUICK_MOVE
        clickType = if (slotId == -999 && clickType == ClickType.PICKUP) ClickType.THROW else clickType

        if (slotId == -999 && selectedTabType != CreativeModeTab.Type.INVENTORY && clickType != ClickType.QUICK_CRAFT) {
            if (!menu.carried.isEmpty && lastClickOutsideBounds) {
                if (button == 0) {
                    player.drop(menu.carried, true)
                    gameMode.handleCreativeModeItemDrop(menu.carried)
                    menu.carried = ItemStack.EMPTY
                } else if (button == 1) {
                    val dropped = menu.carried.split(1)
                    player.drop(dropped, true)
                    gameMode.handleCreativeModeItemDrop(dropped)
                }
            }
            return
        }

        if (slotId == -999) {
            if (clickType == ClickType.QUICK_CRAFT) {
                menu.clicked(slotId, button, clickType, player)
                if (AbstractContainerMenu.getQuickcraftHeader(button) == 2) {
                    for (k in 0..8) {
                        gameMode.handleCreativeModeItemAdd(menu.getSlot(45 + k).item, 36 + k)
                    }
                }
                player.inventoryMenu.broadcastChanges()
            }
            return
        }

        if (!slot.mayPickup(player)) {
            return
        }

        if (slot === deleteItemSlot && quickMove) {
            for (i in player.inventoryMenu.items.indices) {
                gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, i)
            }
            return
        }

        if (selectedTabType == CreativeModeTab.Type.INVENTORY) {
            if (slot === deleteItemSlot) {
                menu.carried = ItemStack.EMPTY
            } else if (clickType == ClickType.THROW && slot.hasItem()) {
                val removed = slot.remove(if (button == 0) 1 else slot.item.maxStackSize)
                val remaining = slot.item
                player.drop(removed, true)
                gameMode.handleCreativeModeItemDrop(removed)
                gameMode.handleCreativeModeItemAdd(remaining, (slot as CreativeSlot).slot.index)
            } else if (clickType == ClickType.THROW && !menu.carried.isEmpty) {
                player.drop(menu.carried, true)
                gameMode.handleCreativeModeItemDrop(menu.carried)
                menu.carried = ItemStack.EMPTY
            } else {
                player.inventoryMenu.clicked(
                    if (slotId == -999) slotId else (slot as CreativeSlot).slot.index,
                    button,
                    clickType,
                    player
                )
                player.inventoryMenu.broadcastChanges()
            }
            return
        }

        if (clickType != ClickType.QUICK_CRAFT && slot.container === INVENTORY) {
            val carried = menu.carried
            val slotStack = slot.item

            if (clickType == ClickType.SWAP) {
                if (!slotStack.isEmpty) {
                    player.inventory.setItem(button, slotStack.copyWithCount(slotStack.maxStackSize))
                    player.inventoryMenu.broadcastChanges()
                }
                return
            }

            if (clickType == ClickType.CLONE) {
                if (menu.carried.isEmpty && slot.hasItem()) {
                    menu.carried = slotStack.copyWithCount(slotStack.maxStackSize)
                }
                return
            }

            if (clickType == ClickType.THROW) {
                if (!slotStack.isEmpty) {
                    val dropped = slotStack.copyWithCount(if (button == 0) 1 else slotStack.maxStackSize)
                    player.drop(dropped, true)
                    gameMode.handleCreativeModeItemDrop(dropped)
                }
                return
            }

            if (!carried.isEmpty && !slotStack.isEmpty && ItemStack.isSameItemSameComponents(carried, slotStack)) {
                if (button == 0) {
                    if (quickMove) {
                        carried.count = carried.maxStackSize
                    } else if (carried.count < carried.maxStackSize) {
                        carried.grow(1)
                    }
                } else {
                    carried.shrink(1)
                }
            } else if (!slotStack.isEmpty && carried.isEmpty) {
                val count = if (quickMove) slotStack.maxStackSize else slotStack.count
                menu.carried = slotStack.copyWithCount(count)
            } else if (button == 0) {
                menu.carried = ItemStack.EMPTY
            } else if (!menu.carried.isEmpty) {
                menu.carried.shrink(1)
            }
            return
        }

        val previousStack = if (slotId == -999) ItemStack.EMPTY else menu.getSlot(slotId).item.copy()
        menu.clicked(slotId, button, clickType, player)
        if (AbstractContainerMenu.getQuickcraftHeader(button) == 2) {
            for (k in 0..8) {
                gameMode.handleCreativeModeItemAdd(menu.getSlot(45 + k).item, 36 + k)
            }
        } else if (slotId != -999 && Inventory.isHotbarSlot(slot.containerSlot)) {
            val currentStack = menu.getSlot(slotId).item
            gameMode.handleCreativeModeItemAdd(currentStack, 36 + slot.containerSlot)
            if (clickType == ClickType.SWAP && button in 0..8) {
                gameMode.handleCreativeModeItemAdd(previousStack, 36 + button)
            } else if (clickType == ClickType.THROW && !previousStack.isEmpty) {
                val dropped = previousStack.copyWithCount(if (button == 0) 1 else previousStack.maxStackSize)
                player.drop(dropped, true)
                gameMode.handleCreativeModeItemDrop(dropped)
            }
            player.inventoryMenu.broadcastChanges()
        }
    }

    private fun isCreativeInventorySlot(slot: Slot?): Boolean {
        return slot != null && slot.container === INVENTORY
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





