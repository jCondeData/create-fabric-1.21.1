package com.simibubi.create.infrastructure.gametest.tests;

import static com.simibubi.create.infrastructure.gametest.tests.TestPortLogistics.*;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;

import java.util.List;
import java.util.UUID;

/**
 * Round-2 QA around the packager extraction fix (PackagerItemHandler takes part in Fabric
 * transactions). Spec: a packager packs its attached inventory on a redstone pulse, shows the
 * package for its animation, and then hands it to whatever pulls from it (funnels, chutes, vanilla
 * hoppers, other mods' pipes); a package is never lost or duplicated, also across a save/reload or
 * when the packager is broken.
 */
@GameTestGroup(path = "qa")
public class TestPortPackagerPulls {

    static PackagerBlockEntity packagerAt(CreateGameTestHelper helper, BlockPos pos) {
        return (PackagerBlockEntity) helper.getBlockEntity(pos);
    }

    /** Package items (boxes) anywhere: containers, packagers, item and package entities. */
    static long packagesIn(CreateGameTestHelper helper, List<BlockPos> containers) {
        long n = 0;
        for (BlockPos pos : containers) {
            if (helper.getBlockEntity(pos) instanceof Container c) {
                for (int i = 0; i < c.getContainerSize(); i++)
                    if (PackageItem.isPackage(c.getItem(i))) n += c.getItem(i).getCount();
            } else if (helper.getBlockEntity(pos) instanceof PackagerBlockEntity be) {
                if (!be.heldBox.isEmpty()) n++;
                for (var big : be.queuedExitingPackages) n += big.count;
            }
        }
        for (ItemEntity e :
                helper.getLevel()
                        .getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2)))
            if (PackageItem.isPackage(e.getItem())) n += e.getItem().getCount();
        n +=
                helper.getLevel()
                        .getEntitiesOfClass(
                                com.simibubi.create.content.logistics.box.PackageEntity.class,
                                helper.getBounds().inflate(2))
                        .size();
        return n;
    }

    /**
     * A chute under a packager pulls the package out and drops it into the chest below it, with
     * every item inside.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.FIFTEEN_SECONDS)
    public static void chutePullsPackageOutOfPackager(CreateGameTestHelper helper) {
        BlockPos chestTop = new BlockPos(2, 4, 2);
        BlockPos packager = new BlockPos(2, 3, 2);
        BlockPos chute = new BlockPos(2, 2, 2);
        BlockPos chestBottom = new BlockPos(2, 1, 2);
        helper.setBlock(chestTop, Blocks.CHEST);
        fillContainer(helper, chestTop, new ItemStack(Items.GOLD_INGOT, 8));
        helper.setBlock(packager, packager(Direction.DOWN)); // target = chest above
        helper.setBlock(chute, AllBlocks.CHUTE.getDefaultState());
        helper.setBlock(chestBottom, Blocks.CHEST);
        helper.runAfterDelay(
                10, () -> helper.setBlock(new BlockPos(1, 3, 2), Blocks.REDSTONE_BLOCK));
        List<BlockPos> all = List.of(chestTop, packager, chute, chestBottom);
        helper.succeedWhenWithDiagnostics(
                () -> {
                    long total =
                            countInContainer(helper, chestTop, Items.GOLD_INGOT)
                                    + countInPackager(helper, packager, Items.GOLD_INGOT)
                                    + countIn(helper.itemStorageAt(chute), Items.GOLD_INGOT)
                                    + countInContainer(helper, chestBottom, Items.GOLD_INGOT)
                                    + countInEntities(helper, Items.GOLD_INGOT);
                    if (total != 8) helper.fail("8 gold went in, " + total + " exist");
                    helper.assertTrue(
                            countInContainer(helper, chestBottom, Items.GOLD_INGOT) == 8,
                            "package not in the bottom chest yet");
                    helper.assertTrue(packagesIn(helper, all) == 1, "exactly one package");
                    Create.LOGGER.info("[qa] chute pulled the package into the chest below");
                },
                () ->
                        "top "
                                + countInContainer(helper, chestTop, Items.GOLD_INGOT)
                                + " packager "
                                + countInPackager(helper, packager, Items.GOLD_INGOT)
                                + " chute "
                                + countIn(helper.itemStorageAt(chute), Items.GOLD_INGOT)
                                + " bottom "
                                + countInContainer(helper, chestBottom, Items.GOLD_INGOT)
                                + " entities "
                                + countInEntities(helper, Items.GOLD_INGOT));
    }

    /**
     * While the packager animates a new package nobody may take it (upstream: extractItem returns
     * nothing while animationTicks != 0), and the refused attempts must not delete it; once the
     * animation is over one extraction takes exactly that package, and a second gets nothing.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void packagerHoldsItsPackageDuringTheAnimation(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(2, 2, 2);
        BlockPos packager = new BlockPos(2, 1, 2);
        helper.setBlock(chest, Blocks.CHEST);
        fillContainer(
                helper,
                chest,
                new ItemStack(Items.IRON_INGOT, 16),
                new ItemStack(Items.DIAMOND, 3));
        helper.setBlock(packager, packager(Direction.DOWN)); // target = chest above
        helper.runAfterDelay(
                5, () -> helper.setBlock(new BlockPos(1, 1, 2), Blocks.REDSTONE_BLOCK));

        int[] refused = {0};
        String[] problem = {null};
        ItemStack[] taken = {ItemStack.EMPTY};
        helper.onEachTick(
                () -> {
                    if (problem[0] != null || !taken[0].isEmpty()) return;
                    PackagerBlockEntity be = packagerAt(helper, packager);
                    if (be.heldBox.isEmpty()) return;
                    Storage<ItemVariant> storage = helper.itemStorageAt(packager);
                    ItemVariant box = ItemVariant.of(be.heldBox);
                    if (be.animationTicks > 0) {
                        try (Transaction t = Transaction.openOuter()) {
                            long n = storage.extract(box, 1, t);
                            t.commit();
                            if (n != 0)
                                problem[0] =
                                        "extracted "
                                                + n
                                                + " during the animation (ticks left "
                                                + be.animationTicks
                                                + ")";
                        }
                        if (be.heldBox.isEmpty())
                            problem[0] = "a refused extraction deleted the package";
                        refused[0]++;
                        return;
                    }
                    try (Transaction t = Transaction.openOuter()) {
                        long n = storage.extract(box, 64, t);
                        long again = storage.extract(box, 64, t);
                        t.commit();
                        if (n != 1 || again != 0)
                            problem[0] = "after the animation: took " + n + ", then " + again;
                    }
                    taken[0] = box.toStack();
                });
        helper.succeedWhen(
                () -> {
                    helper.assertTrue(problem[0] == null, String.valueOf(problem[0]));
                    helper.assertFalse(taken[0].isEmpty(), "package not taken yet");
                    helper.assertTrue(
                            refused[0] >= 10,
                            "only " + refused[0] + " extraction attempts during the animation");
                    helper.assertTrue(
                            countInStack(taken[0], Items.IRON_INGOT) == 16
                                    && countInStack(taken[0], Items.DIAMOND) == 3,
                            "the package holds the chest's items");
                    helper.assertTrue(
                            packagerAt(helper, packager).heldBox.isEmpty(),
                            "the packager still holds a package");
                    Create.LOGGER.info(
                            "[qa] packager refused {} pulls while animating, then gave 1 package",
                            refused[0]);
                });
    }

    /**
     * Save and reload (a chunk unload) a packager in the middle of its animation: it keeps the
     * package, finishes the animation, and the hopper below then gets that same package once.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void heldPackageSurvivesReloadMidAnimation(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(2, 3, 2);
        BlockPos packager = new BlockPos(2, 2, 2);
        BlockPos hopper = new BlockPos(2, 1, 2);
        helper.setBlock(chest, Blocks.CHEST);
        fillContainer(helper, chest, new ItemStack(Items.EMERALD, 5));
        helper.setBlock(packager, packager(Direction.DOWN));
        helper.setBlock(
                hopper,
                Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        helper.runAfterDelay(
                5, () -> helper.setBlock(new BlockPos(1, 2, 2), Blocks.REDSTONE_BLOCK));

        ItemStack[] before = {ItemStack.EMPTY};
        int[] ticksAtReload = {-1};
        helper.onEachTick(
                () -> {
                    if (ticksAtReload[0] >= 0) return;
                    PackagerBlockEntity be = packagerAt(helper, packager);
                    if (be.heldBox.isEmpty() || be.animationTicks > PackagerBlockEntity.CYCLE / 2)
                        return;
                    before[0] = be.heldBox.copy();
                    ticksAtReload[0] = be.animationTicks;
                    TestPortPersistence.reload(helper.getLevel(), helper.absolutePos(packager));
                });
        helper.succeedWhenWithDiagnostics(
                () -> {
                    helper.assertTrue(ticksAtReload[0] > 0, "not reloaded mid-animation yet");
                    PackagerBlockEntity be = packagerAt(helper, packager);
                    helper.assertTrue(be.heldBox.isEmpty(), "package still in the packager");
                    Container h = (Container) helper.getBlockEntity(hopper);
                    long packages = 0;
                    ItemStack got = ItemStack.EMPTY;
                    for (int i = 0; i < h.getContainerSize(); i++)
                        if (PackageItem.isPackage(h.getItem(i))) {
                            packages += h.getItem(i).getCount();
                            got = h.getItem(i);
                        }
                    helper.assertTrue(packages == 1, "hopper has " + packages + " packages");
                    helper.assertTrue(
                            ItemStack.isSameItemSameComponents(got, before[0]),
                            "the hopper got a different package: " + got + " vs " + before[0]);
                    helper.assertTrue(
                            countInEntities(helper, Items.EMERALD) == 0
                                    && countInContainer(helper, chest, Items.EMERALD) == 0,
                            "emeralds outside the package");
                    Create.LOGGER.info(
                            "[qa] reloaded at {} animation ticks left; hopper got the same"
                                    + " package",
                            ticksAtReload[0]);
                },
                () ->
                        "reloadAt="
                                + ticksAtReload[0]
                                + " packager="
                                + packagerAt(helper, packager).heldBox
                                + " anim="
                                + packagerAt(helper, packager).animationTicks
                                + " hopperEmeralds="
                                + countInContainer(helper, hopper, Items.EMERALD));
    }

    /**
     * Three pulses make three packages while a hopper below and two extracting funnels on the sides
     * all pull from the same packager every tick. Every package comes out exactly once and every
     * item is accounted for.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TWENTY_SECONDS)
    public static void threeExtractorsShareThreePackages(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(3, 3, 3);
        BlockPos packager = new BlockPos(3, 2, 3);
        BlockPos hopper = new BlockPos(3, 1, 3);
        BlockPos hopperTarget = new BlockPos(4, 1, 3);
        BlockPos funnelWest = new BlockPos(2, 2, 3);
        BlockPos funnelEast = new BlockPos(4, 2, 3);
        BlockPos lever = new BlockPos(3, 2, 2);

        helper.setBlock(chest, Blocks.CHEST);
        ItemStack[] full = new ItemStack[27];
        for (int i = 0; i < full.length; i++) full[i] = new ItemStack(Items.COBBLESTONE, 64);
        fillContainer(helper, chest, full);
        helper.setBlock(packager, packager(Direction.DOWN));
        helper.setBlock(
                hopper,
                Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        helper.setBlock(hopperTarget, Blocks.CHEST);
        for (var f : List.of(funnelWest, funnelEast))
            helper.setBlock(
                    f,
                    AllBlocks.ANDESITE_FUNNEL
                            .getDefaultState()
                            .setValue(
                                    FunnelBlock.FACING,
                                    f == funnelWest ? Direction.WEST : Direction.EAST)
                            .setValue(FunnelBlock.EXTRACTING, true));
        // three pulses, 60 ticks apart (the packager ignores pulses within 40 ticks)
        for (int i = 0; i < 3; i++) {
            helper.runAfterDelay(10 + 60 * i, () -> helper.setBlock(lever, Blocks.REDSTONE_BLOCK));
            helper.runAfterDelay(15 + 60 * i, () -> helper.setBlock(lever, Blocks.AIR));
        }
        BlockPos hopperLock = new BlockPos(3, 1, 4);
        helper.setBlock(hopperLock, Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(125, () -> helper.setBlock(hopperLock, Blocks.AIR));
        List<BlockPos> containers = List.of(chest, packager, hopper, hopperTarget);
        helper.runAfterDelay(
                220,
                () -> {
                    long cobble =
                            countInContainer(helper, chest, Items.COBBLESTONE)
                                    + countInPackager(helper, packager, Items.COBBLESTONE)
                                    + countInContainer(helper, hopper, Items.COBBLESTONE)
                                    + countInContainer(helper, hopperTarget, Items.COBBLESTONE)
                                    + countInEntities(helper, Items.COBBLESTONE);
                    long packages = packagesIn(helper, containers);
                    long inChest = countInContainer(helper, chest, Items.COBBLESTONE);
                    helper.assertTrue(
                            cobble == 27 * 64, "1728 cobblestone went in, " + cobble + " exist");
                    helper.assertTrue(
                            inChest == 27 * 64 - 3 * 9 * 64,
                            "three packages of 9 stacks should have left the chest, it has "
                                    + inChest);
                    helper.assertTrue(packages == 3, packages + " packages exist, expected 3");
                    long pulledByFunnels =
                            helper.getLevel()
                                    .getEntitiesOfClass(
                                            com.simibubi.create.content.logistics.box.PackageEntity
                                                    .class,
                                            helper.getBounds().inflate(2))
                                    .size();
                    helper.assertTrue(
                            pulledByFunnels >= 2,
                            "funnels pulled " + pulledByFunnels + " packages, expected at least 2");
                    helper.assertTrue(
                            packagerAt(helper, packager).heldBox.isEmpty(),
                            "a package is still in the packager");
                    Create.LOGGER.info(
                            "[qa] 3 packages pulled by hopper + 2 funnels; hopper side {}, world"
                                    + " {}",
                            countInContainer(helper, hopper, Items.COBBLESTONE)
                                    + countInContainer(helper, hopperTarget, Items.COBBLESTONE),
                            countInEntities(helper, Items.COBBLESTONE));
                    helper.succeed();
                });
    }

    /**
     * A survival player breaking a packager that holds a package (during and after its animation)
     * gets the packager and the package, once.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void brokenPackagerDropsHeldPackageOnce(CreateGameTestHelper helper) {
        // Each packager sits on its chest (facing up, target below) and is powered by a lever,
        // so nothing solid is next to or above it: a dropped package that ends up inside a solid
        // block suffocates and pops open (upstream behaviour), which would muddle the count.
        BlockPos chestA = new BlockPos(1, 2, 2);
        BlockPos packagerA = new BlockPos(1, 3, 2); // broken while animating
        BlockPos chestB = new BlockPos(5, 2, 2);
        BlockPos packagerB = new BlockPos(5, 3, 2); // broken after the animation
        helper.setBlock(chestA, Blocks.CHEST);
        helper.setBlock(chestB, Blocks.CHEST);
        fillContainer(helper, chestA, new ItemStack(Items.REDSTONE, 7));
        fillContainer(helper, chestB, new ItemStack(Items.LAPIS_LAZULI, 9));
        helper.setBlock(packagerA, packager(Direction.UP));
        helper.setBlock(packagerB, packager(Direction.UP));
        BlockPos leverA = new BlockPos(0, 3, 2);
        BlockPos leverB = new BlockPos(6, 3, 2);
        for (BlockPos lever : List.of(leverA, leverB)) {
            helper.setBlock(lever.below(), Blocks.STONE);
            helper.setBlock(
                    lever,
                    Blocks.LEVER
                            .defaultBlockState()
                            .setValue(
                                    net.minecraft.world.level.block.LeverBlock.FACE,
                                    net.minecraft.world.level.block.state.properties.AttachFace
                                            .FLOOR));
        }
        helper.runAfterDelay(
                5,
                () -> {
                    helper.pullLever(leverA);
                    helper.pullLever(leverB);
                });
        FakePlayer player =
                FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "qa-breaker"));
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        helper.runAfterDelay(
                10,
                () -> { // qa-wrap: assertion messages survive
                    PackagerBlockEntity be = packagerAt(helper, packagerA);
                    helper.assertTrue(
                            !be.heldBox.isEmpty() && be.animationTicks > 0,
                            "packager A should be animating a package");
                    player.gameMode.destroyBlock(helper.absolutePos(packagerA));
                });
        helper.runAfterDelay(
                5 + PackagerBlockEntity.CYCLE + 10,
                () -> { // qa-wrap: assertion messages survive
                    PackagerBlockEntity be = packagerAt(helper, packagerB);
                    helper.assertTrue(
                            !be.heldBox.isEmpty() && be.animationTicks == 0,
                            "packager B should hold a finished package");
                    player.gameMode.destroyBlock(helper.absolutePos(packagerB));
                });
        helper.runAfterDelay(
                5 + PackagerBlockEntity.CYCLE + 15,
                () -> {
                    helper.assertBlockPresent(Blocks.AIR, packagerA);
                    helper.assertBlockPresent(Blocks.AIR, packagerB);
                    long packagers = 0;
                    long packages = 0;
                    for (ItemEntity e :
                            helper.getLevel()
                                    .getEntitiesOfClass(
                                            ItemEntity.class, helper.getBounds().inflate(2))) {
                        if (e.getItem().is(AllBlocks.PACKAGER.asItem()))
                            packagers += e.getItem().getCount();
                        if (PackageItem.isPackage(e.getItem())) packages += e.getItem().getCount();
                    }
                    // dropped packages turn into package entities
                    packages +=
                            helper.getLevel()
                                    .getEntitiesOfClass(
                                            com.simibubi.create.content.logistics.box.PackageEntity
                                                    .class,
                                            helper.getBounds().inflate(2))
                                    .size();
                    long redstone = countInEntities(helper, Items.REDSTONE);
                    long lapis = countInEntities(helper, Items.LAPIS_LAZULI);
                    helper.assertTrue(packagers == 2, packagers + " packager items dropped");
                    helper.assertTrue(
                            packages == 2,
                            packages
                                    + " packages dropped, expected 2 (redstone "
                                    + redstone
                                    + ", lapis "
                                    + lapis
                                    + ")");
                    helper.assertTrue(
                            redstone == 7 && lapis == 9,
                            "dropped " + redstone + " redstone, " + lapis + " lapis");
                    Create.LOGGER.info(
                            "[qa] broken packagers dropped 2 packagers + 2 packages, once");
                    helper.succeed();
                });
    }

    /**
     * Pipes from other mods nest transactions. A committed nested extraction inside an aborted
     * outer transaction, and an aborted nested one inside a committed outer one, both leave the
     * package in the packager; only a fully committed extraction takes it (and only once).
     */
    @GameTest(template = "flat_7x6x7")
    public static void nestedTransactionsRollBackPackagerExtraction(CreateGameTestHelper helper) {
        BlockPos packager = new BlockPos(2, 1, 2);
        helper.setBlock(packager, packager(Direction.UP));
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    PackagerBlockEntity be = packagerAt(helper, packager);
                    ItemStack box =
                            PackageItem.containing(List.of(new ItemStack(Items.DIAMOND, 2)));
                    be.heldBox = box.copy();
                    Storage<ItemVariant> storage = helper.itemStorageAt(packager);
                    ItemVariant variant = ItemVariant.of(box);
                    try (Transaction outer = Transaction.openOuter()) {
                        try (Transaction nested = outer.openNested()) {
                            helper.assertTrue(
                                    storage.extract(variant, 1, nested) == 1, "nested extract");
                            nested.commit();
                        }
                        helper.assertTrue(
                                be.heldBox.isEmpty(), "extracted inside the open transaction");
                        outer.abort();
                    }
                    helper.assertTrue(
                            ItemStack.isSameItemSameComponents(be.heldBox, box),
                            "outer abort must restore the package, have " + be.heldBox);

                    try (Transaction outer = Transaction.openOuter()) {
                        try (Transaction nested = outer.openNested()) {
                            storage.extract(variant, 1, nested);
                            nested.abort();
                        }
                        helper.assertTrue(
                                !be.heldBox.isEmpty(), "nested abort must restore the package");
                        outer.commit();
                    }
                    helper.assertTrue(!be.heldBox.isEmpty(), "package lost after nested abort");

                    // wrong resource and zero amounts take nothing
                    ItemVariant other =
                            ItemVariant.of(
                                    PackageItem.containing(List.of(new ItemStack(Items.DIRT))));
                    try (Transaction t = Transaction.openOuter()) {
                        long wrong = storage.extract(other, 1, t);
                        long zero = storage.extract(variant, 0, t);
                        t.commit();
                        helper.assertTrue(wrong == 0 && zero == 0, "took " + wrong + " / " + zero);
                    }
                    helper.assertTrue(!be.heldBox.isEmpty(), "package lost to a no-op extract");

                    try (Transaction t = Transaction.openOuter()) {
                        long a = storage.extract(variant, 1, t);
                        long b = storage.extract(variant, 1, t);
                        t.commit();
                        helper.assertTrue(a == 1 && b == 0, "took " + a + " then " + b);
                    }
                    helper.assertTrue(be.heldBox.isEmpty(), "committed extraction kept it");
                    Create.LOGGER.info("[qa] packager extraction follows nested transactions");
                    helper.succeed();
                });
    }

    /**
     * A comparator reading a packager shows 15 while it holds a package and drops to 0 once a
     * hopper has pulled the package out (the packager must announce the change when the extraction
     * commits).
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void comparatorSeesThePackageLeave(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(2, 3, 2);
        BlockPos packager = new BlockPos(2, 2, 2);
        BlockPos hopper = new BlockPos(2, 1, 2);
        BlockPos comparator = new BlockPos(3, 2, 2);
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.STONE);
        helper.setBlock(chest, Blocks.CHEST);
        fillContainer(helper, chest, new ItemStack(Items.QUARTZ, 4));
        helper.setBlock(packager, packager(Direction.DOWN));
        // the comparator faces its input (the packager, to the west)
        helper.setBlock(
                comparator,
                Blocks.COMPARATOR
                        .defaultBlockState()
                        .setValue(ComparatorBlock.FACING, Direction.WEST));
        // hopper locked by a redstone block until the package is ready
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.REDSTONE_BLOCK);
        helper.setBlock(
                hopper,
                Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));
        helper.runAfterDelay(
                5, () -> helper.setBlock(new BlockPos(1, 2, 2), Blocks.REDSTONE_BLOCK));
        int[] phase = {0};
        helper.succeedWhenWithDiagnostics(
                () -> {
                    int signal =
                            ((ComparatorBlockEntity) helper.getBlockEntity(comparator))
                                    .getOutputSignal();
                    if (phase[0] == 0) {
                        PackagerBlockEntity be = packagerAt(helper, packager);
                        helper.assertTrue(
                                !be.heldBox.isEmpty() && be.animationTicks == 0,
                                "no finished package yet");
                        helper.assertTrue(signal == 15, "comparator reads " + signal);
                        phase[0] = 1;
                        helper.setBlock(new BlockPos(2, 1, 3), Blocks.AIR); // unlock the hopper
                    }
                    helper.assertTrue(
                            packagerAt(helper, packager).heldBox.isEmpty(),
                            "hopper has not pulled yet");
                    helper.assertTrue(
                            signal == 0, "comparator still reads " + signal + " after the pull");
                    Create.LOGGER.info("[qa] comparator 15 -> 0 when the package left");
                },
                () ->
                        "phase "
                                + phase[0]
                                + " signal "
                                + ((ComparatorBlockEntity) helper.getBlockEntity(comparator))
                                        .getOutputSignal()
                                + " held "
                                + packagerAt(helper, packager).heldBox);
    }
}
