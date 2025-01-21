package cc.modlabs.custommodeldataviewer.client

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider
import net.minecraft.registry.RegistryWrapper
import java.util.concurrent.CompletableFuture

class LanguageProvider(dataOutput: FabricDataOutput, registryLookup: CompletableFuture<RegistryWrapper.WrapperLookup>) : FabricLanguageProvider(dataOutput, "en_us", registryLookup) {

    override fun generateTranslations(
        wrapperLookup: RegistryWrapper.WrapperLookup,
        translationBuilder: TranslationBuilder
    ) {

        translationBuilder.add("itemGroup.${CustommodeldataviewerClient.MOD_ID}", "Custom Model Data Viewer")

    }

}