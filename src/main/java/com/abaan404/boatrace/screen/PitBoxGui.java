package com.abaan404.boatrace.screen;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.NoteBlock;
import com.abaan404.boatrace.BoatRace;
import com.abaan404.boatrace.BoatRaceConfig;
import com.abaan404.boatrace.events.PlayerPitSuccess;
import com.abaan404.boatrace.gameplay.Countdown;
import com.abaan404.boatrace.utils.TextUtils;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.AnimatedGuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.plasmid.api.game.config.GameConfig;
import xyz.nucleoid.stimuli.EventInvokers;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.EventResult;

public class PitBoxGui extends SimpleGui {
    private final BoatRaceConfig.Pits config;
    private final Countdown countdown = new Countdown();

    private State state = State.IDLE;
    private long duration = 0;

    public PitBoxGui(ServerPlayer player) {
        super(MenuType.GENERIC_9x3, player, false);

        Optional<GameSpace> gameSpace = Optional.ofNullable(GameSpaceManager.get().byPlayer(player));

        BoatRaceConfig.Pits config = gameSpace.flatMap(gs -> {
            GameConfig<?> gameConfig = gs.getMetadata().sourceConfig().value();

            if (gameConfig.type().id().equals(BoatRace.TYPE.id())) {
                return ((BoatRaceConfig) gameConfig.config()).race()
                        .map(race -> race.pits());
            }

            return Optional.empty();
        }).orElse(BoatRaceConfig.Pits.DEFAULT);

        this.config = config;
        this.setTitle(Component.nullToEmpty("PitBox"));
        this.setLockPlayerInventory(true);
    }

    /**
     * Get the time taken in this pitstop.
     *
     * @return The duration in ms.
     */
    public long getDuration() {
        return this.duration;
    }

    /**
     * Set a state with its associated elements. Also triggers
     * {@link PlayerPitSuccess} if the next state was SUCCESS.
     *
     * @param state The next state.
     */
    private void setState(State state) {
        this.state = state;
        this.fillSlots(state.getElement());

        if (state == State.READY) {
            player.playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 1.0f, NoteBlock.getPitchFromNote(12));
        }

        if (state == State.FAIL) {
            player.playSound(SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 1.0f, NoteBlock.getPitchFromNote(12));
        }

        if (state == State.SUCCESS) {
            try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
                EventResult result = invokers.get(PlayerPitSuccess.EVENT).onPitSuccess(player);
                if (result == EventResult.ALLOW) {
                    player.sendSystemMessage(TextUtils.chatPitTime(this.duration, true));
                } else {
                    player.sendSystemMessage(TextUtils.chatPitTime(this.duration, false));
                }
            }
        }
    }

    /**
     * Fill every slot with this element.
     *
     * @param element The new element.
     */
    private void fillSlots(GuiElementInterface element) {
        for (int i = 0; i < this.size; i++) {
            this.setSlot(i, element);
        }
    }

    @Override
    public void beforeOpen() {
        this.setState(State.WAIT);
        this.countdown.setCountdown(config.ready());
    }

    @Override
    public void onTick() {
        this.duration += this.player.level().tickRateManager().millisecondsPerTick();

        Countdown.TickResult result = this.countdown.tick(this.player.level());
        if (result != Countdown.TickResult.FINISH) {
            return;
        }

        switch (this.state) {
            case FAIL:
                this.setState(State.WAIT);
                this.countdown.setCountdown(config.ready());
                break;

            case WAIT:
                this.setState(State.READY);
                break;

            default:
                break;
        }
    }

    @Override
    public boolean onClick(int index, ClickType type, net.minecraft.world.inventory.ClickType action, GuiElementInterface element) {
        switch (this.state) {
            case WAIT:
                this.setState(State.FAIL);
                this.countdown.setCountdown(config.failure());
                break;

            case READY:
                this.setState(State.SUCCESS);
                this.close();
                break;

            default:
                break;
        }

        return super.onClick(index, type, action, element);
    }

    @Override
    public void onClose() {
        if (this.state == State.READY) {
            this.setState(State.SUCCESS);
        }
    }

    private enum State {
        /**
         * Waiting for Ready.
         */
        WAIT(new GuiElementBuilder()
                .setItem(Items.RED_STAINED_GLASS_PANE)
                .setItemName(Component.literal("Wait for go..."))
                .build()),

        /**
         * Ready to click and exit.
         */
        READY(new GuiElementBuilder()
                .setItem(Items.LIME_STAINED_GLASS_PANE)
                .setItemName(Component.literal("GO!"))
                .build()),

        /**
         * Clicked too early.
         */
        FAIL(new AnimatedGuiElementBuilder()
                .setItem(Items.ORANGE_STAINED_GLASS_PANE)
                .setItemName(Component.literal("Too Early"))
                .saveItemStack()
                .setItem(Items.YELLOW_STAINED_GLASS_PANE)
                .setItemName(Component.literal("Too Early"))
                .saveItemStack()
                .setInterval(10)
                .build()),

        /**
         * Completed pit.
         */
        SUCCESS(new GuiElementBuilder()
                .setItem(Items.AIR)
                .build()),

        /**
         * Doing nothing.
         */
        IDLE(new GuiElementBuilder()
                .setItem(Items.AIR)
                .build());

        GuiElementInterface element;

        private State(GuiElementInterface element) {
            this.element = element;
        }

        /**
         * Get the ui elements for this state.
         *
         * @return The elements.
         */
        public GuiElementInterface getElement() {
            return this.element;
        }
    };
}
