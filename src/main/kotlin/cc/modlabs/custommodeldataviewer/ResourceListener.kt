package cc.modlabs.custommodeldataviewer

import com.google.gson.JsonElement
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
        val itemsWithCustomModels = mutableSetOf<ItemStack>()

        manager.findResources("items") { true }.forEach { (identifier, resource) ->
            try {
                resource.inputStream.reader().use { reader ->
                    val json = JsonParser.parseReader(reader).asJsonObject
                    if (!json.has("model")) return@use

                    val rootModel = json.getAsJsonObject("model")
                    val propertyValue = rootModel.get("property")?.asString
                    if (!isCustomModelDataProperty(propertyValue)) return@use

                    val itemId = identifier.path.split("/").last().substringBefore('.')
                    val item = Registries.ITEM.get(Identifier.ofVanilla(itemId))

                    val foundBefore = itemsWithCustomModels.size
                    collectCustomModelItems(
                        rootModel,
                        item,
                        identifier,
                        resource,
                        itemsWithCustomModels,
                        emptyList(),
                        emptyList()
                    )
                    val found = itemsWithCustomModels.size - foundBefore
                    if (found > 0) {
                        Custommodeldataviewer.logger.info("Found $found custom model data for $identifier")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error reading custom model data file for $identifier")
            }
        }

        return itemsWithCustomModels.toList()
    }

    private fun isCustomModelDataProperty(property: String?): Boolean {
        if (property == null) return false
        val key = property.substringAfterLast(':')
        return key == "custom_model_data"
    }

    private fun getTypeName(model: JsonObject): String? {
        val type = model.get("type")?.asString ?: return null
        return type.substringAfterLast(':')
    }

    private fun asObject(element: JsonElement?): JsonObject? = try {
        element?.asJsonObject
    } catch (_: Exception) {
        null
    }

    private fun collectCustomModelItems(
        model: JsonObject,
        item: net.minecraft.item.Item,
        identifier: Identifier,
        resource: net.minecraft.resource.Resource,
        sink: MutableSet<ItemStack>,
        predicates: List<String>,
        thresholds: List<Float>
    ) {
        when (getTypeName(model)) {
            "model" -> {
                if (!model.has("model")) return
                val modelName = model.getAsJsonPrimitive("model").asString
                val stack = ItemStack(item, 1)
                if (stack.isEmpty) {
                    Custommodeldataviewer.logger.warn(
                        "Empty item stack for model $modelName - identifier: $identifier - resource: $resource"
                    )
                    return
                }
                stack.set(DataComponentTypes.ITEM_NAME, Text.literal(modelName))
                val colors = getTints(model, stack, modelName)
                val customModelData = CustomModelDataComponent(thresholds, listOf(), predicates, colors)
                stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, customModelData)
                sink.add(stack)
            }
            "select" -> {
                val cases = model.getAsJsonArray("cases") ?: return
                for (caseElem in cases) {
                    val caseObj = asObject(caseElem) ?: continue
                    val predicate = caseObj.get("when")?.asString ?: continue
                    val nested = asObject(caseObj.get("model")) ?: continue
                    collectCustomModelItems(
                        nested,
                        item,
                        identifier,
                        resource,
                        sink,
                        predicates + predicate,
                        thresholds
                    )
                }
            }
            "range_dispatch" -> {
                val entries = model.getAsJsonArray("entries") ?: return
                for (entry in entries) {
                    val entryObj = asObject(entry) ?: continue
                    val thresholdValue = entryObj.get("threshold")?.asFloat ?: continue
                    val nested = asObject(entryObj.get("model")) ?: continue
                    collectCustomModelItems(
                        nested,
                        item,
                        identifier,
                        resource,
                        sink,
                        predicates,
                        thresholds + thresholdValue
                    )
                }
            }
            "composite" -> {
                val models = model.getAsJsonArray("models") ?: return
                for (sub in models) {
                    val subObj = asObject(sub) ?: continue
                    collectCustomModelItems(
                        subObj,
                        item,
                        identifier,
                        resource,
                        sink,
                        predicates,
                        thresholds
                    )
                }
            }
            "condition" -> {
                asObject(model.get("on_true"))?.let {
                    collectCustomModelItems(it, item, identifier, resource, sink, predicates, thresholds)
                }
                asObject(model.get("on_false"))?.let {
                    collectCustomModelItems(it, item, identifier, resource, sink, predicates, thresholds)
                }
            }
            else -> {
                // Types like empty, bundle/selected_item, special are ignored for collection purposes
            }
        }
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