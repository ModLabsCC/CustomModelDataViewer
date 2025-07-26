package cc.modlabs.custommodeldataviewer

import cc.modlabs.custommodeldataviewer.gui.CMDVScreen
import cc.modlabs.custommodeldataviewer.gui.IconButtonWidget
import cc.modlabs.custommodeldataviewer.mixin.HandledScreenAccessor
import cc.modlabs.custommodeldataviewer.mixin.ScreenInvoker
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.CustomModelDataComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.resource.ResourceType
import net.minecraft.text.Text
import org.slf4j.LoggerFactory

class Custommodeldataviewer : ClientModInitializer {

    companion object {
        lateinit var INSTANCE: Custommodeldataviewer
        val logger = LoggerFactory.getLogger("CustomModelDataViewer")

        val customModelItems: MutableList<ItemStack> = mutableListOf()

        private fun getCMDVItem(): ItemStack {
            val item = ItemStack(Items.DEBUG_STICK)
            val customModelData = CustomModelDataComponent(listOf(), listOf(), listOf("cmdv"), listOf())
            item.set(DataComponentTypes.CUSTOM_MODEL_DATA, customModelData)
            return item
        }
    }

    override fun onInitializeClient() {
        INSTANCE = this

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(ResourceListener())

        ScreenEvents.AFTER_INIT.register { _, screen, mouseX, mouseY ->
            if (screen is CreativeInventoryScreen) {
                // Place the button to the right of the creative tab inventory
                val accessor = screen as HandledScreenAccessor
                val x = accessor.x + accessor.backgroundWidth + 5
                val y = accessor.y + 5
                val button = IconButtonWidget(
                    x, y, 20, 20,
                    getCMDVItem(),
                    Text.literal(""),
                    { MinecraftClient.getInstance().setScreen(CMDVScreen(customModelItems)) }
                )

                // Render a custom icon or item (optional): overwrite the button’s render, or call context.drawItem(...)
                button.tooltip = Tooltip.of(Text.literal("Custom Model Data Viewer"))

                (screen as ScreenInvoker).invokeAddDrawableChild(button)
            }
        }

        logger.info("CMDV initialized")
    }

}