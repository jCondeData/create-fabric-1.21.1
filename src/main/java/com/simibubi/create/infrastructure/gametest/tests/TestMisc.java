package com.simibubi.create.infrastructure.gametest.tests;

import static com.simibubi.create.infrastructure.gametest.CreateGameTestHelper.FIFTEEN_SECONDS;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlockEntity;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity.State;
import com.simibubi.create.foundation.utility.CreatePaths;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;

@GameTestGroup(path = "misc")
public class TestMisc {
    @GameTest(template = "schematicannon", timeoutTicks = FIFTEEN_SECONDS)
    public static void schematicannon(CreateGameTestHelper helper) {
        // load the structure
        BlockPos whiteEndBottom = helper.absolutePos(new BlockPos(5, 2, 1));
        BlockPos redEndTop = helper.absolutePos(new BlockPos(5, 4, 7));
        ServerLevel level = helper.getLevel();
        SchematicExport.saveSchematic(
                CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve("Deployer"),
                "schematicannon_gametest",
                true,
                level,
                whiteEndBottom,
                redEndTop);
        ItemStack schematic =
                SchematicItem.create(level, "schematicannon_gametest.nbt", "Deployer");
        // deploy to pos
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        schematic.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
        schematic.set(AllDataComponents.SCHEMATIC_ANCHOR, anchor);
        // setup cannon
        BlockPos cannonPos = new BlockPos(3, 2, 6);
        SchematicannonBlockEntity cannon =
                helper.getBlockEntity(AllBlockEntityTypes.SCHEMATICANNON.get(), cannonPos);
        cannon.inventory.setStackInSlot(0, schematic);
        // run
        cannon.state = State.RUNNING;
        cannon.statusMsg = "running";
        helper.succeedWhenWithDiagnostics(
                () -> {
                    if (cannon.state != State.STOPPED) {
                        helper.fail("Schematicannon not done");
                    }
                    BlockPos lastBlock = new BlockPos(1, 4, 7);
                    helper.assertBlockPresent(Blocks.RED_WOOL, lastBlock);
                },
                () ->
                        "state="
                                + cannon.state
                                + ", status="
                                + cannon.statusMsg
                                + ", loaded="
                                + cannon.printer.isLoaded()
                                + ", errored="
                                + cannon.printer.isErrored()
                                + ", target="
                                + cannon.printer.getCurrentTarget()
                                + ", creative="
                                + cannon.hasCreativeCrate
                                + ", fuel="
                                + cannon.remainingFuel
                                + "; "
                                + helper.snapshot(cannonPos, cannonPos.north()));
    }

    @GameTest(template = "shearing")
    public static void shearing(CreateGameTestHelper helper) {
        BlockPos sheepPos = new BlockPos(2, 1, 2);
        Sheep sheep = helper.getFirstEntity(EntityType.SHEEP, sheepPos);
        sheep.shear(SoundSource.NEUTRAL);
        helper.succeedWhen(
                () -> {
                    helper.assertItemEntityPresent(Items.WHITE_WOOL, sheepPos, 2);
                });
    }

    @GameTest(template = "smart_observer_blocks")
    public static void smartObserverBlocks(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(2, 2, 1);
        BlockPos leftLamp = new BlockPos(3, 4, 3);
        BlockPos rightLamp = new BlockPos(1, 4, 3);
        helper.pullLever(lever);
        helper.succeedWhenWithDiagnostics(
                () -> {
                    helper.assertBlockProperty(leftLamp, RedstoneLampBlock.LIT, true);
                    helper.assertBlockProperty(rightLamp, RedstoneLampBlock.LIT, false);
                },
                () ->
                        helper.snapshot(
                                lever,
                                new BlockPos(1, 2, 1),
                                new BlockPos(3, 2, 1),
                                new BlockPos(1, 4, 1),
                                new BlockPos(3, 4, 1),
                                new BlockPos(1, 4, 2),
                                new BlockPos(3, 4, 2),
                                leftLamp,
                                rightLamp));
    }

