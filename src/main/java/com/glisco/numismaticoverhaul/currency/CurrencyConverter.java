package com.glisco.numismaticoverhaul.currency;

import com.glisco.numismaticoverhaul.item.NumismaticOverhaulItems;
import net.minecraft.item.ItemStack;
import net.minecraft.village.TradedItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CurrencyConverter {

    /**
     * @return An array of 3 {@link ItemStack}, format ItemStack[]{BRONZE, SILVER, GOLD}, stack sizes can exceed 99
     */
    public static ItemStack[] getAsItemStackArray(long value) {
        ItemStack[] output = new ItemStack[]{null, null, null};

        long[] values = CurrencyResolver.splitValues(value);

        output[2] = new ItemStack(NumismaticOverhaulItems.GOLD_COIN, asInt(values[2]));
        output[1] = new ItemStack(NumismaticOverhaulItems.SILVER_COIN, asInt(values[1]));
        output[0] = new ItemStack(NumismaticOverhaulItems.BRONZE_COIN, asInt(values[0]));

        return output;
    }

    /**
     * Wrapper for {@link #getAsItemStackArray(long)} that only includes non-zero {@link ItemStack}
     *
     * @return A list of {@link ItemStack}, stack sizes can exceed 99
     */
    public static List<ItemStack> getAsItemStackList(long value) {
        List<ItemStack> list = new ArrayList<>();

        Arrays.stream(getAsItemStackArray(value)).forEach(itemStack -> {
            if (itemStack.getCount() != 0) list.add(0, itemStack);
        });

        return list;
    }

    /**
     * Wrapper for {@link #getAsItemStackArray(long)} that only includes non-zero {@link ItemStack}
     *
     * @return A list of {@link ItemStack}, stack sizes can exceed 99
     */
    public static List<ItemStack> getAsItemStackList(long[] values) {
        List<ItemStack> list = new ArrayList<>();

        for (int i = 0; i < values.length; i++) {
            if (values[i] <= 0) continue;
            list.add(0, new ItemStack(Currency.values()[i], asInt(values[i])));
        }

        return list;
    }

    /**
     * @return The amount of currency types required to represent this stack's raw value
     */
    public static int getRequiredCurrencyTypes(long value) {
        return splitAtMaxCount(getAsItemStackList(value)).size();
    }

    /**
     * Splits the provided list into another list where no stacks are over their max size
     *
     * @param input A list of {@link ItemStack} that could contain some with illegal sizes
     * @return A list where no stacks have illegal sizes, most likely bigger than input
     */
    public static List<ItemStack> splitAtMaxCount(List<ItemStack> input) {
        List<ItemStack> output = new ArrayList<>();

        for (ItemStack stack : input) {
            if (stack.getCount() <= stack.getMaxCount()) {
                output.add(stack);
            } else {
                for (int i = 0; i < stack.getCount() / stack.getMaxCount(); i++) {
                    ItemStack copy = stack.copy();
                    copy.setCount(stack.getMaxCount());
                    output.add(copy);
                }

                ItemStack copy = stack.copy();
                copy.setCount(stack.getCount() % stack.getMaxCount());
                output.add(copy);
            }
        }

        return output;
    }

    public static List<ItemStack> getAsValidStacks(long value) {
        return splitAtMaxCount(getAsItemStackList(value));
    }

    public static List<ItemStack> getAsValidStacks(long[] values) {
        return splitAtMaxCount(getAsItemStackList(values));
    }

    public static int asInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

}
