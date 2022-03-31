package tk.avicia.avomod.events;

import net.minecraft.util.text.TextFormatting;
import tk.avicia.avomod.Avomod;
import tk.avicia.avomod.renderer.Element;
import tk.avicia.avomod.renderer.MultipleElements;
import tk.avicia.avomod.renderer.Rectangle;
import tk.avicia.avomod.renderer.Text;
import tk.avicia.avomod.utils.*;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

public class AttacksMenu {
    public static HashMap<String, ScreenCoordinates> attackCoordinates = new HashMap<>();
    public static HashMap<String, Tuple<String, Long>> savedDefenses = new HashMap<>();

    public static MultipleElements getElementsToDraw(List<String> upcomingAttacks, boolean sample) {
        if (upcomingAttacks.size() == 0) {
            BeaconManager.soonestTerritory = null;
            BeaconManager.soonestTerritoryLocation = null;
            BeaconManager.compassTerritory = null;
            BeaconManager.compassLocation = null;

            return null;
        }

        if (!TerritoryData.hasValues()) {
            TerritoryData.updateTerritoryData();
        }

        List<Tuple<String, String>> upcomingAttacksSplit = new ArrayList<>();
        List<String> upcomingAttackTerritories = new ArrayList<>();

        for (String upcomingAttack : upcomingAttacks) {
            String upcomingAttackUnformatted = TextFormatting.getTextWithoutFormattingCodes(upcomingAttack);
            if (upcomingAttackUnformatted == null) return null;

            String[] words = upcomingAttackUnformatted.split(" ");
            if (words.length < 3) return null;

            String time = words[1];
            String territory = String.join(" ", Arrays.copyOfRange(words, 2, words.length));

            upcomingAttacksSplit.add(new Tuple<>(time, territory));
            upcomingAttackTerritories.add(territory);
        }

        List<String> terrsToRemove = new ArrayList<>();
        for (Map.Entry<String, Tuple<String, Long>> savedDefense : savedDefenses.entrySet()) {
            if (!upcomingAttackTerritories.contains(savedDefense.getKey())) {
                terrsToRemove.add(savedDefense.getKey());
            }
        }

        if (!sample) {
            for (String terrToRemove : terrsToRemove) {
                savedDefenses.remove(terrToRemove);
                attackCoordinates.remove(terrToRemove);

                if (terrToRemove.equals(BeaconManager.compassTerritory)) {
                    BeaconManager.compassTerritory = null;
                    BeaconManager.compassLocation = null;
                }
            }
        }

        upcomingAttacksSplit.sort((o1, o2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                Date d1 = sdf.parse(o1.x);
                Date d2 = sdf.parse(o2.x);
                return (int) (d1.getTime() - d2.getTime());
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });

        if (!sample) {
            if (!upcomingAttacksSplit.get(0).y.equals(BeaconManager.soonestTerritory)) {
                BeaconManager.soonestTerritory = upcomingAttacksSplit.get(0).y;
                BeaconManager.soonestTerritoryLocation = Avomod.territoryData.getMiddleOfTerritory(upcomingAttacksSplit.get(0).y);
            }
        }

        int xPos = Avomod.getMC().player.getPosition().getX();
        int zPos = Avomod.getMC().player.getPosition().getZ();
        String currentTerritory = Avomod.territoryData.coordinatesInTerritory(new Tuple<>(xPos, zPos));

        final int rectangleHeight = 12;
        final float scale = 1F;
        ArrayList<Element> elementsList = new ArrayList<>();
        float y = Utils.getStartY("attacksMenu", upcomingAttacksSplit.size(), scale);

        for (Tuple<String, String> attack : upcomingAttacksSplit) {
            Tuple<String, Long> savedDefense = savedDefenses.get(attack.y);
            int minutes = Integer.parseInt(attack.x.split(":")[0]);
            int seconds = Integer.parseInt(attack.x.split(":")[1]);
            Long warTimestamp = (minutes * 60000L + seconds * 1000L) + System.currentTimeMillis();

            if (sample) {
                savedDefense = new Tuple<>("Retrieving...", warTimestamp);
            } else if (savedDefense == null || Math.abs(savedDefense.y - warTimestamp) > 10000) {
                if (System.currentTimeMillis() - AttackedTerritoryDifficulty.currentTime < 5000 && attack.y.equals((AttackedTerritoryDifficulty.currentTerritory))) {
                    savedDefense = new Tuple<>(AttackedTerritoryDifficulty.currentDefense, (AttackedTerritoryDifficulty.currentTimer * 60000L) + System.currentTimeMillis());
                } else {
                    savedDefense = new Tuple<>(TerritoryData.getTerritoryDefense(attack.y, warTimestamp), warTimestamp);
                }

                savedDefenses.put(attack.y, savedDefense);
            }

            String terrDefense = savedDefense.x;
            if (terrDefense.equals("Low") || terrDefense.equals("Very Low")) {
                terrDefense = TextFormatting.GREEN + terrDefense;
            } else if (terrDefense.equals("Medium")) {
                terrDefense = TextFormatting.YELLOW + terrDefense;
            } else {
                terrDefense = TextFormatting.RED + terrDefense;
            }

            String message = TextFormatting.GOLD + attack.y + " (" + terrDefense + TextFormatting.GOLD + ") " + TextFormatting.AQUA + attack.x;
            if (attack.y.equals(currentTerritory)) {
                message = TextFormatting.LIGHT_PURPLE + "" + TextFormatting.BOLD + attack.y + TextFormatting.RESET + TextFormatting.GOLD + " (" + terrDefense + TextFormatting.GOLD + ") " + TextFormatting.AQUA + attack.x;
            }

            int rectangleWidth = Avomod.getMC().fontRenderer.getStringWidth(message) + 4;
            float x = Utils.getStartX("attacksMenu", rectangleWidth, scale);

            elementsList.add(new Rectangle(x, y, rectangleWidth, rectangleHeight, new Color(100, 100, 100, 100)));
            elementsList.add(new Text(message, x + 2, y + 2, new Color(255, 170, 0)));
            attackCoordinates.put(attack.y, new ScreenCoordinates(x, y, x + rectangleWidth, y + rectangleHeight));
            y += rectangleHeight;
        }


        return new MultipleElements("attacksMenu", 1F, elementsList);
    }
}
