package cc.modlabs.custommodeldataviewer.client

import com.google.gson.JsonParser
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.CustomModelDataComponent
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceReloader
import net.minecraft.text.Text
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class ResourceListener : IdentifiableResourceReloadListener {
    override fun getFabricId(): Identifier {
        return Identifier.of("custommodeldataviewer", "resourcelistener")
    }

    override fun reload(
        synchronizer: ResourceReloader.Synchronizer,
        manager: ResourceManager,
        prepareExecutor: Executor,
        applyExecutor: Executor
    ): CompletableFuture<Void> {
        val prepareFuture = CompletableFuture.supplyAsync(
            {
                CustommodeldataviewerClient.logger.info("PrePareExecutor executed")
                null // Preparation data (null in this case)
            },
            prepareExecutor
        )

        return prepareFuture.thenCompose { _ ->
            synchronizer.whenPrepared(null)
        }.thenAcceptAsync(
            {
                val items = getAllItemsWithModelData(manager)
                addEntriesToOperatorTab(items)
                CustommodeldataviewerClient.logger.info("Resources reloaded - found ${items.size} custom modeled items")
            },
            applyExecutor
        )
    }

    private fun getAllItemsWithModelData(manager: ResourceManager): List<ItemStack> {
        val itemsWithCustomModels = mutableSetOf<ItemStack>() // Using Set to avoid duplicates
        // Check all resource packs for this item's model
        manager.findResources("items") { true }.forEach { (identifier, resource) ->
            try {
                resource.inputStream.reader().use { reader ->
                    val json = JsonParser.parseReader(reader).asJsonObject

                    // Check for the new select model format
                    if (json.has("model")) {
                        val model = json.getAsJsonObject("model")
                        if (model.get("type")?.asString ?.contains("select") == true &&
                            model.get("property")?.asString?.contains("custom_model_data") == true) {

                            val cases = model.getAsJsonArray("cases")
                            val item = Registries.ITEM.get(Identifier.ofVanilla(identifier.path.split("/").last().split(".").first()))

                            var found = 0
                            for (case in cases) {
                                val caseObj = case.asJsonObject
                                val predicate = caseObj.getAsJsonPrimitive("when").asString
                                val stack = ItemStack(item, 1)

                                if (stack.isEmpty) {
                                    CustommodeldataviewerClient.logger.warn("Empty item stack for $predicate : type ${item::class.simpleName} - identifier: $identifier - resource: $resource")
                                    continue
                                }

                                stack.set(DataComponentTypes.ITEM_NAME, Text.literal(predicate))


                                val colors = mutableListOf<Int>()
                                if (caseObj.get("tints") != null) {
                                    CustommodeldataviewerClient.logger.info("Found tints for $predicate")
                                    for (tint in caseObj.getAsJsonArray("tints")) {
                                        colors.add(tint.asJsonObject.getAsJsonPrimitive("default").asInt)
                                    }
                                    stack.set(DataComponentTypes.BASE_COLOR, DyeColor.byFireworkColor(colors.first()))
                                }

                                val customModelData = CustomModelDataComponent(listOf(), listOf(), listOf(predicate), colors)
                                stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, customModelData)

                                itemsWithCustomModels.add(stack)
                                found++
                            }

                            if (found > 0) {
                                CustommodeldataviewerClient.logger.info("Found $found custom model data for $identifier")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error reading custom model data file for $identifier")
            }
        }

        return itemsWithCustomModels.toList()
    }

    private fun addEntriesToOperatorTab(items: List<ItemStack>) {
        ItemGroupEvents.modifyEntriesEvent(CustommodeldataviewerClient.CUSTOM_ITEM_GROUP_KEY)
            .register(ModifyEntries { content: FabricItemGroupEntries ->
                items.forEach {
                    content.add(it)
                }
            })
    }
}