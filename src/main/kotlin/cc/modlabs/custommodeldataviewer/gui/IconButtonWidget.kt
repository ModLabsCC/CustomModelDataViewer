package cc.modlabs.custommodeldataviewer.gui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component as MCText

class IconButtonWidget(
    x: Int, y: Int, width: Int, height: Int,
    val iconStack: ItemStack,
    message: MCText,
    onPress: (Button) -> Unit
) : Button(x, y, width, height, message, onPress, DEFAULT_NARRATION) {

    override fun renderContents(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        context.renderItem(
            iconStack,
            this.x + (this.width - 16) / 2,
            this.y + (this.height - 16) / 2
        )
    }
}
