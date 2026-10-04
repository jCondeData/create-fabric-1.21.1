package com.simibubi.create.infrastructure.gametest.tests;

import static com.simibubi.create.infrastructure.gametest.tests.TestPortLogistics.*;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;

import java.util.List;

/**
 * BUG (fails on 3ec980de36): packages vanish when anything simulates an extraction from a packager.
 * PackagerItemHandler.extract clears PackagerBlockEntity.heldBox immediately instead of taking part
 * in the transaction, so the simulate-then-move pattern used by Create's own funnels and by
 * Fabric's StorageUtil.move (vanilla hoppers, other mods' pipes) deletes the package and its items.
 */
@GameTestGroup(path = "qa")
public class TestPortBugPackager {

    /**
     * Chest A -> packager A (redstone pulse) -> vanilla hopper pulls the package -> hopper pushes
     * it into packager B -> unpacked into chest B. Spec: packagers pack the attached inventory on a
     * redstone pulse and unpack packages they receive into the attached inventory; nothing is lost.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TWENTY_SECONDS)
    public static void packageTravelsThroughHopperBetweenPackagers(CreateGameTestHelper helper) {
        BlockPos chestA = new BlockPos(1, 3, 1);
        BlockPos packagerA = new BlockPos(1, 2, 1);
        BlockPos hopper = new BlockPos(1, 1, 1);
        BlockPos packagerB = new BlockPos(2, 1, 1);
        BlockPos chestB = new BlockPos(2, 1, 2);

        helper.setBlock(chestA, Blocks.CHEST);
        fillContainer(
                helper,
                chestA,
                new ItemStack(Items.IRON_INGOT, 16),
                new ItemStack(Items.DIAMOND, 4));
        helper.setBlock(packagerA, packager(Direction.DOWN)); // target = block above
        helper.setBlock(
                hopper,
                Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        helper.setBlock(packagerB, packager(Direction.NORTH)); // target = block south (chest B)
        helper.setBlock(chestB, Blocks.CHEST);

        helper.runAfterDelay(
                10, () -> helper.setBlock(new BlockPos(0, 2, 1), Blocks.REDSTONE_BLOCK));

        helper.succeedWhenWithDiagnostics(
                () -> {
                    long iron = countInContainer(helper, chestB, Items.IRON_INGOT);
                    long diamonds = countInContainer(helper, chestB, Items.DIAMOND);
                    if (iron != 16 || diamonds != 4)
                        helper.fail("chest B has " + iron + " iron, " + diamonds + " diamonds");
                    helper.assertTrue(
                            countInContainer(helper, chestA, Items.IRON_INGOT) == 0,
                            "chest A should be emptied by the packager");
                    Create.LOGGER.info(
                            "[qa] packager->hopper->packager moved 16 iron + 4 diamonds");
                },
                () -> {
                    Item[] items = {Items.IRON_INGOT, Items.DIAMOND};
                    StringBuilder sb = new StringBuilder();
                    for (Item item : items) {
                        sb.append(item)
                                .append(": chestA=")
                                .append(countInContainer(helper, chestA, item))
                                .append(" packagerA=")
                                .append(countInPackager(helper, packagerA, item))
                                .append(" hopper=")
                                .append(countInContainer(helper, hopper, item))
                                .append(" packagerB=")
                                .append(countInPackager(helper, packagerB, item))
                                .append(" chestB=")
                                .append(countInContainer(helper, chestB, item))
                                .append(" entities=")
                                .append(countInEntities(helper, item))
                                .append("; ");
                    }
                    return sb.toString();
                });
    }

    /**
     * Root cause of the two tests above: a simulated extraction (a transaction that is aborted, as
     * Fabric's StorageUtil.move and Create's funnels do before every real move) must leave the
     * packager's package where it was.
     */
    @GameTest(template = "flat_7x6x7")
    public static void packagerKeepsPackageWhenExtractionIsRolledBack(CreateGameTestHelper helper) {
        BlockPos packager = new BlockPos(2, 1, 2);
        helper.setBlock(packager, packager(Direction.UP));
        helper.runAfterDelay(
                1,
                () -> {
                    PackagerBlockEntity be = (PackagerBlockEntity) helper.getBlockEntity(packager);
                    ItemStack box =
                            PackageItem.containing(List.of(new ItemStack(Items.DIAMOND, 2)));
                    be.heldBox = box.copy();
                    Storage<ItemVariant> storage = helper.itemStorageAt(packager);
                    try (Transaction t = Transaction.openOuter()) {
                        long simulated = storage.extract(ItemVariant.of(box), 1, t);
                        helper.assertTrue(
                                simulated == 1, "packager offered " + simulated + " packages");
                        t.abort();
                    }
                    helper.assertTrue(
                            !be.heldBox.isEmpty(),
                            "an aborted extraction deleted the packager's package"
                                + " (PackagerItemHandler.extract clears heldBox without taking part"
                                + " in the transaction)");
                    helper.succeed();
                });
    }

    /**
     * A Create funnel pulling from a packager (the setup Create's ponder shows): the funnel
     * simulates the extraction first, then extracts for real. The package must come out of the
     * packager (as a package entity under the funnel) or stay in it; it must never vanish.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void funnelPullsPackageOutOfPackager(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(2, 4, 2);
        BlockPos packager = new BlockPos(2, 3, 2);
        BlockPos funnel = new BlockPos(2, 2, 2);

        helper.setBlock(chest, Blocks.CHEST);
        fillContainer(helper, chest, new ItemStack(Items.GOLD_INGOT, 8));
        helper.setBlock(packager, packager(Direction.DOWN));
        helper.setBlock(
                funnel,
                AllBlocks.ANDESITE_FUNNEL
                        .getDefaultState()
                        .setValue(FunnelBlock.FACING, Direction.DOWN)
                        .setValue(FunnelBlock.EXTRACTING, true));
        helper.runAfterDelay(
                10, () -> helper.setBlock(new BlockPos(1, 3, 2), Blocks.REDSTONE_BLOCK));

        helper.runAfterDelay(
                100,
                () -> {
                    long inChest = countInContainer(helper, chest, Items.GOLD_INGOT);
                    long inPackager = countInPackager(helper, packager, Items.GOLD_INGOT);
                    long inWorld = countInEntities(helper, Items.GOLD_INGOT);
                    long total = inChest + inPackager + inWorld;
                    if (total != 8)
                        helper.fail(
                                "8 gold ingots went in, "
                                        + total
                                        + " are left (chest "
                                        + inChest
                                        + ", packager "
                                        + inPackager
                                        + ", dropped "
                                        + inWorld
                                        + ")");
                    helper.assertTrue(
                            inWorld == 8,
                            "funnel should have pulled the package out, in world: " + inWorld);
                    Create.LOGGER.info(
                            "[qa] funnel pulled the package: {} gold in the world", inWorld);
                    helper.succeed();
                });
    }
}
