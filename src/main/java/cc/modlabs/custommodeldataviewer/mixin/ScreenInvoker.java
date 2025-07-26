package cc.modlabs.custommodeldataviewer.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Element;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// Use generics to match the original method signature and allow any widget type
@Mixin(Screen.class)
public interface ScreenInvoker {
    @Invoker("addDrawableChild")
    <T extends Element> T invokeAddDrawableChild(T drawableElement);
}