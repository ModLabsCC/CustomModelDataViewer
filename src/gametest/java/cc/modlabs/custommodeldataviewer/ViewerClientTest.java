package cc.modlabs.custommodeldataviewer;

import cc.modlabs.custommodeldataviewer.gui.CMDVScreen;
import cc.modlabs.custommodeldataviewer.gui.IconButtonWidget;
import cc.modlabs.custommodeldataviewer.mixin.AbstractContainerScreenAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import java.util.List;
import java.util.Optional;

public final class ViewerClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder()
                .adjustSettings(settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                .create()) {
            context.runOnClient(client -> checkModels());
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(CreativeModeInventoryScreen.class);
            var button = context.computeOnClient(client -> client.gui.screen().children().stream()
                    .filter(IconButtonWidget.class::isInstance)
                    .map(IconButtonWidget.class::cast)
                    .findFirst().orElseThrow(() -> new AssertionError("Viewer button is missing")));
            moveCursor(context, button.getX() + 10, button.getY() + 10);
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.waitForScreen(CMDVScreen.class);
            var bounds = context.computeOnClient(client -> {
                var screen = (AbstractContainerScreenAccessor) client.gui.screen();
                return new int[]{screen.getLeftPos(), screen.getTopPos()};
            });
            context.getInput().typeChars("diamond");
            context.runOnClient(client -> {
                var menu = ((CMDVScreen) client.gui.screen()).getMenu();
                require(menu.getItemList().size() == 54, "Search must retain all 54 diamond variants");
                require(menu.shouldShowScrollbar(), "More than 45 variants must enable scrolling");
            });
            context.getInput().scroll(-1);
            context.runOnClient(client -> {
                var menu = ((CMDVScreen) client.gui.screen()).getMenu();
                require(ItemStack.isSameItemSameComponents(menu.getSlot(0).getItem(), menu.getItemList().get(9)),
                        "Scrolling must advance the visible items by one row");
            });
            moveCursor(context, bounds[0] + 181, bounds[1] + 120);
            context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
            moveCursor(context, bounds[0] + 181, bounds[1] + 20);
            context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.runOnClient(client -> {
                var menu = ((CMDVScreen) client.gui.screen()).getMenu();
                require(ItemStack.isSameItemSameComponents(menu.getSlot(0).getItem(), menu.getItemList().getFirst()),
                        "Dragging the scrollbar with SDL's left button must return to the first row");
            });

            moveCursor(context, bounds[0] + 17, bounds[1] + 26);
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.runOnClient(client -> require(((CMDVScreen) client.gui.screen()).getMenu().getCarried().is(Items.DIAMOND),
                    "Left-clicking a model must pick it up"));
            moveCursor(context, bounds[0] - 20, bounds[1] + 50);
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.runOnClient(client -> require(((CMDVScreen) client.gui.screen()).getMenu().getCarried().isEmpty(),
                    "Clicking outside must drop the carried model"));
            context.waitFor(client -> {
                for (var entity : client.level.entitiesForRendering()) {
                    if (entity instanceof ItemEntity item && item.getItem().is(Items.DIAMOND)) return true;
                }
                return false;
            });
            moveCursor(context, bounds[0] + 17, bounds[1] + 26);
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            moveCursor(context, bounds[0] + 17, bounds[1] + 120);
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            world.getConnection().waitForServerboundPackets();
            world.getServer().runOnServer(server -> {
                var stack = world.getConnection().getServerPlayer().getInventory().getItem(0);
                require(stack.is(Items.DIAMOND) && strings(stack).equals(List.of("fixture_0")),
                        "Placing a model in the hotbar must preserve its components on the server");
            });
            context.takeScreenshot("cmdv-26.3-search-scroll");
            context.getInput().typeChars("not-found");
            context.runOnClient(client -> require(((CMDVScreen) client.gui.screen()).getMenu().getItemList().isEmpty(),
                    "Search must clear items when there are no matches"));
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitForScreen(null);

            context.runOnClient(client -> Custommodeldataviewer.Companion.getCustomModelItems().clear());
            var reload = context.computeOnClient(client -> client.reloadResourcePacks());
            context.waitFor(client -> reload.isDone(), 1200);
            reload.join();
            context.runOnClient(client -> checkModels());
        }
        context.runOnClient(client -> require(Custommodeldataviewer.Companion.getCustomModelItems().isEmpty(),
                "Disconnect must clear items bound to the previous world's components"));
    }

    private static void checkModels() {
        var items = Custommodeldataviewer.Companion.getCustomModelItems();
        checkReportedNumericModels(items);
        require(items.stream().anyMatch(item -> item.is(Items.DEBUG_STICK)
                && strings(item).equals(List.of("cmdv"))), "Built-in CMDV model must be available after JOIN/reload");
        var diamonds = items.stream().filter(item -> item.is(Items.DIAMOND)).toList();
        require(diamonds.size() == 54, "select.when array must produce exactly 54 variants, got " + diamonds.size());
        for (int i = 0; i < 54; i++) {
            require(strings(diamonds.get(i)).equals(List.of("fixture_" + i)),
                    "Only custom_model_data selectors may add CMD strings; variant " + i);
            require(diamonds.get(i).get(DataComponents.EQUIPPABLE) == null,
                    "Non-equippable items must remain non-equippable");
        }
        var elytra = items.stream().filter(item -> item.is(Items.ELYTRA)
                && strings(item).equals(List.of("salvation"))).findFirst()
                .orElseThrow(() -> new AssertionError("Custom elytra preview is missing"));
        var original = new ItemStack(Items.ELYTRA).get(DataComponents.EQUIPPABLE);
        require(original != null, "Elytra must have an equippable component");
        var asset = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.parse("xtr:elytra/salvation"));
        var expected = new Equippable(original.slot(), original.equipSound(), Optional.of(asset),
                original.cameraOverlay(), original.allowedEntities(), original.dispensable(), original.swappable(),
                original.damageOnHurt(), original.equipOnInteract(), original.canBeSheared(), original.shearingSound());
        require("xtr:elytra/salvation".equals(elytra.get(DataComponents.ITEM_NAME).getString()),
                "Custom model path must be used as the item name");
        require(expected.equals(elytra.get(DataComponents.EQUIPPABLE)),
                "Custom elytra must use the model path as its asset ID and preserve other equipment settings");
    }

    private static void checkReportedNumericModels(List<ItemStack> items) {
        // Reproduce the paper.json shown in issue #2, including unqualified type/property names.
        // Removing numeric range dispatch support must make this check fail on JOIN and reload.
        var paper = items.stream().filter(item -> item.is(Items.PAPER)).toList();
        require(paper.size() == 3,
                "Issue #2: all three numeric models must be found without adding the fallback, got " + paper.size());
        var thresholds = List.of(1002.0F, 1004.0F, 23000.0F);
        var names = List.of("custom/decorations/book_1", "custom/decorations/book_stack_1", "item/paper");
        for (int i = 0; i < paper.size(); i++) {
            var stack = paper.get(i);
            var data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
            require(data != null && data.floats().equals(List.of(thresholds.get(i))),
                    "Issue #2: numeric threshold must become the first CMD float, variant " + i);
            require(data.strings().isEmpty() && data.flags().isEmpty() && data.colors().isEmpty(),
                    "Issue #2: numeric selectors must not populate other CMD components, variant " + i);
            require(names.get(i).equals(stack.get(DataComponents.ITEM_NAME).getString()),
                    "Issue #2: each preview must retain its model path, variant " + i);
        }
    }

    private static List<String> strings(ItemStack item) {
        var data = item.get(DataComponents.CUSTOM_MODEL_DATA);
        return data == null ? List.of() : data.strings();
    }

    private static void moveCursor(ClientGameTestContext context, int x, int y) {
        var position = context.computeOnClient(client -> new double[]{
                (double) x * client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth(),
                (double) y * client.getWindow().getScreenHeight() / client.getWindow().getGuiScaledHeight()
        });
        context.getInput().setCursorPos(position[0], position[1]);
        context.waitTick();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
