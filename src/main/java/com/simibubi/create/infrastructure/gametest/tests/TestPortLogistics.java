package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Packagers, hoppers and funnels moving items through Create inventories: everything here goes
 * through Fabric Transfer API transactions (vanilla hoppers use StorageUtil.move on Fabric, Create
 * funnels simulate before they extract), the riskiest part of the port. Every test checks that no
 * item is lost or duplicated.
 */
@GameTestGroup(path = "qa")
public class TestPortLogistics {

    // ---------------------------------------------------------------- helpers

    /** Counts {@code item} in the storage, looking inside packages too. Read-only. */
    static long countIn(Storage<ItemVariant> storage, Item item) {
        long total = 0;
        if (storage == null) return 0;
        for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
            ItemStack stack = view.getResource().toStack((int) view.getAmount());
            total += countInStack(stack, item);
        }
        return total;
    }

    static long countInStack(ItemStack stack, Item item) {
        if (stack.isEmpty()) return 0;
        if (stack.is(item)) return stack.getCount();
        if (PackageItem.isPackage(stack)) {
            long total = 0;
            var contents = PackageItem.getContents(stack);
            for (int i = 0; i < contents.getSlotCount(); i++)
                total += countInStack(contents.getStackInSlot(i), item) * stack.getCount();
            return total;
        }
        return 0;
    }

    static long countInContainer(CreateGameTestHelper helper, BlockPos pos, Item item) {
        if (!(helper.getBlockEntity(pos) instanceof Container c)) return 0;
        long total = 0;
        for (int i = 0; i < c.getContainerSize(); i++) total += countInStack(c.getItem(i), item);
        return total;
    }

    /** Items lying around as item entities or package entities anywhere in the test area. */
    static long countInEntities(CreateGameTestHelper helper, Item item) {
        AABB area = helper.getBounds().inflate(2);
        long total = 0;
        for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area))
            total += countInStack(e.getItem(), item);
        for (PackageEntity e : helper.getLevel().getEntitiesOfClass(PackageEntity.class, area))
            total += countInStack(e.box, item);
        return total;
    }

    static long countInPackager(CreateGameTestHelper helper, BlockPos pos, Item item) {
        if (!(helper.getBlockEntity(pos) instanceof PackagerBlockEntity be)) return 0;
        long total = countInStack(be.heldBox, item);
        for (var big : be.queuedExitingPackages) total += countInStack(big.stack, item) * big.count;
        return total;
    }

    static void fillContainer(CreateGameTestHelper helper, BlockPos pos, ItemStack... stacks) {
        Container c = (Container) helper.getBlockEntity(pos);
        for (int i = 0; i < stacks.length; i++) c.setItem(i, stacks[i].copy());
        c.setChanged();
    }

    static BlockState packager(Direction facing) {
        return AllBlocks.PACKAGER.getDefaultState().setValue(PackagerBlock.FACING, facing);
    }

    // ---------------------------------------------------------------- tests

    /**
     * A package pushed by a hopper into a packager whose chest is full must stay in the hopper: the
     * unpack is simulated first, and nothing may be lost or duplicated.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void packagerRefusesPackageWhenTargetIsFull(CreateGameTestHelper helper) {
        BlockPos hopper = new BlockPos(1, 1, 1);
        BlockPos packager = new BlockPos(2, 1, 1);
        BlockPos chest = new BlockPos(2, 1, 2);

        helper.setBlock(
                hopper,
                Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        helper.setBlock(packager, packager(Direction.NORTH));
        helper.setBlock(chest, Blocks.CHEST);
        ItemStack[] full = new ItemStack[27];
        for (int i = 0; i < full.length; i++) full[i] = new ItemStack(Items.COBBLESTONE, 64);
        fillContainer(helper, chest, full);

        ItemStack box = PackageItem.containing(List.of(new ItemStack(Items.DIAMOND, 5)));
        ((HopperBlockEntity) helper.getBlockEntity(hopper)).setItem(0, box);

        helper.runAfterDelay(
                60,
                () -> {
                    long diamonds =
                            countInContainer(helper, hopper, Items.DIAMOND)
                                    + countInContainer(helper, chest, Items.DIAMOND)
                                    + countInPackager(helper, packager, Items.DIAMOND)
                                    + countInEntities(helper, Items.DIAMOND);
                    helper.assertTrue(diamonds == 5, "5 diamonds in the package, now " + diamonds);
                    helper.assertTrue(
                            countInContainer(helper, chest, Items.COBBLESTONE) == 27 * 64,
                            "full chest changed");
                    helper.assertTrue(
                            countInContainer(helper, hopper, Items.DIAMOND) == 5,
                            "package should still wait in the hopper");
                    Create.LOGGER.info("[qa] full target: package kept in hopper");
                    helper.succeed();
                });
    }

    /**
     * Vanilla hoppers feed Create inventories through Fabric's storage API (inside a transaction):
     * depot, basin and item vault accept the items; chute and smart chute pass them down into a
     * chest. No crash, no loss.
     */
    @GameTest(template = "flat_15x6x15", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void hoppersFeedCreateInventories(CreateGameTestHelper helper) {
        List<BlockState> targets =
                List.of(
                        AllBlocks.DEPOT.getDefaultState(),
                        AllBlocks.BASIN.getDefaultState(),
                        AllBlocks.ITEM_VAULT.getDefaultState(),
                        AllBlocks.CHUTE.getDefaultState(),
                        AllBlocks.SMART_CHUTE.getDefaultState());
        List<BlockPos> destinations = new ArrayList<>();
        List<BlockPos> hoppers = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            boolean chute = i >= 3;
            BlockPos target = new BlockPos(1 + 2 * i, chute ? 2 : 1, 1);
            BlockPos hopper = target.above();
            if (chute) helper.setBlock(target.below(), Blocks.CHEST);
            helper.setBlock(target, targets.get(i));
            helper.setBlock(
                    hopper,
                    Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
            ((HopperBlockEntity) helper.getBlockEntity(hopper))
                    .setItem(0, new ItemStack(Items.IRON_INGOT, 8));
            destinations.add(chute ? target.below() : target);
            hoppers.add(hopper);
        }
        helper.succeedWhenWithDiagnostics(
                () -> {
                    for (int i = 0; i < targets.size(); i++) {
                        long inHopper = countInContainer(helper, hoppers.get(i), Items.IRON_INGOT);
                        Storage<ItemVariant> storage =
                                ItemStorage.SIDED.find(
                                        helper.getLevel(),
                                        helper.absolutePos(destinations.get(i)),
                                        Direction.UP);
                        long inTarget = countIn(storage, Items.IRON_INGOT);
                        // a depot only takes an item while it is empty (upstream DepotItemHandler)
                        boolean depot = targets.get(i).is(AllBlocks.DEPOT.get());
                        if (depot
                                ? inHopper + inTarget != 8 || inTarget < 1
                                : inHopper != 0 || inTarget != 8)
                            helper.fail(
                                    targets.get(i).getBlock()
                                            + ": hopper "
                                            + inHopper
                                            + ", destination "
                                            + inTarget
                                            + ", dropped in the area "
                                            + countInEntities(helper, Items.IRON_INGOT));
                    }
                    helper.assertTrue(
                            countInEntities(helper, Items.IRON_INGOT) == 0,
                            "iron dropped in the world");
                    Create.LOGGER.info("[qa] hoppers filled {} Create inventories", targets.size());
                },
                () -> "");
    }

    /**
     * Vanilla hoppers pull from Create inventories (extraction inside a Fabric transaction): an
     * item vault and a depot are emptied into the hopper below, nothing lost or duplicated.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void hoppersPullFromCreateInventories(CreateGameTestHelper helper) {
        BlockPos vault = new BlockPos(1, 2, 1);
        BlockPos vaultHopper = new BlockPos(1, 1, 1);
        BlockPos depot = new BlockPos(3, 2, 1);
        BlockPos depotHopper = new BlockPos(3, 1, 1);
        helper.setBlock(vault, AllBlocks.ITEM_VAULT.getDefaultState());
        helper.setBlock(depot, AllBlocks.DEPOT.getDefaultState());
        helper.setBlock(
                vaultHopper,
                Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        helper.setBlock(
                depotHopper,
                Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        try (Transaction t = Transaction.openOuter()) {
            long a = helper.itemStorageAt(vault).insert(ItemVariant.of(Items.REDSTONE), 100, t);
            long b = helper.itemStorageAt(depot).insert(ItemVariant.of(Items.EMERALD), 7, t);
            helper.assertTrue(a == 100 && b == 7, "setup inserts: vault " + a + ", depot " + b);
            t.commit();
        }
        helper.succeedWhenWithDiagnostics(
                () -> {
                    long vaultLeft = countIn(helper.itemStorageAt(vault), Items.REDSTONE);
                    long vaultHopperHas = countInContainer(helper, vaultHopper, Items.REDSTONE);
                    long depotLeft = countIn(helper.itemStorageAt(depot), Items.EMERALD);
                    long depotHopperHas = countInContainer(helper, depotHopper, Items.EMERALD);
                    helper.assertTrue(
                            vaultLeft + vaultHopperHas == 100,
                            "redstone lost or duplicated: " + vaultLeft + " + " + vaultHopperHas);
                    helper.assertTrue(
                            depotLeft + depotHopperHas == 7,
                            "emeralds lost or duplicated: " + depotLeft + " + " + depotHopperHas);
                    // hoppers hold 5 stacks; vault -> hopper moves 1 item per 8 ticks
                    helper.assertTrue(depotLeft == 0, "depot not emptied: " + depotLeft);
                    helper.assertTrue(
                            vaultHopperHas >= 5, "vault hopper only got " + vaultHopperHas);
                    Create.LOGGER.info("[qa] hoppers pulled from vault and depot");
                },
                () -> "");
    }

    /**
     * Offering a depot more than a stack: it accepts exactly one stack (64), holds 64, and the rest
     * stays with the caller. No duplication through an oversized held stack.
     */
    @GameTest(template = "flat_7x6x7")
    public static void depotTakesAtMostOneStack(CreateGameTestHelper helper) {
        BlockPos depot = new BlockPos(2, 1, 2);
        helper.setBlock(depot, AllBlocks.DEPOT.getDefaultState());
        helper.runAfterDelay(
                1,
                () -> {
                    long accepted;
                    try (Transaction t = Transaction.openOuter()) {
                        accepted =
                                helper.itemStorageAt(depot)
                                        .insert(ItemVariant.of(Items.COBBLESTONE), 100, t);
                        t.commit();
                    }
                    long held = countIn(helper.itemStorageAt(depot), Items.COBBLESTONE);
                    helper.assertTrue(
                            accepted == 64 && held == 64,
                            "depot accepted "
                                    + accepted
                                    + " and holds "
                                    + held
                                    + " of 100 cobblestone");
                    helper.succeed();
                });
    }
}
