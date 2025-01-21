package cc.modlabs.custommodeldataviewer.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.CustomModelDataComponent
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.resource.ResourceType
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.util.function.Supplier


class CustommodeldataviewerClient : ClientModInitializer {

    companion object {
        lateinit var INSTANCE: CustommodeldataviewerClient
        val logger = LoggerFactory.getLogger("CustomModelDataViewer")
        val MOD_ID = "custommodeldataviewer"

        val CUSTOM_ITEM_GROUP_KEY: RegistryKey<ItemGroup> =
            RegistryKey.of(Registries.ITEM_GROUP.getKey(), Identifier.of(MOD_ID, "item_group"))
        val CUSTOM_ITEM_GROUP: ItemGroup = FabricItemGroup.builder()
            .icon(Supplier { getCMDVItem() })
            .displayName(Text.translatable("itemGroup.$MOD_ID"))
            .build()

        private fun getCMDVItem(): ItemStack {
            val item = ItemStack(Items.DEBUG_STICK)
            val customModelData = CustomModelDataComponent(listOf(), listOf(), listOf("cmdv"), listOf())
            item.set(DataComponentTypes.CUSTOM_MODEL_DATA, customModelData)
            return item
        }
    }

    override fun onInitializeClient() {
        INSTANCE = this

        Registry.register(Registries.ITEM_GROUP, CUSTOM_ITEM_GROUP_KEY, CUSTOM_ITEM_GROUP)

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(ResourceListener())
        logger.info("CMDV initialized")
    }

}
