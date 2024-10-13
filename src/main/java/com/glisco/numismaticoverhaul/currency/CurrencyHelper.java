package com.glisco.numismaticoverhaul.currency;

import com.glisco.numismaticoverhaul.item.CoinItem;
import com.glisco.numismaticoverhaul.item.CurrencyItem;
import com.glisco.numismaticoverhaul.item.MoneyBagItem;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.KeyedEndec;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.village.TradedItem;

import java.util.ArrayList;
import java.util.List;

public class CurrencyHelper {

    public static final KeyedEndec<Long> VALUE = new KeyedEndec<>("Value", Endec.LONG, 0L);
    public static final KeyedEndec<long[]> VALUES = new KeyedEndec<>("Values", BuiltInEndecs.LONG_ARRAY, new long[]{});

    /**
     * Checks how much money a player has as coin items in their inventory
     *
     * @param player The player to operate on (duh)
     * @param remove Whether to remove all coins from the player in the process
     * @return The amount of currency contained in the player's inventory
     */
    public static long getMoneyInInventory(PlayerEntity player, boolean remove) {

        long value = 0;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isCombined(stack)) continue;
            if (!(stack.getItem() instanceof CurrencyItem currencyItem)) continue;

            value += currencyItem.getValue(stack);

            if (remove) player.getInventory().removeOne(stack);
        }

        return value;
    }

    public static int getValue(List<ItemStack> stacks) {
        return stacks.stream().mapToInt(stack -> {
            if (stack == null) return 0;

            if (isCombined(stack)) return 0;
            if (!(stack.getItem() instanceof CurrencyItem currencyItem)) return 0;
            return (int) currencyItem.getValue(stack);
        }).sum();
    }

    public static void offerAsCoins(PlayerEntity player, long value) {
        for (ItemStack itemStack : CurrencyConverter.getAsValidStacks(value)) {
            player.getInventory().offerOrDrop(itemStack);
        }
    }

    public static boolean deductFromInventory(PlayerEntity player, long value) {
        long presentInInventory = getMoneyInInventory(player, false);
        if (presentInInventory < value) return false;

        getMoneyInInventory(player, true);

        offerAsCoins(player, presentInInventory - value);
        return true;
    }

    /**
     * Converts an amount of currency to a list of {@link ItemStack},
     * prefers {@link CoinItem} but may fall back to {@link MoneyBagItem}
     *
     * @param value     The currency value to convert
     * @param maxStacks The maximum amount of stacks the result may have
     * @return The List of {@link ItemStack}
     */
    public static List<ItemStack> getAsStacks(long value, int maxStacks) {

        List<ItemStack> stacks = new ArrayList<>();
        List<ItemStack> rawStacks = CurrencyConverter.getAsValidStacks(value);

        if (rawStacks.size() <= maxStacks) {
            stacks.addAll(rawStacks);
        } else {
            stacks.add(MoneyBagItem.create(value));
        }

        return stacks;
    }

    public static ItemStack getClosest(long value) {
        long[] values = CurrencyResolver.splitValues(value);

        for (int i = 0; i < 2; i++) {
            if (values[i + 1] == 0) break;
            values[i + 1] += Math.round(values[i] / 100f);
            values[i] = 0;
        }

        return CurrencyConverter.getAsItemStackList(CurrencyResolver.combineValues(values)).get(0);
    }

    public static TradedItem getClosestTradeItem(long price) {
        var closestPriceStack = CurrencyHelper.getClosest(price);
        return new TradedItem(closestPriceStack.getItem(), closestPriceStack.getCount());
    }

    private static boolean isCombined(ItemStack stack) {
        return stack.get(MoneyBagItem.COMBINED);
    }

    public static long[] getValues(ItemStack stack) {
        return stack.get(VALUES);
    }
}
