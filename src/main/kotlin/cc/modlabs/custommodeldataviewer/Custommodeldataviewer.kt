package cc.modlabs.custommodeldataviewer

import cc.modlabs.custommodeldataviewer.gui.CMDVScreen
import cc.modlabs.custommodeldataviewer.gui.IconButtonWidget
import cc.modlabs.custommodeldataviewer.mixin.AbstractContainerScreenAccessor
import cc.modlabs.custommodeldataviewer.mixin.ScreenInvoker
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.server.packs.PackType
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory

class Custommodeldataviewer : ClientModInitializer {

    companion object {
        lateinit var INSTANCE: Custommodeldataviewer
        val logger = LoggerFactory.getLogger("CustomModelDataViewer")

        val customModelItems: MutableList<ItemStack> = mutableListOf()

        private fun getCMDVItem(): ItemStack {
            val item = ItemStack(Items.DEBUG_STICK)
            val customModelData = CustomModelData(listOf(), listOf(), listOf("cmdv"), listOf())
            item.set(DataComponents.CUSTOM_MODEL_DATA, customModelData)
            return item
        }
    }

    override fun onInitializeClient() {
        INSTANCE = this

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(ResourceListener())

        ScreenEvents.AFTER_INIT.register { _, screen, mouseX, mouseY ->
            if (screen is CreativeModeInventoryScreen) {
                // Place the button to the right of the creative tab inventory
                val accessor = screen as AbstractContainerScreenAccessor
                val x = accessor.leftPos + accessor.imageWidth + 5
                val y = accessor.topPos + 5
                val button = IconButtonWidget(
                    x, y, 20, 20,
                    getCMDVItem(),
                    Component.literal(""),
                    { Minecraft.getInstance().setScreen(CMDVScreen(customModelItems)) }
                )

                // Render a custom icon or item (optional): overwrite the button’s render, or call context.drawItem(...)
                button.setTooltip(Tooltip.create(Component.literal("Custom Model Data Viewer")))

                (screen as ScreenInvoker).invokeAddRenderableWidget(button)
            }
        }

        logger.info("CMDV initialized")
    }

}