    @GameTest(template = "threshold_switch_pulley")
    public static void thresholdSwitchPulley(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(3, 7, 1);
        BlockPos switchPos = new BlockPos(1, 6, 1);
        BlockPos finalPos = new BlockPos(2, 2, 1);
        SequencedGearshiftBlockEntity gearshift =
                helper.getBlockEntity(
                        AllBlockEntityTypes.SEQUENCED_GEARSHIFT.get(), switchPos.east(2));
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        gearshift.getSpeed() != 0,
                                        "Waiting for gearshift kinetics"))
                .thenExecute(() -> helper.pullLever(lever));
        helper.succeedWhenWithDiagnostics(
                () -> {
                    ThresholdSwitchBlockEntity switchBe =
                            helper.getBlockEntity(
                                    AllBlockEntityTypes.THRESHOLD_SWITCH.get(), switchPos);
                    int level = switchBe.getStockLevel();
                    int expectedLevel = helper.absolutePos(finalPos).getY();
                    if (level != expectedLevel)
                        helper.fail(
                                "Unexpected level: "
                                        + level
                                        + "; bounds: "
                                        + switchBe.getMinLevel()
                                        + ".."
                                        + switchBe.getMaxLevel()
                                        + "; state: "
                                        + switchBe.getBlockState());
                },
                () ->
                        helper.snapshot(
                                switchPos,
                                switchPos.east(),
                                switchPos.east(2),
                                switchPos.east(3),
                                lever));
    }

    @GameTest(template = "netherite_backtank", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void netheriteBacktank(CreateGameTestHelper helper) {
        BlockPos lava = new BlockPos(2, 2, 3);
        BlockPos zombieSpawn = lava.above(2);
        BlockPos armorStandPos = new BlockPos(2, 2, 1);
        Zombie[] spawned = new Zombie[1];
        ItemStack[] equipment = new ItemStack[EquipmentSlot.values().length];
        helper.runAtTickTime(
                5,
                () -> {
                    Zombie zombie = helper.spawn(EntityType.ZOMBIE, zombieSpawn);
                    spawned[0] = zombie;
                    ArmorStand armorStand =
                            helper.getFirstEntity(EntityType.ARMOR_STAND, armorStandPos);
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        equipment[slot.ordinal()] = armorStand.getItemBySlot(slot).copy();
                        zombie.setItemSlot(slot, equipment[slot.ordinal()].copy());
                    }
                });
        helper.succeedWhenWithDiagnostics(
                () -> {
                    helper.assertSecondsPassed(9);
                    helper.assertTrue(spawned[0] != null, "Zombie was not spawned");
                    helper.assertTrue(spawned[0].isAlive(), "Zombie died");
                    helper.assertTrue(!spawned[0].isRemoved(), "Zombie was removed");
                    helper.assertTrue(spawned[0].getHealth() > 0, "Zombie has no health");
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        helper.assertTrue(
                                ItemStack.isSameItem(
                                        spawned[0].getItemBySlot(slot), equipment[slot.ordinal()]),
                                "Zombie changed equipment in " + slot);
                    }
                    helper.assertEntityPresent(EntityType.ZOMBIE, lava);
                },
                () ->
                        spawned[0] == null
                                ? "zombie not spawned"
                                : "health="
                                        + spawned[0].getHealth()
                                        + ", alive="
                                        + spawned[0].isAlive()
                                        + ", removed="
                                        + spawned[0].isRemoved()
                                        + ", fireImmune="
                                        + spawned[0].fireImmune()
                                        + ", fireTicks="
                                        + spawned[0].getRemainingFireTicks()
                                        + ", air="
                                        + spawned[0].getAirSupply()
                                        + ", position="
                                        + spawned[0].position()
                                        + ", data="
                                        + spawned[0].saveWithoutId(new CompoundTag())
                                        + "; "
                                        + helper.snapshot(lava, armorStandPos));
    }
}
