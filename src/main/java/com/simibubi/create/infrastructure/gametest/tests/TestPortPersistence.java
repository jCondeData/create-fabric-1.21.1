package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItemComponent;
import com.simibubi.create.content.fluids.potion.PotionFluid;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.infrastructure.fabric.transfer.fluid.FluidStack;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * Saving and loading: what a chunk unload / server restart does to block entities
 * (saveWithFullMetadata -> BlockEntity.loadStatic with no level -> back into the level), and the
 * item component codecs used by saves (NBT) and by the network (stream codecs; a broken one
 * disconnects the player).
 */
@GameTestGroup(path = "qa")
public class TestPortPersistence {

    /** What a chunk unload + reload does: save, drop the block entity, load a fresh one. */
    static BlockEntity reload(ServerLevel level, BlockPos abs) {
        BlockEntity be = level.getBlockEntity(abs);
        CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
        BlockState state = level.getBlockState(abs);
        level.removeBlockEntity(abs);
        BlockEntity loaded = BlockEntity.loadStatic(abs, state, tag, level.registryAccess());
        if (loaded == null)
            throw new net.minecraft.gametest.framework.GameTestAssertException(
                    "loadStatic failed for " + state);
        level.setBlockEntity(loaded);
        return loaded;
    }

    static String shorten(String s) {
        return s.length() > 900 ? s.substring(0, 900) + "..." : s;
    }

    static long countItem(Storage<ItemVariant> storage, Item item) {
        long n = 0;
        for (StorageView<ItemVariant> v : storage.nonEmptyViews())
            if (v.getResource().isOf(item)) n += v.getAmount();
        return n;
    }

    static boolean hasExactStack(Storage<ItemVariant> storage, ItemStack expected) {
        for (StorageView<ItemVariant> v : storage.nonEmptyViews())
            if (v.getResource().matches(expected) && v.getAmount() == expected.getCount())
                return true;
        return false;
    }

