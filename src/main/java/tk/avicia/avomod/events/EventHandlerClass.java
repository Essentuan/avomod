package tk.avicia.avomod.events;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.BossInfo;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import tk.avicia.avomod.Avomod;
import tk.avicia.avomod.utils.*;
import tk.avicia.avomod.webapi.UpdateChecker;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class EventHandlerClass {
    private int tick = 0;
    private boolean guiOpen = false;
    private boolean guiJustOpened = false;

    @SubscribeEvent
    public void onChatEvent(ClientChatReceivedEvent event) {
        if (Avomod.getConfigBoolean("disableAll")) return;

        if (Avomod.getConfigBoolean("revealNicks")) {
            ChatUtils.execute(event);
        }

        String message = TextFormatting.getTextWithoutFormattingCodes(event.getMessage().getUnformattedText());
        if (message == null) return;

        boolean bankMessage = message.startsWith("[INFO]") && message.contains("Guild Bank");
        if (bankMessage && Avomod.getConfigBoolean("filterBankMessages")) {
            event.setCanceled(true);
        }

        KeyBinding sneakKeyBind = Avomod.getMC().gameSettings.keyBindSneak;
        if (message.contains("Press SHIFT to continue") && Avomod.getConfigBoolean("skipDialogue")) {
            Thread thread = new Thread(() -> {
                try {
                    KeyBinding.setKeyBindState(sneakKeyBind.getKeyCode(), true);
                    Thread.sleep(100);
                    KeyBinding.setKeyBindState(sneakKeyBind.getKeyCode(), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        }

        if (message.startsWith("[INFO]") && message.contains("resources") && Avomod.getConfigBoolean("filterResourceMessages")) {
            event.setCanceled(true);
        }

        if (message.contains("The war for") && message.endsWith("minutes.")) {
            String territory = message.split("for ")[1].split(" will")[0];
            AttackedTerritoryDifficulty.receivedChatMessage(message, territory);
        }

        if (message.trim().startsWith("Welcome to Wynncraft")) {
            Thread thread = new Thread(UpdateChecker::checkUpdate);
            thread.start();
        }
        if (Avomod.getConfigBoolean("dpsInWars") && System.currentTimeMillis() - WarDPS.lastTimeInWar < 5000 && message.contains(WarDPS.previousTerritoryName)) {
            // If you saw a tower health bar less than 5 seconds ago (if you're in a war)
            if (message.startsWith("[WAR] You have taken control of ")) {
                WarDPS.warEnded(true);
            }
            if (message.startsWith("[WAR] Your guild has lost the war for ") || message.startsWith("Your active attack was canceled and refunded to your headquarter")) {
                WarDPS.warEnded(false);
            }
        }
    }

    @SubscribeEvent
    public void onGuiMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (Avomod.getConfigBoolean("disableAll")) return;

        if (Avomod.getMC().player == null) {
            return;
        }

        Container openContainer = Avomod.getMC().player.openContainer;
        ScaledResolution scaledResolution = new ScaledResolution(Avomod.getMC());
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();
        int scaleFactor = scaledResolution.getScaleFactor();
        int scaledMouseX = Mouse.getX() / scaleFactor;
        int scaledMouseY = Mouse.getY() / scaleFactor;
        int slotDimensions = 18;

        if (openContainer instanceof ContainerPlayer && event.getGui() instanceof GuiInventory && Avomod.getConfigBoolean("disableMovingArmor")) {
            if (Mouse.getEventButtonState() && scaledMouseY > (screenHeight / 2) - slotDimensions && scaledMouseX < (screenWidth / 2.0) - (4.5 * slotDimensions) + (4 * slotDimensions)) {
                event.setCanceled(true);
            }
        } else if (openContainer instanceof ContainerChest) {
            String containerName = ((ContainerChest) openContainer).getLowerChestInventory().getName();
            if (containerName.equals("Trade Market")) {
                TradeMarketAutoSearch.execute(event, openContainer, screenWidth, screenHeight, slotDimensions, scaleFactor);
            }
        }

        if (event.getGui() instanceof GuiChat && Mouse.getEventButtonState()) {
            for (Map.Entry<String, ScreenCoordinates> attackCoordinates : AttacksMenu.attackCoordinates.entrySet()) {
                if (attackCoordinates.getValue().mouseIn(scaledMouseX, screenHeight - scaledMouseY)) {
                    Coordinates territoryLocation = Avomod.territoryData.getMiddleOfTerritory(attackCoordinates.getKey());
                    BeaconManager.compassLocation = territoryLocation;

                    if (BeaconManager.compassLocation != null) {
                        Avomod.getMC().player.sendMessage(new TextComponentString("A blue beacon beam has been created in " + attackCoordinates.getKey() + " at (" + territoryLocation.getX() + ", " + territoryLocation.getZ() + ")"));
                        BeaconManager.compassTerritory = attackCoordinates.getKey();
                    } else {
                        Avomod.getMC().player.sendMessage(new TextComponentString("Not a correct territory name (probably too long for the scoreboard)"));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onGuiKeyInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (Avomod.getConfigBoolean("disableAll")) return;

        if (Avomod.getMC().player == null) {
            return;
        }
        if (Avomod.getConfigBoolean("disableMovingArmor")) {
            Container openContainer = Avomod.getMC().player.openContainer;

            if (openContainer instanceof ContainerPlayer) {
                int screenWidth = Avomod.getMC().displayWidth;
                int screenHeight = Avomod.getMC().displayHeight;
                int slotDimensions = 36;

                if (Keyboard.getEventKey() == 16 && Mouse.getY() > (screenHeight / 2) - slotDimensions && Mouse.getX() < (screenWidth / 2.0) - (4.5 * slotDimensions) + (4 * slotDimensions)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (Avomod.getConfigBoolean("disableAll")) return;

        if (Avomod.getMC().player == null || event.getGui() == null) {
            return;
        }

        Container openContainer = Avomod.getMC().player.openContainer;
        if (openContainer instanceof ContainerChest) {
            InventoryBasic lowerInventory = (InventoryBasic) ((ContainerChest) openContainer).getLowerChestInventory();
            String containerName = lowerInventory.getName();
            if (containerName.contains("Loot Chest")) {
                AverageLevel.execute(event, lowerInventory);
            } else if (containerName.contains("Attacking: ")) {
                try {
                    AttackedTerritoryDifficulty.inMenu();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        guiJustOpened = false;
    }

    @SubscribeEvent
    public void guiInitialize(GuiScreenEvent.InitGuiEvent.Post event) {
        if (Avomod.getConfigBoolean("disableAll")) return;

        if (Avomod.getMC().player == null || event.getGui() == null) {
            return;
        }

        Container openContainer = Avomod.getMC().player.openContainer;
        if (openContainer instanceof ContainerChest) {
            InventoryBasic lowerInventory = (InventoryBasic) ((ContainerChest) openContainer).getLowerChestInventory();
            String containerName = lowerInventory.getName();
            if (containerName.contains("Loot Chest")) {
                ScaledResolution scaledResolution = new ScaledResolution(Avomod.getMC());
                int screenHeight = scaledResolution.getScaledHeight();
                int scaleFactor = scaledResolution.getScaleFactor();
                int scaledMouseX = Mouse.getX() / scaleFactor;
                int scaledMouseY = Mouse.getY() / scaleFactor;

                if (guiJustOpened && scaledMouseY != Math.ceil(screenHeight / 2.0)) {
                    Mouse.setCursorPosition(scaledMouseX * scaleFactor, ((int) Math.ceil(screenHeight / 2.0)) * scaleFactor);
                }
            }
        }
    }

    @SubscribeEvent
    public void guiJustOpened(GuiOpenEvent event) {
        guiJustOpened = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (Avomod.getConfigBoolean("disableAll")) return;

        if (Avomod.getMC().player == null) {
            return;
        }
        Container openContainer = Avomod.getMC().player.openContainer;

        if (!(openContainer instanceof ContainerChest)) {
            AverageLevel.isChestNew = true;
        }
        ToolTipState.isTooltipRendering = false;

        tick++;
        if (tick % 60000 == 0) {
            TerritoryData.updateTerritoryData();
        }

        guiOpen = false;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (Avomod.getConfigBoolean("disableAll")) return;

        for (Keybind keybind : Avomod.keybinds.values()) {
            if (keybind.isPressed()) {
                Avomod.getMC().player.sendChatMessage("/" + keybind.getCommandToRun());
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (Avomod.guiToDraw != null) {
            Avomod.getMC().displayGuiScreen(Avomod.guiToDraw);
            Avomod.guiToDraw = null;
        }

        if (Avomod.getConfigBoolean("disableAll")) return;

        if (Avomod.getMC().gameSettings.keyBindPlayerList.isKeyDown()) {
            WorldInfo.draw();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void renderOverlay(RenderGameOverlayEvent.Chat event) {
        if (Avomod.getConfigBoolean("disableAll")) return;

        // The Chat RenderGameOverlayEvent renders stuff normally, it disappears in f1, you can see it when your
        // inventory is open and you can make stuff transparent
        List<String> upcomingAttacks = Utils.getUpcomingAttacks();
        if (Avomod.getConfigBoolean("attacksMenu")) {
            AttacksMenu.draw(upcomingAttacks);
        }
    }

    @SubscribeEvent
    public void renderWorld(RenderWorldLastEvent e) {
        if (Avomod.getConfigBoolean("disableAll")) return;

        if (BeaconManager.compassLocation != null) {
            BeaconManager.drawBeam(BeaconManager.compassLocation, new Color(0, 50, 150, 255), e.getPartialTicks(), BeaconManager.compassTerritory);
        }

        if (BeaconManager.soonestTerritoryLocation != null && Avomod.getConfigBoolean("greenBeacon")) {
            BeaconManager.drawBeam(BeaconManager.soonestTerritoryLocation, new Color(50, 150, 0, 255), e.getPartialTicks(), BeaconManager.soonestTerritory);
        }

    }

    @SubscribeEvent
    public void onScreenDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (Avomod.getConfigBoolean("disableAll")) return;

        if (Avomod.getMC().player == null || event.getGui() == null) {
            return;
        }

        // Stuff you draw here appears above the chest slots and behind the items z-levels, so you can use this for
        // highlights
        if (!(event.getGui() instanceof GuiChat)) {
            guiOpen = true;
        }
    }

    @SubscribeEvent
    public void onTooltipRender(RenderTooltipEvent.PostBackground event) {
        if (Avomod.getConfigBoolean("disableAll")) return;

        ToolTipState.isTooltipRendering = true;
        ToolTipState.toolTipX = event.getX();
        ToolTipState.toolTipY = event.getY();
        ToolTipState.toolTipWidth = event.getWidth();
        ToolTipState.toolTipHeight = event.getHeight();
    }

    @SubscribeEvent
    public void bossInfo(RenderGameOverlayEvent.BossInfo event) {
        if (Avomod.getConfigBoolean("disableAll")) return;

        try {
            BossInfo bossInfo = event.getBossInfo();
            String bossbarName = bossInfo.getName().getFormattedText();
            String[] bossbarWords = bossbarName.split(" ");
            if (Avomod.getConfigBoolean("dpsInWars") && bossbarName.contains("Tower") && bossbarWords.length >= 6) {
                try {
                    WarDPS.execute(bossbarWords);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void entityRender(RenderLivingEvent.Pre<EntityLivingBase> event) {
        if (!Avomod.getConfigBoolean("hideEntitiesInWar") || (System.currentTimeMillis() - WarDPS.lastTimeInWar) > 2000)
            return;

        EntityLivingBase entity = event.getEntity();
        if (entity.getTeam() == null) {
            event.setCanceled(true);
        }
    }
}