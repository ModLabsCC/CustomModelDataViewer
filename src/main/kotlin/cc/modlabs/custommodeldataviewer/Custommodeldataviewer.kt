package cc.modlabs.custommodeldataviewer

import cc.modlabs.custommodeldataviewer.gui.CMDVScreen
import cc.modlabs.custommodeldataviewer.gui.IconButtonWidget
import cc.modlabs.custommodeldataviewer.mixin.AbstractContainerScreenAccessor
import cc.modlabs.custommodeldataviewer.mixin.ScreenInvoker
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.item.component.TypedEntityData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.server.packs.PackType
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey

class Custommodeldataviewer : ClientModInitializer {

    companion object {
        lateinit var INSTANCE: Custommodeldataviewer
        val logger = LoggerFactory.getLogger("CustomModelDataViewer")

        val customModelItems: MutableList<ItemStack> = mutableListOf()

        private val OPERATORS_TAB: ResourceKey<CreativeModeTab> = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.withDefaultNamespace("op_blocks")
        )

        private fun getCMDVItem(): ItemStack {
            val item = ItemStack(Items.DEBUG_STICK)
            val customModelData = CustomModelData(listOf(), listOf(), listOf("cmdv"), listOf())
            item.set(DataComponents.CUSTOM_MODEL_DATA, customModelData)
            return item
        }

        private fun getInvisibleFrame(
            item: Item,
            entityType: EntityType<*>,
            enchantment: Holder<Enchantment>
        ): ItemStack {
            val publicBukkitValues = CompoundTag().apply { putByte("imageframe:invisible", 1) }
            val customData = CompoundTag().apply { put("PublicBukkitValues", publicBukkitValues) }
            val entityData = CompoundTag().apply { putBoolean("Invisible", true) }

            return ItemStack(item).apply {
                set(
                    DataComponents.LORE,
                    ItemLore(listOf(Component.translatable("effect.minecraft.invisibility").withStyle {
                        it.withColor(ChatFormatting.GRAY).withItalic(false)
                    }))
                )
                set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.ENCHANTMENTS, true))
                enchant(enchantment, 1)
                set(DataComponents.CUSTOM_DATA, CustomData.of(customData))
                set(DataComponents.ENTITY_DATA, TypedEntityData.of(entityType, entityData))
            }
        }
    }

    override fun onInitializeClient() {
        INSTANCE = this

        CreativeModeTabEvents.modifyOutputEvent(OPERATORS_TAB).register { output ->
            val enchantment = output.context.holders()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.LUCK_OF_THE_SEA)
            output.accept(getInvisibleFrame(Items.ITEM_FRAME, EntityTypes.ITEM_FRAME, enchantment))
            output.accept(getInvisibleFrame(Items.GLOW_ITEM_FRAME, EntityTypes.GLOW_ITEM_FRAME, enchantment))
        }

        val resourceListener = ResourceListener()
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(resourceListener)
        ClientPlayConnectionEvents.JOIN.register { _, _, client ->
            resourceListener.refreshItems(client.resourceManager)
        }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> customModelItems.clear() }

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
                    { Minecraft.getInstance().gui.setScreen(CMDVScreen(customModelItems)) }
                )

                // Render a custom icon or item (optional): overwrite the button’s render, or call context.drawItem(...)
                button.setTooltip(Tooltip.create(Component.literal("Custom Model Data Viewer")))

                (screen as ScreenInvoker).invokeAddRenderableWidget(button)
            }
        }

        logger.info("CMDV initialized")
    }

}
