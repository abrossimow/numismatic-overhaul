package com.glisco.numismaticoverhaul.block;

import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.item.CurrencyTooltipData;
import io.wispforest.owo.registration.reflect.BlockEntityRegistryContainer;
import io.wispforest.owo.registration.reflect.BlockRegistryContainer;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import java.util.*;

public class NumismaticOverhaulBlocks implements BlockRegistryContainer {

    public static final Block SHOP = new ShopBlock(false);
    public static final Block INEXHAUSTIBLE_SHOP = new ShopBlock(true);
    public static final Block PIGGY_BANK = new PiggyBankBlock();

    @Override
    public BlockItem createBlockItem(Block block, String identifier) {
        if (block == INEXHAUSTIBLE_SHOP) {
            return new BlockItem(block, new Item.Settings().group(NumismaticOverhaul.NUMISMATIC_GROUP).rarity(Rarity.EPIC)) {
                @Override
                public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
                    tooltip.add(Text.translatable(stack.getTranslationKey() + ".tooltip").formatted(Formatting.GRAY));
                }
            };
        } else if (block == PIGGY_BANK) {
            return new BlockItem(block, new Item.Settings().group(NumismaticOverhaul.NUMISMATIC_GROUP).equipmentSlot((entity, stack) -> EquipmentSlot.HEAD)) {

                @Override
                public Optional<TooltipData> getTooltipData(ItemStack stack) {
                    var containerComponent = stack.getComponents().getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);

                    List<Long> valueList = new ArrayList<>();
                    containerComponent.iterateNonEmpty().forEach(itemStack -> valueList.add(((long) itemStack.getCount())));

                    if (valueList.isEmpty()) return Optional.empty();

                    long[] values = new long[valueList.size()];
                    for (int i = 0; i < valueList.size(); i++) {
                        values[i] = valueList.get(i);
                    }
                    return Optional.of(new CurrencyTooltipData(values, new long[]{-1}));
                }
            };
        }

        return new BlockItem(block, new Item.Settings().group(NumismaticOverhaul.NUMISMATIC_GROUP));
    }

    public static final class Entities implements BlockEntityRegistryContainer {

        public static final BlockEntityType<ShopBlockEntity> SHOP =
            BlockEntityType.Builder.create(ShopBlockEntity::new, NumismaticOverhaulBlocks.SHOP, NumismaticOverhaulBlocks.INEXHAUSTIBLE_SHOP).build();

        public static final BlockEntityType<PiggyBankBlockEntity> PIGGY_BANK =
            BlockEntityType.Builder.create(PiggyBankBlockEntity::new, NumismaticOverhaulBlocks.PIGGY_BANK).build();
    }
}
