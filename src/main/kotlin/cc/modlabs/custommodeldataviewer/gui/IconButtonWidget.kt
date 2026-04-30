package cc.modlabs.custommodeldataviewer.gui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component as MCText

class IconButtonWidget(
    x: Int, y: Int, width: Int, height: Int,
    val iconStack: ItemStack,
    message: MCText,
    onPress: (Button) -> Unit
) : Button(x, y, width, height, message, onPress, DEFAULT_NARRATION) {

    override fun extractContents(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        extractDefaultSprite(context)
        context.item(
            iconStack,
            this.x + (this.width - 16) / 2,
            this.y + (this.height - 16) / 2
        )
    }
}
