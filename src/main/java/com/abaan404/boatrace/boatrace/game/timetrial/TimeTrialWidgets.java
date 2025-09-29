package com.abaan404.boatrace.boatrace.game.timetrial;

import java.util.stream.Collectors;

import com.abaan404.boatrace.boatrace.game.maps.TrackMap;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public final class TimeTrialWidgets {
    private final GlobalWidgets widgets;
    private final TrackMap.Meta meta;

    private final Object2ObjectMap<PlayerRef, SidebarWidget> sidebars = new Object2ObjectOpenHashMap<>();

    public TimeTrialWidgets(GlobalWidgets widgets, TrackMap map) {
        this.meta = map.getMetadata();
        this.widgets = widgets;
    }

    /**
     * Tick the UI for the player.
     *
     * @param gameSpace    The current game space.
     * @param stageManager The game's state.
     * @param leaderboard  The track's leaderboard.
     */
    public void tick(GameSpace gameSpace, TimeTrialStageManager stageManager, TimeTrialLeaderboard leaderboard) {
        this.tickSidebar(gameSpace, stageManager, leaderboard);
        this.tickActionBar(gameSpace, stageManager);
    }

    private void tickActionBar(GameSpace gameSpace, TimeTrialStageManager stageManager) {
        gameSpace.getPlayers().forEach(player -> {
            OverlayMessageS2CPacket a = new OverlayMessageS2CPacket(Text.literal("testing"));

            player.networkHandler.send(a, null);
        });
    }

    private void tickSidebar(GameSpace gameSpace, TimeTrialStageManager stageManager,
            TimeTrialLeaderboard leaderboard) {
        gameSpace.getPlayers().forEach(player -> {
            PlayerRef ref = PlayerRef.of(player);

            if (!sidebars.containsKey(ref)) {
                Text title = Text.literal("Boat").formatted(Formatting.RED, Formatting.BOLD)
                        .append(Text.literal("Race").formatted(Formatting.WHITE, Formatting.ITALIC))
                        .append(Text.literal(" - "))
                        .append(Text.literal("TimeTrial").formatted(Formatting.DARK_GRAY));

                SidebarWidget newSidebar = this.widgets.addSidebar(
                        Text.literal("    ").append(title).append(Text.literal("    ")),
                        p -> PlayerRef.of(p).equals(ref));
                newSidebar.addPlayer(player);

                this.sidebars.put(ref, newSidebar);
            }

            SidebarWidget sidebar = this.sidebars.get(ref);

            sidebar.set(content -> {
                Text nameText = Text.literal(this.meta.name()).formatted(Formatting.GOLD, Formatting.UNDERLINE,
                        Formatting.BOLD);
                Text authorsText = Text.literal(" - By " + this.meta.authors().stream()
                        .collect(Collectors.joining(", ")))
                        .formatted(Formatting.GRAY, Formatting.ITALIC);

                Text paddingText = Text.literal("");

                Text timerText = Text.literal(formatTime(stageManager.getTimer(player)));
                Text lapsText = Text.literal(String.valueOf(stageManager.getLaps(player)));

                content.add(paddingText);

                content.add(nameText);
                content.add(authorsText);

                content.add(Text.literal(""));

                content.add(lapsText);
                content.add(timerText);

                stageManager.getSplits(player)
                        .doubleStream()
                        .forEach(split -> {
                            Text splitText = Text.literal(formatTime((float) split));
                            content.add(splitText);
                        });
            });
        });
    }

    private String formatTime(float time) {
        final long totalMillis = (long) time;
        final long totalSeconds = totalMillis / 1000;
        final long millis = totalMillis % 1000;

        final long hours = totalSeconds / 3600;
        final long minutes = (totalSeconds % 3600) / 60;
        final long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, millis);
        } else {
            return String.format("%02d:%02d.%03d", minutes, seconds, millis);
        }
    }
}
