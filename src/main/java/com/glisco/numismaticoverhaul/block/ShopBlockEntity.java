package com.glisco.numismaticoverhaul.block;

import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.owo.ops.WorldOps;
import io.wispforest.owo.util.ImplementedInventory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.village.Merchant;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class ShopBlockEntity extends LockableContainerBlockEntity implements ImplementedInventory, SidedInventory, NamedScreenHandlerFactory {

    private static final int[] SLOTS = IntStream.range(0, 27).toArray();
    private static final int[] NO_SLOTS = new int[0];
    public static KeyedEndec<List<ShopOffer>> OFFERS_LIST = ShopOffer.ENDEC.listOf().keyed("offers", ArrayList::new);

    // TODO - Review, no longer final
    private DefaultedList<ItemStack> INVENTORY = DefaultedList.ofSize(27, ItemStack.EMPTY);

    public boolean busy = false;
    private final Merchant merchant;
    private List<ShopOffer> offers;

    private long storedCurrency;
    private UUID owner;
    private boolean allowsTransfer = false;

    private int tradeIndex;

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(NumismaticOverhaulBlocks.Entities.SHOP, pos, state);

        boolean inexhaustible = (state.getBlock() instanceof ShopBlock shop) && shop.inexhaustible();
        this.merchant = new ShopMerchant(this, inexhaustible);

        this.offers = new ArrayList<>();
        this.storedCurrency = 0;
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return INVENTORY;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return allowsTransfer ? SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return allowsTransfer;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("gui.numismatic-overhaul.shop.inventory_title");
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return this.INVENTORY;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
        this.INVENTORY = inventory;
    }

    @NotNull
    public Merchant getMerchant() {
        return merchant;
    }

    public List<ShopOffer> getOffers() {
        return offers;
    }

    public long getStoredCurrency() {
        return storedCurrency;
    }

    public boolean isTransferEnabled() {
        return allowsTransfer;
    }

    public void toggleTransfer() {
        this.allowsTransfer = !this.allowsTransfer;
    }

    public void setStoredCurrency(long storedCurrency) {
        this.storedCurrency = storedCurrency;
        markDirty();
    }

    public void addCurrency(long value) {
        this.storedCurrency += value;
        markDirty();
    }

    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(tag, registryLookup);
        Inventories.writeNbt(tag, INVENTORY, registryLookup);
        tag.put(OFFERS_LIST, offers);
        tag.putBoolean("AllowsTransfer", this.allowsTransfer);
        tag.putLong("StoredCurrency", storedCurrency);
        if (owner != null) {
            tag.putUuid("Owner", owner);
        }
    }

    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(tag, registryLookup);
        Inventories.readNbt(tag, INVENTORY, registryLookup);
        this.offers = tag.get(OFFERS_LIST);
        if (tag.contains("Owner")) {
            owner = tag.getUuid("Owner");
        }
        this.allowsTransfer = tag.getBoolean("AllowsTransfer");
        this.storedCurrency = tag.getLong("StoredCurrency");
    }

    public void addOrReplaceOffer(ShopOffer offer) {

        int indexToReplace = -1;

        for (int i = 0; i < offers.size(); i++) {
            if (!ItemStack.areEqual(offer.getSellStack(), offers.get(i).getSellStack())) continue;
            indexToReplace = i;
            break;
        }

        if (indexToReplace == -1) {
            if (offers.size() >= 24) {
                NumismaticOverhaul.LOGGER.error("Tried adding more than 24 trades to shop at {}", this.pos);
                return;
            }
            offers.add(offer);
        } else {
            offers.set(indexToReplace, offer);
        }

        this.markDirty();
    }

    public void deleteOffer(ItemStack stack) {
        if (!offers.removeIf(offer -> ItemStack.areEqual(stack, offer.getSellStack()))) {
            NumismaticOverhaul.LOGGER.error("Tried to delete invalid trade for {} from shop at {}", stack, this.pos);
            return;
        }

        this.markDirty();
    }

    public static void tick(World world, BlockPos ignoredPos, BlockState ignoredState, ShopBlockEntity blockEntity) {
        blockEntity.tick(world);
    }

    public void tick(World world) {
        if (world.getTime() % 60 == 0) tradeIndex++;
    }

    @Environment(EnvType.CLIENT)
    public ItemStack getItemToRender() {
        if (tradeIndex > offers.size() - 1) tradeIndex = 0;
        return offers.get(tradeIndex).getSellStack();
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new ShopScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return player.getUuid().equals(this.owner) && this.world.getBlockEntity(this.pos) == this && this.pos.getSquaredDistance(player.getX(), player.getY(), player.getZ()) <= 100;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound tag = new NbtCompound();
        this.writeNbt(tag, registryLookup);
        tag.remove("Items");
        tag.remove("StoredCurrency");
        return tag;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        markDirty();
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        WorldOps.updateIfOnServer(world, pos);
    }
}
