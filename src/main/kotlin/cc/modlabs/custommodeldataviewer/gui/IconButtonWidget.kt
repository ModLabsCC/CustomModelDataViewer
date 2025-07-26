package cc.modlabs.custommodeldataviewer.gui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.item.ItemStack
import net.minecraft.text.Text

class IconButtonWidget(
    x: Int, y: Int, width: Int, height: Int,
    val iconStack: ItemStack,
    message: Text,
    onPress: (ButtonWidget) -> Unit
) : ButtonWidget(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER) {

    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        super.renderWidget(context, mouseX, mouseY, delta)
        // Draw the item icon in the center of the button
        context.drawItem(
            iconStack,
            this.x + (this.width - 16) / 2,
            this.y + (this.height - 16) / 2
        )
    }
}