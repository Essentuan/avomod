package tk.avicia.avomod.commands;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import tk.avicia.avomod.Avomod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public abstract class PlayerTabCompletionCommand extends Command {
    // extend this instead of command if the command should tab complete player names e.g. /am cc <player>
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        JsonArray onlineWorldPlayers = null;
        List<String> players = new ArrayList<>();
        try {
            Avomod.onlinePlayers.updateData();
            String name = Avomod.getMC().getConnection().getPlayerInfo(UUID.fromString("16ff7452-714f-3752-b3cd-c3cb2068f6af")).getDisplayName().getUnformattedText();
            String currentWorld = name.substring(name.indexOf("[") + 1, name.indexOf("]"));
            onlineWorldPlayers = Avomod.onlinePlayers.getWorldPlayers(currentWorld);
        } catch (NullPointerException | StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        if (onlineWorldPlayers != null) {
            for (JsonElement onlineWorldPlayer : onlineWorldPlayers) {
                players.add(onlineWorldPlayer.getAsString());
            }
        } else {
            // If you are not in a wynncraft world, or the api is broken, it will try to read from the tablist instead
            // it's very scuffed in a world because of wynncraft's custom tablist.
            Collection<NetworkPlayerInfo> networkPlayers = Avomod.getMC().getConnection().getPlayerInfoMap();
            for (NetworkPlayerInfo networkPlayer : networkPlayers) {
                String username = networkPlayer.getDisplayName().getUnformattedText().replaceAll("\u00A7.", "");
                if (username.length() > 0 && !username.contains(" ")) {
                    players.add(username);
                }
            }
        }
        return getListOfStringsMatchingLastWord(args, players);
    }
}
