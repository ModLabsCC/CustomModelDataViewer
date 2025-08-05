package cc.modlabs.custommodeldataviewer

import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
import net.minecraft.client.MinecraftClient
import net.minecraft.item.Items
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
                Custommodeldataviewer.logger.info("PrePareExecutor executed")
                null // Preparation data (null in this case)
            },
            prepareExecutor
        )

        return prepareFuture.thenCompose { _ ->
            synchronizer.whenPrepared(null)
        }.thenAcceptAsync(
            {
                val items = getAllItemsWithModelData(manager)
                val client = MinecraftClient.getInstance()
                client.execute {
                    Custommodeldataviewer.customModelItems.clear()
                    Custommodeldataviewer.customModelItems.addAll(items)
                }
                Custommodeldataviewer.logger.info("Resources reloaded - found ${items.size} custom modeled items")
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
                        if (model.get("property")?.asString?.contains("custom_model_data") == true) {

                            var found = 0

                            val modelType = model.get("type")?.asString

                            if(modelType?.contains("select") == true) {
                                val cases = model.getAsJsonArray("cases")
                                val item = Registries.ITEM.get(Identifier.ofVanilla(identifier.path.split("/").last().split(".").first()))

                                for (case in cases) {
                                    val caseObj = case.asJsonObject
                                    val predicate = caseObj.getAsJsonPrimitive("when").asString
                                    val caseModelObj = caseObj.getAsJsonObject("model")
                                    val stack = ItemStack(item, 1)

                                    if (stack.isEmpty) {
                                        Custommodeldataviewer.logger.warn("Empty item stack for $predicate : type ${item::class.simpleName} - identifier: $identifier - resource: $resource")
                                        continue
                                    }

                                    val modelName = caseModelObj.getAsJsonPrimitive("model").asString
                                    stack.set(DataComponentTypes.ITEM_NAME, Text.literal(modelName))

                                    val colors = getTints(caseObj, stack, modelName)

                                    val customModelData = CustomModelDataComponent(listOf(), listOf(), listOf(predicate), colors)
                                    stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, customModelData)

                                    itemsWithCustomModels.add(stack)
                                    found++
                                }
                            } else if (modelType?.contains("range_dispatch") == true) {
                                val entries = model.getAsJsonArray("entries")
                                val item = Registries.ITEM.get(Identifier.ofVanilla(identifier.path.split("/").last().split(".").first()))

                                for (entry in entries) {
                                    val entryObj = entry.asJsonObject
                                    val threshold = entryObj.getAsJsonPrimitive("threshold").asInt
                                    val entryModelObj = entryObj.getAsJsonObject("model")

                                    // often has "model": "item/name", and possibly "type": "model"

                                    val stack = ItemStack(item, 1)

                                    if (stack.isEmpty) {
                                        Custommodeldataviewer.logger.warn("Empty item stack for threshold $threshold : type ${item::class.simpleName} - identifier: $identifier - resource: $resource")
                                        continue
                                    }

                                    val modelName = entryModelObj.getAsJsonPrimitive("model").asString
                                    stack.set(DataComponentTypes.ITEM_NAME, Text.literal(modelName))

                                    val colors = getTints(entryObj, stack, modelName)

                                    val customModelData = CustomModelDataComponent(listOf(threshold.toFloat()), listOf(), listOf(), colors)
                                    stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, customModelData)

                                    itemsWithCustomModels.add(stack)
                                    found++
                                }
                            }

                            if (found > 0) {
                                Custommodeldataviewer.logger.info("Found $found custom model data for $identifier")
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

    private fun getTints(jObj: JsonObject, stack: ItemStack, name: String): List<Int> {
        val colors = mutableListOf<Int>()
        if (jObj.get("tints") != null) {
            Custommodeldataviewer.logger.info("Found tints for $name")
            for (tint in jObj.getAsJsonArray("tints")) {
                colors.add(tint.asJsonObject.getAsJsonPrimitive("default").asInt)
            }
            stack.set(DataComponentTypes.BASE_COLOR, DyeColor.byFireworkColor(colors.first()))
        }
        return colors
    }
}