    /**
     * Every Create block that has a block entity: place it, save it, load it the way chunk loading
     * does (no level during load), save again; the two saves must be identical and nothing may
     * throw.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = 400)
    public static void everyCreateBlockEntitySurvivesSaveAndLoad(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    ServerLevel level = helper.getLevel();
                    var registries = level.registryAccess();
                    BlockPos pos = new BlockPos(3, 2, 3);
                    BlockPos abs = helper.absolutePos(pos);
                    List<String> failures = new ArrayList<>();
                    int tested = 0;
                    for (Block block : BuiltInRegistries.BLOCK) {
                        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                        if (!id.getNamespace().equals(Create.ID) || !(block instanceof EntityBlock))
                            continue;
                        BlockState state = block.defaultBlockState();
                        try {
                            level.setBlock(abs, state, Block.UPDATE_CLIENTS);
                            BlockEntity be = level.getBlockEntity(abs);
                            if (be == null) continue;
                            tested++;
                            CompoundTag first = be.saveWithFullMetadata(registries);
                            BlockEntity loaded =
                                    BlockEntity.loadStatic(abs, state, first.copy(), registries);
                            if (loaded == null) {
                                failures.add(id.getPath() + ": loadStatic returned null");
                                continue;
                            }
                            level.removeBlockEntity(abs);
                            // attach it like chunk loading does, if the block is still there
                            if (level.getBlockState(abs).is(block)) level.setBlockEntity(loaded);
                            CompoundTag second = loaded.saveWithFullMetadata(registries);
                            // a freshly placed block entity may write extra keys once loaded
                            // (lazily
                            // initialised behaviours); everything it saved must come back unchanged
                            for (String key : first.getAllKeys())
                                if (!java.util.Objects.equals(first.get(key), second.get(key)))
                                    failures.add(
                                            id.getPath()
                                                    + "."
                                                    + key
                                                    + ": "
                                                    + first.get(key)
                                                    + " -> "
                                                    + second.get(key));
                        } catch (Throwable t) {
                            failures.add(id.getPath() + ": " + t);
                        } finally {
                            level.removeBlockEntity(abs);
                            level.setBlock(
                                    abs, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                    }
                    helper.assertTrue(
                            failures.isEmpty(),
                            shorten(
                                    failures.size()
                                            + " of "
                                            + tested
                                            + " block entities changed or failed: "
                                            + String.join(" | ", failures)));
                    helper.assertTrue(tested > 100, "only " + tested + " block entities tested");
                    Create.LOGGER.info("[qa] {} Create block entities round-tripped", tested);
                    helper.succeed();
                });
    }

    /**
     * Filled block entities keep their contents through a reload: a fluid tank with lava, a basin
     * with items and water, a depot holding a named item, an item vault with an enchanted sword
     * (6.0.10 #9788 enchanted items vanishing) and a spout with potion fluid.
     */
    @GameTest(template = "flat_15x6x15", timeoutTicks = 100)
    public static void filledBlockEntitiesKeepContentsAfterReload(CreateGameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos tank = new BlockPos(1, 1, 1);
        BlockPos basin = new BlockPos(3, 1, 1);
        BlockPos depot = new BlockPos(5, 1, 1);
        BlockPos vault = new BlockPos(7, 1, 1);
        BlockPos spout = new BlockPos(9, 2, 1);
        BlockPos toolbox = new BlockPos(11, 1, 1);
        helper.setBlock(tank, AllBlocks.FLUID_TANK.getDefaultState());
        helper.setBlock(basin, AllBlocks.BASIN.getDefaultState());
        helper.setBlock(depot, AllBlocks.DEPOT.getDefaultState());
        helper.setBlock(vault, AllBlocks.ITEM_VAULT.getDefaultState());
        helper.setBlock(spout, AllBlocks.SPOUT.getDefaultState());
        helper.setBlock(
                toolbox,
                AllBlocks.TOOLBOXES.get(net.minecraft.world.item.DyeColor.BROWN).getDefaultState());

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(
                level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SHARPNESS),
                3);
        ItemStack named = new ItemStack(Items.COMPASS);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("north"));
        FluidStack potion =
                PotionFluid.of(
                        FluidConstants.BUCKET / 2,
                        new PotionContents(Potions.LEAPING),
                        BottleType.SPLASH);

        try (Transaction t = Transaction.openOuter()) {
            helper.fluidStorageAt(tank)
                    .insert(FluidVariant.of(Fluids.LAVA), 5 * FluidConstants.BUCKET, t);
            helper.itemStorageAt(basin).insert(ItemVariant.of(Items.IRON_INGOT), 5, t);
            helper.fluidStorageAt(basin)
                    .insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, t);
            helper.itemStorageAt(depot).insert(ItemVariant.of(named), 1, t);
            helper.itemStorageAt(vault).insert(ItemVariant.of(sword), 1, t);
            helper.itemStorageAt(vault).insert(ItemVariant.of(Items.COBBLESTONE), 300, t);
            helper.fluidStorageAt(spout).insert(potion.getVariant(), potion.getAmount(), t);
            helper.itemStorageAt(toolbox).insert(ItemVariant.of(Items.STICK), 20, t);
            t.commit();
        }

        helper.runAfterDelay(
                5,
                () -> {
                    for (BlockPos p : new BlockPos[] {tank, basin, depot, vault, spout, toolbox})
                        reload(level, helper.absolutePos(p));
                });

        helper.runAfterDelay(
                15,
                () -> {
                    helper.assertTrue(
                            helper.getTankContents(tank).getAmount() == 5 * FluidConstants.BUCKET,
                            "tank: " + helper.getTankContents(tank).getAmount());
                    helper.assertTrue(
                            countItem(helper.itemStorageAt(basin), Items.IRON_INGOT) == 5,
                            "basin items");
                    helper.assertTrue(
                            helper.getTankContents(basin).getFluid() == Fluids.WATER
                                    && helper.getTankContents(basin).getAmount()
                                            == FluidConstants.BUCKET,
                            "basin fluid: " + helper.getTankContents(basin).getAmount());
                    helper.assertTrue(
                            hasExactStack(helper.itemStorageAt(depot), named),
                            "depot lost the named compass");
                    helper.assertTrue(
                            hasExactStack(helper.itemStorageAt(vault), sword),
                            "vault lost the enchanted sword");
                    helper.assertTrue(
                            countItem(helper.itemStorageAt(vault), Items.COBBLESTONE) == 300,
                            "vault cobble");
                    FluidStack spoutFluid = helper.getTankContents(spout);
                    helper.assertTrue(
                            FluidStack.isSameFluidSameComponents(spoutFluid, potion)
                                    && spoutFluid.getAmount() == potion.getAmount(),
                            "spout: " + spoutFluid.getVariant() + " x " + spoutFluid.getAmount());
                    helper.assertTrue(
                            countItem(helper.itemStorageAt(toolbox), Items.STICK) == 20,
                            "toolbox sticks");
                    Create.LOGGER.info(
                            "[qa] 6 filled block entities kept their contents after reload");
                    helper.succeed();
                });
    }

    /**
     * Compares serialized forms (encode -> decode -> encode) rather than ItemStack.matches: some
     * component records (e.g. upstream's SandPaperItemComponent) have an equals() that never
     * matches a copy, which is upstream behaviour and not what this test is about.
     */
    static void roundTrip(CreateGameTestHelper helper, ItemStack stack, List<String> failures) {
        var registries = helper.getLevel().registryAccess();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        Tag saved;
        try {
            saved = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
            ItemStack loaded = ItemStack.CODEC.parse(ops, saved).getOrThrow();
            Tag again = ItemStack.CODEC.encodeStart(ops, loaded).getOrThrow();
            if (!saved.equals(again) || loaded.getCount() != stack.getCount())
                failures.add(name + " (NBT): " + saved + " -> " + again);
        } catch (Throwable t) {
            failures.add(name + " (NBT): " + t);
            return;
        }
        try {
            RegistryFriendlyByteBuf buf =
                    new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
            ItemStack decoded = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            Tag viaNetwork = ItemStack.CODEC.encodeStart(ops, decoded).getOrThrow();
            if (!saved.equals(viaNetwork) || buf.readableBytes() != 0)
                failures.add(name + " (network): " + saved + " -> " + viaNetwork);
        } catch (Throwable t) {
            failures.add(name + " (network): " + t);
        }
    }

    /**
     * Every Create item, plus items carrying Create's own data components (package with contents,
     * address and order, filter with items, sandpaper polishing, clipboard pages, backtank air,
     * linked controller frequencies), survives the save codec and the network codec unchanged.
     */
    @GameTest(template = "flat_7x6x7")
    public static void createItemStacksSurviveSaveAndNetworkCodecs(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    List<String> failures = new ArrayList<>();
                    int n = 0;
                    for (Item item : BuiltInRegistries.ITEM) {
                        if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Create.ID))
                            continue;
                        roundTrip(helper, new ItemStack(item), failures);
                        n++;
                    }

                    List<ItemStack> special = new ArrayList<>();
                    ItemStack box =
                            PackageItem.containing(
                                    List.of(
                                            new ItemStack(Items.DIAMOND, 3),
                                            new ItemStack(AllItems.BRASS_INGOT.get(), 64)));
                    PackageItem.addAddress(box, "storage*");
                    PackageItem.setOrder(box, 7, 1, true, 2, false, null);
                    special.add(box);

                    ItemStack filter = new ItemStack(AllItems.FILTER.get());
                    filter.set(
                            AllDataComponents.FILTER_ITEMS,
                            ItemContainerContents.fromItems(
                                    List.of(
                                            new ItemStack(Items.STONE),
                                            new ItemStack(Items.DIRT))));
                    filter.set(AllDataComponents.FILTER_ITEMS_BLACKLIST, true);
                    special.add(filter);

                    ItemStack sandpaper = new ItemStack(AllItems.SAND_PAPER.get());
                    sandpaper.set(
                            AllDataComponents.SAND_PAPER_POLISHING,
                            new SandPaperItemComponent(new ItemStack(Items.QUARTZ)));
                    special.add(sandpaper);

                    ItemStack clipboard = new ItemStack(AllBlocks.CLIPBOARD.get());
                    List<ClipboardEntry> page = new ArrayList<>();
                    page.add(new ClipboardEntry(true, Component.literal("buy zinc")));
                    page.add(new ClipboardEntry(false, Component.literal("build mixer")));
                    clipboard.set(
                            AllDataComponents.CLIPBOARD_CONTENT,
                            com.simibubi.create.content.equipment.clipboard.ClipboardContent.EMPTY
                                    .setPages(List.of(page)));
                    special.add(clipboard);

                    ItemStack backtank = new ItemStack(AllItems.COPPER_BACKTANK.get());
                    backtank.set(AllDataComponents.BACKTANK_AIR, 600);
                    special.add(backtank);

                    ItemStack controller = new ItemStack(AllItems.LINKED_CONTROLLER.get());
                    controller.set(
                            AllDataComponents.LINKED_CONTROLLER_ITEMS,
                            ItemContainerContents.fromItems(
                                    List.of(
                                            new ItemStack(Items.REDSTONE),
                                            new ItemStack(Items.LAPIS_LAZULI))));
                    special.add(controller);

                    ItemStack schedule = new ItemStack(AllItems.SCHEDULE.get());
                    CompoundTag scheduleTag = new CompoundTag();
                    scheduleTag.putBoolean("Cyclic", true);
                    schedule.set(AllDataComponents.TRAIN_SCHEDULE, scheduleTag);
                    special.add(schedule);

                    for (ItemStack s : special) roundTrip(helper, s, failures);

                    helper.assertTrue(
                            failures.isEmpty(),
                            shorten(
                                    failures.size()
                                            + " item stacks changed: "
                                            + String.join(" | ", failures)));
                    Create.LOGGER.info(
                            "[qa] {} Create items + {} component stacks round-tripped",
                            n,
                            special.size());
                    helper.succeed();
                });
    }

    /**
     * Changing what a Create inventory holds must mark its chunk as changed, or the change is lost
     * when the chunk unloads without anything else touching it.
     */
    @GameTest(template = "flat_15x6x15")
    public static void inventoryChangesMarkTheChunkForSaving(CreateGameTestHelper helper) {
        List<BlockState> blocks =
                List.of(
                        AllBlocks.ITEM_VAULT.getDefaultState(),
                        AllBlocks.DEPOT.getDefaultState(),
                        AllBlocks.BASIN.getDefaultState(),
                        AllBlocks.TOOLBOXES
                                .get(net.minecraft.world.item.DyeColor.BROWN)
                                .getDefaultState());
        for (int i = 0; i < blocks.size(); i++)
            helper.setBlock(new BlockPos(1 + 3 * i, 1, 1), blocks.get(i));
        // one block at a time, 10 ticks apart: some (the basin) record the change on their next
        // tick, so look for the mark until a few ticks after the insert. The flag is read at the
        // end of every tick: the server may save the chunk (clearing the flag) at the start of
        // the next one, which made a single late check fail under load (tester round 3).
        boolean[] marked = new boolean[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            int index = i;
            BlockPos pos = new BlockPos(1 + 3 * i, 1, 1);
            helper.runAfterDelay(
                    5 + 10 * i,
                    () -> {
                        helper.getLevel().getChunkAt(helper.absolutePos(pos)).setUnsaved(false);
                        try (Transaction t = Transaction.openOuter()) {
                            long in =
                                    helper.itemStorageAt(pos)
                                            .insert(ItemVariant.of(Items.APPLE), 3, t);
                            helper.assertTrue(
                                    in == 3, blocks.get(index).getBlock() + " took " + in);
                            t.commit();
                        }
                        marked[index] |=
                                helper.getLevel().getChunkAt(helper.absolutePos(pos)).isUnsaved();
                    });
            // (tasks due on the same tick run in no fixed order, so sample from the next tick on
            // and decide one tick after the last sample)
            for (int d = 6; d <= 9; d++)
                helper.runAfterDelay(
                        d + 10 * i,
                        () ->
                                marked[index] |=
                                        helper.getLevel()
                                                .getChunkAt(helper.absolutePos(pos))
                                                .isUnsaved());
            helper.runAfterDelay(
                    10 + 10 * i,
                    () ->
                            helper.assertTrue(
                                    marked[index],
                                    blocks.get(index).getBlock()
                                            + " changed its items without marking the chunk"
                                            + " unsaved"));
        }
        helper.runAfterDelay(10 * blocks.size() + 5, helper::succeed);
    }

    /** Two item vault blocks in a row form one vault: one inventory, twice the capacity. */
    @GameTest(template = "flat_7x6x7")
    public static void itemVaultsMergeIntoOneInventory(CreateGameTestHelper helper) {
        BlockPos a = new BlockPos(2, 1, 2);
        BlockPos b = new BlockPos(3, 1, 2);
        var state =
                AllBlocks.ITEM_VAULT
                        .getDefaultState()
                        .setValue(
                                com.simibubi.create.content.logistics.vault.ItemVaultBlock
                                        .HORIZONTAL_AXIS,
                                net.minecraft.core.Direction.Axis.X);
        helper.setBlock(a, state);
        helper.setBlock(b, state);
        helper.runAfterDelay(
                10,
                () -> {
                    int slotsPerBlock =
                            com.simibubi.create.infrastructure.config.AllConfigs.server()
                                    .logistics
                                    .vaultCapacity
                                    .get();
                    long accepted;
                    try (Transaction t = Transaction.openOuter()) {
                        accepted =
                                helper.itemStorageAt(a)
                                        .insert(ItemVariant.of(Items.COBBLESTONE), 100_000, t);
                        t.commit();
                    }
                    helper.assertTrue(
                            accepted == 2L * slotsPerBlock * 64,
                            "2-block vault accepted "
                                    + accepted
                                    + ", expected "
                                    + (2L * slotsPerBlock * 64));
                    helper.assertTrue(
                            countItem(helper.itemStorageAt(b), Items.COBBLESTONE) == accepted,
                            "the other vault block sees a different inventory");
                    helper.succeed();
                });
    }
}
