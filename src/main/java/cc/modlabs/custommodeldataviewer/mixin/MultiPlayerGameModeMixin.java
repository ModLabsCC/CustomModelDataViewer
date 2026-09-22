package cc.modlabs.custommodeldataviewer.mixin;

import cc.modlabs.custommodeldataviewer.gui.CMDVScreen;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Definition(id = "CreativeModeInventoryScreen", type = CreativeModeInventoryScreen.class)
    @Expression("? instanceof CreativeModeInventoryScreen")
    @ModifyExpressionValue(method = "handleCreativeModeItemDrop", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean cmdv$allowViewerDrops(boolean creativeScreen) {
        return creativeScreen || Minecraft.getInstance().gui.screen() instanceof CMDVScreen;
    }
}
