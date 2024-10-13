package com.glisco.numismaticoverhaul.mixin;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.block.ShopMerchant;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.glisco.numismaticoverhaul.currency.CurrencyHelper;
import com.glisco.numismaticoverhaul.item.CoinItem;
import com.glisco.numismaticoverhaul.item.NumismaticOverhaulItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradedItem;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantScreenHandler.class)
public class MerchantScreenHandlerMixin {

    @Shadow
    @Final
    private Merchant merchant;

    //Autofill with coins from the player's purse if the trade requires it
    //Injected at TAIL to let normal autofill run and fill up if anything is missing
    @Inject(method = "autofill", at = @At("TAIL"))
    public void autofillOverride(int slot, TradedItem tradedItem, CallbackInfo ci) {
        var stack = tradedItem.itemStack();
        MerchantScreenHandler handler = (MerchantScreenHandler) (Object) this;
        CurrencyComponent playerBalance = ModComponents.CURRENCY.get(((PlayerInventory) handler.getSlot(3).inventory).player);

        if (stack.getItem() instanceof CoinItem) {
            numismatic$autofillWithCoins(slot, stack, handler, playerBalance);
        } else if (stack.getItem() == NumismaticOverhaulItems.MONEY_BAG) {
            autofillWithMoneyBag(slot, stack, handler, playerBalance);
        }

        if (slot == 1) playerBalance.commitTransactions();
    }

    private static void numismatic$autofillWithCoins(int slot, ItemStack stack, MerchantScreenHandler handler, CurrencyComponent playerBalance) {
        //See how much is required and how much was already autofilled
        long requiredCurrency = ((CoinItem) stack.getItem()).currency.getRawValue(stack.getCount());
        long presentCurrency = ((CoinItem) stack.getItem()).currency.getRawValue(handler.getSlot(slot).getStack().getCount());

        if (requiredCurrency <= presentCurrency) return;

        //Find out how much we still need to fill
        long neededCurrency = requiredCurrency - presentCurrency;

        //Is that even possible?
        if (!(neededCurrency <= playerBalance.getValue())) return;

        playerBalance.pushTransaction(-neededCurrency);

        handler.slots.get(slot).setStack(stack.copy());
    }

    private static void autofillWithMoneyBag(int slot, ItemStack stack, MerchantScreenHandler handler, CurrencyComponent playerBalance) {
        if (ItemStack.areEqual(stack, handler.getSlot(slot).getStack())) return;
        PlayerEntity player = ((PlayerInventory) handler.getSlot(3).inventory).player;

        //See how much is required and how much in present in the player's inventory
        long requiredCurrency = NumismaticOverhaulItems.MONEY_BAG.getValue(stack);
        long availableCurrencyInPlayerInventory = CurrencyHelper.getMoneyInInventory(player, false);

        //Find out how much we still need to fill
        long neededCurrency = requiredCurrency - availableCurrencyInPlayerInventory;

        //Is that even possible?
        if (neededCurrency > playerBalance.getValue()) return;

        if (neededCurrency <= 0) {
            CurrencyHelper.deductFromInventory(player, requiredCurrency);
        } else {
            CurrencyHelper.deductFromInventory(player, availableCurrencyInPlayerInventory);
            playerBalance.pushTransaction(-neededCurrency);
        }

        handler.slots.get(slot).setStack(stack.copy());
    }

    @Inject(method = "playYesSound", at = @At("HEAD"), cancellable = true)
    public void checkForEntityOnYes(CallbackInfo ci) {
        if (!(merchant instanceof Entity)) ci.cancel();
    }

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    public void thwartTaxEvasion(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (!(this.merchant instanceof ShopMerchant shopMerchant)) return;

        var shop = shopMerchant.shop();
        if (shop.getWorld().getBlockEntity(shop.getPos()) != shop || shop.getPos().getSquaredDistance(player.getX(), player.getY(), player.getZ()) > 100) {
            cir.setReturnValue(false);
        }
    }

}
