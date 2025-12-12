package com.abaan404.boatrace.screen;

import java.util.Optional;

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
import net.minecraft.block.NoteBlock;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
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

    public PitBoxGui(ServerPlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X3, player, false);

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
        this.setTitle(Text.of("PitBox"));
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
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.UI, 1.0f,
                    NoteBlock.getNotePitch(12));
        }

        if (state == State.FAIL) {
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), SoundCategory.UI, 1.0f,
                    NoteBlock.getNotePitch(12));
        }

        if (state == State.SUCCESS) {
            try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
                EventResult result = invokers.get(PlayerPitSuccess.EVENT).onPitSuccess(player);
                if (result == EventResult.ALLOW) {
                    player.sendMessage(TextUtils.chatPitTime(this.duration, true));
                } else {
                    player.sendMessage(TextUtils.chatPitTime(this.duration, false));
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
        this.duration += this.player.getWorld().getTickManager().getMillisPerTick();

        Countdown.TickResult result = this.countdown.tick(this.player.getWorld());
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
    public boolean onClick(int index, ClickType type, SlotActionType action, GuiElementInterface element) {
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
                .setItemName(Text.literal("Wait for go..."))
                .build()),

        /**
         * Ready to click and exit.
         */
        READY(new GuiElementBuilder()
                .setItem(Items.LIME_STAINED_GLASS_PANE)
                .setItemName(Text.literal("GO!"))
                .build()),

        /**
         * Clicked too early.
         */
        FAIL(new AnimatedGuiElementBuilder()
                .setItem(Items.ORANGE_STAINED_GLASS_PANE)
                .setItemName(Text.literal("Too Early"))
                .saveItemStack()
                .setItem(Items.YELLOW_STAINED_GLASS_PANE)
                .setItemName(Text.literal("Too Early"))
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
