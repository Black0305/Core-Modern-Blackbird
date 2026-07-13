package su.terrafirmagreg.core.mixins.client.minecraft;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import su.terrafirmagreg.core.TFGCore;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    /**
     * Prevents a client-side freeze (which forces a relog) when returning from a planet with Ad Astra.
     *
     * On a dimension change the Lander menu can receive its ClientboundContainerSetContentPacket before the
     * vehicle entity - and therefore its slots - has synced to the client. The menu then has 0 slots while the
     * server sends items, so initializeContents() calls getSlot(i) out of bounds and throws on the render
     * thread ("Error executing task on Client"), leaving the player stuck until they relog.
     *
     * Dropping the mismatched update avoids the crash; the correct contents arrive on the next sync. The guard
     * only triggers on a genuine size mismatch, so normal menus are never affected.
     */
    @Inject(method = "initializeContents", at = @At("HEAD"), cancellable = true)
    private void tfg$guardContainerDesync(int stateId, List<ItemStack> items, ItemStack carried, CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (items.size() > self.slots.size()) {
            TFGCore.LOGGER.warn(
                "[AdAstra fix] Dropped a container update ({} items for {} slots, menu {}) to avoid a client-side crash (lander sync race).",
                items.size(), self.slots.size(), self.getClass().getName());
            ci.cancel();
        }
    }
}
