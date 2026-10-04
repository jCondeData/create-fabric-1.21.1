package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import java.util.List;

/** Real machines driven by a creative motor or by item transfer, no prebuilt structure. */
@GameTestGroup(path = "qa")
public class TestPortProcessing {

    /**
     * A millstone turned by a creative motor mills 2 clay blocks into 8 clay balls (6.0.9: clay
     * always gives 4 clay balls), and nothing else is left behind.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TWENTY_SECONDS)
    public static void millstoneMillsClayIntoFourBallsEach(CreateGameTestHelper helper) {
        BlockPos motor = new BlockPos(2, 1, 2);
        BlockPos millstone = new BlockPos(2, 2, 2);
        helper.setBlock(
                motor,
                AllBlocks.CREATIVE_MOTOR
                        .getDefaultState()
                        .setValue(DirectionalKineticBlock.FACING, Direction.UP));
        helper.setBlock(millstone, AllBlocks.MILLSTONE.getDefaultState());
        try (Transaction t = Transaction.openOuter()) {
            long in = helper.itemStorageAt(millstone).insert(ItemVariant.of(Items.CLAY), 2, t);
            helper.assertTrue(in == 2, "millstone accepted " + in + " clay");
            t.commit();
        }
        helper.succeedWhenWithDiagnostics(
                () -> {
                    long balls =
                            TestPortLogistics.countIn(
                                    helper.itemStorageAt(millstone), Items.CLAY_BALL);
                    long clay =
                            TestPortLogistics.countIn(helper.itemStorageAt(millstone), Items.CLAY);
                    helper.assertTrue(
                            clay == 0 && balls == 8,
                            "millstone holds " + clay + " clay, " + balls + " clay balls");
                    Create.LOGGER.info("[qa] millstone: 2 clay -> {} clay balls", balls);
                },
                () -> "");
    }

    /**
     * A hopper holding a package pushes it into a packager, which unpacks it into the chest behind
     * it inside the hopper's transaction (the unpack handlers must nest, not open an outer one).
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void hopperDeliversPackageIntoPackager(CreateGameTestHelper helper) {
        BlockPos hopper = new BlockPos(1, 1, 1);
        BlockPos packager = new BlockPos(2, 1, 1);
        BlockPos chest = new BlockPos(2, 1, 2);
        helper.setBlock(
                hopper,
                Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        helper.setBlock(
                packager,
                AllBlocks.PACKAGER
                        .getDefaultState()
                        .setValue(PackagerBlock.FACING, Direction.NORTH));
        helper.setBlock(chest, Blocks.CHEST);
        ItemStack box =
                PackageItem.containing(
                        List.of(new ItemStack(Items.EMERALD, 12), new ItemStack(Items.ARROW, 64)));
        ((HopperBlockEntity) helper.getBlockEntity(hopper)).setItem(0, box);
        helper.succeedWhenWithDiagnostics(
                () -> {
                    long emeralds =
                            TestPortLogistics.countInContainer(helper, chest, Items.EMERALD);
                    long arrows = TestPortLogistics.countInContainer(helper, chest, Items.ARROW);
                    helper.assertTrue(
                            emeralds == 12 && arrows == 64,
                            "chest got " + emeralds + " emeralds, " + arrows + " arrows");
                    helper.assertTrue(
                            TestPortLogistics.countInContainer(helper, hopper, Items.EMERALD) == 0,
                            "package still in hopper (duplicated?)");
                    Create.LOGGER.info("[qa] hopper delivered a package into a packager");
                },
                () -> "hopper: " + ((HopperBlockEntity) helper.getBlockEntity(hopper)).getItem(0));
    }

    /**
     * A packager with nothing behind it cannot unpack: a package pushed into it by a hopper must
     * stay in the hopper instead of vanishing.
     */
    @GameTest(template = "flat_7x6x7", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void packagerWithoutInventoryRefusesPackages(CreateGameTestHelper helper) {
        BlockPos hopper = new BlockPos(1, 1, 1);
        BlockPos packager = new BlockPos(2, 1, 1);
        helper.setBlock(
                hopper,
                Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        helper.setBlock(
                packager,
                AllBlocks.PACKAGER
                        .getDefaultState()
                        .setValue(PackagerBlock.FACING, Direction.NORTH));
        ItemStack box = PackageItem.containing(List.of(new ItemStack(Items.GOLD_NUGGET, 9)));
        ((HopperBlockEntity) helper.getBlockEntity(hopper)).setItem(0, box);
        helper.runAfterDelay(
                60,
                () -> {
                    long nuggets =
                            TestPortLogistics.countInContainer(helper, hopper, Items.GOLD_NUGGET);
                    helper.assertTrue(
                            nuggets == 9,
                            "package should wait in the hopper, hopper holds "
                                    + nuggets
                                    + " nuggets");
                    helper.assertTrue(
                            TestPortLogistics.countInEntities(helper, Items.GOLD_NUGGET) == 0,
                            "nuggets dropped");
                    helper.succeed();
                });
    }
}
