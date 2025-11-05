package com.abaan404.boatrace.game;

import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.BoatRacePlayer;

import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class BoatRaceTeams {
    private final TeamManager teams;
    private final BoatRaceConfig.Team config;

    private int nextTeamId = 0;
    private int nextColorId = 0;

    public BoatRaceTeams(BoatRaceConfig.Team config, TeamManager teams) {
        this.config = config;
        this.teams = teams;
    }

    public BoatRaceTeams(BoatRaceTeams other, TeamManager teams) {
        // copy team state for a new team manager
        this.config = other.config;
        this.nextTeamId = other.nextTeamId;
        this.nextColorId = other.nextColorId;
        this.teams = teams;

        // copy teams into this state
        for (GameTeam team : other.teams) {
            this.teams.addTeam(team);

            for (PlayerRef player : other.teams.allPlayersIn(team.key())) {
                this.teams.addPlayerTo(player, team.key());
            }
        }
    }

    /**
     * Assign a player to a team.
     *
     * @param player The player to assign.
     */
    public void add(BoatRacePlayer player) {
        GameTeamKey key = this.teams.getSmallestTeam();

        // create a new team if the team doesnt exist or the team is already full
        if (key == null || this.teams.allPlayersIn(key).size() >= this.config.size()) {
            key = this.generate();
        }

        this.teams.addPlayerTo(player.ref(), key);
    }

    /**
     * Unassign a player from a team.
     *
     * @param player The player to unassign.
     */
    public void remove(BoatRacePlayer player) {
        this.teams.removePlayer(player.ref());
    }

    /**
     * Generate a new team.
     *
     * @return A new team key
     */
    private GameTeamKey generate() {
        int color1 = this.nextColorId % DyeColor.values().length;
        int color2 = this.nextColorId / DyeColor.values().length;

        // initial cycle of colors will be paired together
        if (color2 == 0) {
            color2 = color1;
        }

        // if the pairs are same, rollover since theyve been taken already (except 0)
        else if (color1 == color2) {
            this.nextColorId++;
            return generate();
        }

        GameTeamKey key = new GameTeamKey(String.valueOf(this.nextTeamId));

        Text prefix = Text.empty()
                .append(Text.literal("⟫").formatted(this.indexToColor(color1), Formatting.BOLD))
                .append(Text.literal("⟫").formatted(this.indexToColor(color2), Formatting.BOLD))
                .append(" ");

        this.teams.addTeam(key, GameTeamConfig.builder()
                .setPrefix(prefix)
                .build());

        this.nextTeamId++;
        this.nextColorId++;

        return key;
    }

    /**
     * Map a dye index to a unique formatting.
     *
     * @param index The dye index.
     * @return A text formatting color.
     */
    private Formatting indexToColor(int index) {
        return switch (DyeColor.byIndex(index)) {
            case WHITE -> Formatting.WHITE;
            case ORANGE -> Formatting.GOLD;
            case MAGENTA -> Formatting.DARK_RED;
            case LIGHT_BLUE -> Formatting.AQUA;
            case YELLOW -> Formatting.YELLOW;
            case LIME -> Formatting.GREEN;
            case PINK -> Formatting.LIGHT_PURPLE;
            case GRAY -> Formatting.DARK_GRAY;
            case LIGHT_GRAY -> Formatting.GRAY;
            case CYAN -> Formatting.DARK_AQUA;
            case PURPLE -> Formatting.DARK_PURPLE;
            case BLUE -> Formatting.BLUE;
            case BROWN -> Formatting.DARK_BLUE;
            case GREEN -> Formatting.DARK_GREEN;
            case RED -> Formatting.RED;
            case BLACK -> Formatting.BLACK;
        };
    }
}
