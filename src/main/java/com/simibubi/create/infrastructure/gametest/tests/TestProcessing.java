package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestGroup(path = "processing")
public class TestProcessing {
    @GameTest(template = "brass_mixing", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void brassMixing(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(2, 3, 2);
        BlockPos chest = new BlockPos(7, 3, 1);
        helper.pullLever(lever);
        helper.succeedWhenWithDiagnostics(
                () -> helper.assertContainerContains(chest, AllItems.BRASS_INGOT.get()),
                () ->
                        helper.snapshot(
                                lever,
                                new BlockPos(1, 3, 1),
                                new BlockPos(1, 3, 3),
                                new BlockPos(4, 3, 1),
                                new BlockPos(4, 5, 1),
                                new BlockPos(4, 6, 2)));
    }

    @GameTest(template = "brass_mixing_2", timeoutTicks = CreateGameTestHelper.TWENTY_SECONDS)
    public static void brassMixing2(CreateGameTestHelper helper) {
        BlockPos basinLever = new BlockPos(3, 3, 1);
        BlockPos armLever = new BlockPos(3, 3, 5);
        BlockPos output = new BlockPos(1, 2, 3);
        helper.pullLever(armLever);
        helper.whenSecondsPassed(7, () -> helper.pullLever(armLever));
        helper.whenSecondsPassed(10, () -> helper.pullLever(basinLever));
        helper.succeedWhenWithDiagnostics(
                () -> helper.assertContainerContains(output, AllItems.BRASS_INGOT.get()),
                () ->
                        helper.snapshot(
                                basinLever,
                                armLever,
                                new BlockPos(3, 3, 3),
                                new BlockPos(3, 5, 3),
                                new BlockPos(3, 6, 2),
                                new BlockPos(2, 3, 6),
                                new BlockPos(2, 3, 8),
                                new BlockPos(3, 3, 8)));
    }

    @GameTest(template = "potion_brewing", timeoutTicks = CreateGameTestHelper.THIRTY_SECONDS)
    public static void potionBrewing(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(8, 3, 5);
        BlockPos potionLever = new BlockPos(2, 3, 4);
        BlockPos bottleLever = new BlockPos(7, 3, 2);
        ItemStack expected = PotionContents.createItemStack(Items.POTION, Potions.HEALING);

        helper.pullLever(potionLever);
        helper.whenSecondsPassed(15, () -> helper.pullLever(bottleLever));
        helper.succeedWhen(() -> helper.assertContainerContains(chest, expected));
    }

    @GameTest(template = "spout_crafting", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void spoutCrafting(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(5, 3, 1);
        helper.pullLever(2, 3, 2);
        helper.succeedWhen(() -> helper.assertContainerContains(chest, Items.REDSTONE));
    }

    @GameTest(template = "crushing_wheel_crafting", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void crushingWheelCrafting(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(1, 4, 3);
        List<BlockPos> levers =
                List.of(new BlockPos(2, 3, 2), new BlockPos(6, 3, 2), new BlockPos(3, 7, 3));
        levers.forEach(helper::pullLever);
        ItemStack expected = new ItemStack(AllBlocks.CRUSHING_WHEEL.get(), 2);
        helper.succeedWhenWithDiagnostics(
                () -> helper.assertContainerContains(chest, expected),
                () ->
                        helper.snapshot(
                                new BlockPos(1, 3, 1),
                                new BlockPos(7, 3, 1),
                                new BlockPos(4, 8, 3),
                                new BlockPos(3, 2, 1),
                                new BlockPos(5, 2, 1),
                                new BlockPos(4, 4, 3),
                                new BlockPos(4, 3, 1)));
    }

    @GameTest(
            template = "precision_mechanism_crafting",
            timeoutTicks = CreateGameTestHelper.TWENTY_SECONDS)
    public static void precisionMechanismCrafting(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(6, 3, 6);
        BlockPos output = new BlockPos(11, 3, 1);
        helper.pullLever(lever);

        SequencedAssemblyRecipe recipe =
                (SequencedAssemblyRecipe)
                        helper.getLevel()
                                .getRecipeManager()
                                .byKey(Create.asResource("sequenced_assembly/precision_mechanism"))
                                .orElseThrow(
                                        () ->
                                                new GameTestAssertException(
                                                        "Precision Mechanism recipe not found"))
                                .value();
        Item result = recipe.getResultItem(helper.getLevel().registryAccess()).getItem();
        Item[] possibleResults =
                recipe.resultPool.stream()
                        .map(ProcessingOutput::getStack)
                        .map(ItemStack::getItem)
                        .filter(item -> item != result)
                        .toArray(Item[]::new);

        // fabric: upstream also required a junk output, but each sheet rolls junk with p = 0.2 and
        // all 16 can come out as mechanisms (0.8^16 = 2.8%, more when the run lags). Deterministic
        // instead: only result-pool items come out, a mechanism among them, and either a junk item
        // or all 16 sheets accounted for.
        Set<Item> pool = new HashSet<>(Arrays.asList(possibleResults));
        pool.add(result);
        helper.succeedWhenWithDiagnostics(
                () -> {
                    Object2LongMap<Item> content = helper.getItemContent(output);
                    for (Item item : content.keySet())
                        if (!pool.contains(item))
                            helper.fail("Unexpected output " + item + " in " + content);
                    helper.assertContainerContains(output, result);
                    boolean junk = content.keySet().stream().anyMatch(item -> item != result);
                    long total = content.values().longStream().sum();
                    if (!junk && total < 16)
                        helper.fail("Waiting for all 16 sheets (or a junk roll): " + content);
                },
                () ->
                        helper.snapshot(
                                lever,
                                new BlockPos(5, 3, 5),
                                new BlockPos(3, 4, 1),
                                new BlockPos(3, 4, 3),
                                new BlockPos(3, 4, 5),
                                new BlockPos(6, 2, 4)));
    }

    @GameTest(template = "sand_washing", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void sandWashing(CreateGameTestHelper helper) {
        BlockPos leverPos = new BlockPos(5, 3, 1);
        helper.pullLever(leverPos);
        BlockPos chestPos = new BlockPos(8, 3, 2);
        helper.succeedWhenWithDiagnostics(
                () -> helper.assertContainerContains(chestPos, Items.CLAY_BALL),
                () ->
                        helper.snapshot(
                                leverPos,
                                new BlockPos(5, 3, 3),
                                new BlockPos(2, 3, 2),
                                new BlockPos(1, 3, 2),
                                new BlockPos(7, 2, 3)));
    }

    @GameTest(
            template = "stone_cobble_sand_crushing",
            timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void stoneCobbleSandCrushing(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(1, 6, 2);
        BlockPos lever = new BlockPos(2, 3, 1);
        helper.pullLever(lever);
        ItemStack expected = new ItemStack(Items.SAND, 5);
        helper.succeedWhenWithDiagnostics(
                () -> helper.assertContainerContains(chest, expected),
                () ->
                        helper.snapshot(
                                lever,
                                new BlockPos(1, 3, 2),
                                new BlockPos(4, 3, 2),
                                new BlockPos(4, 6, 2),
                                new BlockPos(7, 7, 2),
                                new BlockPos(7, 8, 2)));
    }

    @GameTest(template = "track_crafting", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void trackCrafting(CreateGameTestHelper helper) {
        BlockPos output = new BlockPos(7, 3, 2);
        BlockPos lever = new BlockPos(2, 3, 1);
        helper.pullLever(lever);
        ItemStack expected = new ItemStack(AllBlocks.TRACK.get(), 6);
        helper.succeedWhenWithDiagnostics(
                () -> {
                    helper.assertContainerContains(output, expected);
                    Storage<ItemVariant> storage = helper.itemStorageAt(output);
                    ItemHelper.extract(storage, ItemHelper.sameItemPredicate(expected), 6, false);
                    helper.assertContainerEmpty(output);
                },
                () ->
                        helper.snapshot(
                                lever,
                                new BlockPos(1, 3, 2),
                                new BlockPos(3, 4, 2),
                                new BlockPos(4, 4, 2),
                                new BlockPos(5, 4, 2),
                                new BlockPos(6, 4, 2),
                                new BlockPos(3, 6, 2),
                                new BlockPos(4, 6, 2)));
    }

    @GameTest(template = "water_filling_bottle")
    public static void waterFillingBottle(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(3, 3, 3);
        BlockPos output = new BlockPos(2, 2, 4);
        ItemStack expected = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        helper.pullLever(lever);
        helper.succeedWhenWithDiagnostics(
                () -> helper.assertContainerContains(output, expected),
                () ->
                        helper.snapshot(
                                lever,
                                new BlockPos(2, 2, 3),
                                new BlockPos(2, 3, 4),
                                new BlockPos(2, 4, 1),
                                new BlockPos(4, 3, 3),
                                new BlockPos(4, 2, 4),
                                new BlockPos(3, 2, 4)));
    }

    @GameTest(template = "wheat_milling")
    public static void wheatMilling(CreateGameTestHelper helper) {
        BlockPos output = new BlockPos(1, 2, 1);
        BlockPos lever = new BlockPos(1, 7, 1);
        helper.pullLever(lever);
        ItemStack expected = new ItemStack(AllItems.WHEAT_FLOUR.get(), 3);
        helper.succeedWhenWithDiagnostics(
                () -> helper.assertContainerContains(output, expected),
                () ->
                        helper.snapshot(
                                lever,
                                new BlockPos(2, 7, 1),
                                new BlockPos(2, 6, 1),
                                new BlockPos(2, 3, 1),
                                new BlockPos(1, 4, 1)));
    }
}
