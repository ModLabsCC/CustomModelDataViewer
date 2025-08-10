package cc.modlabs.custommodeldataviewer

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.CustomModelDataComponent
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceReloader
import net.minecraft.text.Text
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.client.MinecraftClient
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
        val tints = jObj.get("tints") ?: return colors
        Custommodeldataviewer.logger.info("Found tints for $name")

        var appliedDyedColor = false

        for (tintElem in tints.asJsonArray) {
            val tintObj = tintElem.asJsonObject
            val typeRaw = tintObj.get("type")?.asString ?: continue
            val type = typeRaw.substringAfterLast(':')

            when (type) {
                // Direct color
                "constant" -> {
                    parseColorFromJson(tintObj.get("value"))?.let { colors.add(it) }
                }

                // Defaultable sources (no live context in viewer)
                "dye" -> {
                    val color = parseColorFromJson(tintObj.get("default"))
                    if (color != null) {
                        colors.add(color)
                        // Prefer DYED_COLOR for dye tints (per spec behavior)
                        stack.set(DataComponentTypes.DYED_COLOR, DyedColorComponent(color
                            //? if <1.21.6 {
                            /*,true
                            *///?}
                        ))
                        appliedDyedColor = true
                    }
                }
                "firework", "map_color", "potion", "team", "custom_model_data" -> {
                    parseColorFromJson(tintObj.get("default"))?.let { colors.add(it) }
                }
                else -> {
                    // Unknown tint types are ignored but still collected if they had a default value
                    parseColorFromJson(tintObj.get("default"))?.let { colors.add(it) }
                }
            }
        }

        // For non-dye tint previews, use BASE_COLOR as a generic cue
        if (!appliedDyedColor && colors.isNotEmpty()) {
            stack.set(DataComponentTypes.BASE_COLOR, DyeColor.byFireworkColor(colors.first()))
        }
        return colors
    }

    private fun parseColorFromJson(element: JsonElement?): Int? {
        if (element == null) return null
        if (element.isJsonPrimitive && element.asJsonPrimitive.isNumber) {
            return try { element.asInt } catch (_: Exception) { null }
        }
        if (element.isJsonArray) {
            val arr = element.asJsonArray
            if (arr.size() >= 3) {
                val r = arr[0]
                val g = arr[1]
                val b = arr[2]
                val rf = r.asDouble
                val gf = g.asDouble
                val bf = b.asDouble
                val r255 = toChannel255(rf)
                val g255 = toChannel255(gf)
                val b255 = toChannel255(bf)
                return (r255 shl 16) or (g255 shl 8) or b255
            }
        }
        return null
    }

    private fun toChannel255(value: Double): Int {
        // If value in [0,1], scale; otherwise clamp as 0..255 (supports json providing 0..255 directly)
        val scaled = if (value <= 1.0) (value * 255.0) else value
        return scaled.coerceIn(0.0, 255.0).toInt()
    }
}