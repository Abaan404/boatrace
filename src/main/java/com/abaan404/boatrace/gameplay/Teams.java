package com.abaan404.boatrace.gameplay;

import java.util.Set;

import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.BoatRacePlayer;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class Teams {
    private final TeamManager teams;
    private final BoatRaceConfig.Team config;

    private int nextTeamId = 0;
    private int nextColorId = 0;

    public Teams(BoatRaceConfig.Team config, TeamManager teams) {
        this.config = config;
        this.teams = teams;
    }

    public Teams(Teams other, TeamManager teams) {
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
    public void assign(BoatRacePlayer player) {
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
    public void unassign(BoatRacePlayer player) {
        this.teams.removePlayer(player.ref());
    }

    /**
     * Get the team for this player.
     *
     * @param player The player.
     * @return The player's team.
     */
    public GameTeamKey getTeamFor(BoatRacePlayer player) {
        return this.teams.teamFor(player.ref());
    }

    /**
     * Get a list of players in this team.
     *
     * @param key     The team's key.
     * @param players The list of all participants to reference from.
     * @return The list of every player in this team.
     */
    public Set<BoatRacePlayer> getPlayersIn(GameTeamKey key, Iterable<BoatRacePlayer> players) {
        Set<PlayerRef> members = this.teams.allPlayersIn(key);
        Set<BoatRacePlayer> bMembers = new ObjectOpenHashSet<>();

        for (BoatRacePlayer player : players) {
            if (members.contains(player.ref())) {
                bMembers.add(player);
            }
        }

        return bMembers;
    }

    /**
     * Get the team config for this player.
     *
     * @param player The player.
     * @return Their team's config.
     */
    public GameTeamConfig getConfig(BoatRacePlayer player) {
        return this.teams.getTeamConfig(this.getTeamFor(player));
    }

    /**
     * Get the team config for this team.
     *
     * @param team the team.
     * @return Their teams' config.
     */
    public GameTeamConfig getConfig(GameTeamKey team) {
        return this.teams.getTeamConfig(team);
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
                .append(Text.literal("›").formatted(this.indexToColor(color1), Formatting.BOLD))
                .append(Text.literal("›").formatted(this.indexToColor(color2), Formatting.BOLD))
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
