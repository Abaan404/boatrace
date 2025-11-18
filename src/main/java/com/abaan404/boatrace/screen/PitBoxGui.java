package com.abaan404.boatrace.screen;

import com.abaan404.boatrace.events.PlayerPitSuccess;
import com.abaan404.boatrace.gameplay.Countdown;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.AnimatedGuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xyz.nucleoid.stimuli.EventInvokers;
import xyz.nucleoid.stimuli.Stimuli;

public class PitBoxGui extends SimpleGui {
    private Countdown countdown = new Countdown();
    private State state = State.IDLE;
    private long duration = 0;

    public PitBoxGui(ServerPlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X3, player, false);

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

        if (state == State.SUCCESS) {

            try (EventInvokers invokers = Stimuli.select().forEntity(this.player)) {
                invokers.get(PlayerPitSuccess.EVENT).onPitSuccess(this);
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
        this.countdown.setCountdown(2000, 0);
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
                this.countdown.setCountdown(2000, 0);
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
                this.countdown.setCountdown(2000, 0);
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
