package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity.SelectionMode;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayLayout;
import com.simibubi.create.content.trains.display.FlapDisplaySection;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;
import com.simibubi.create.infrastructure.fabric.transfer.TransferUtil;
import com.simibubi.create.infrastructure.fabric.transfer.item.ItemStackHandler;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@GameTestGroup(path = "items")
public class TestItems {
    @GameTest(template = "arm_purgatory")
    public static void exactExtractionFallback(CreateGameTestHelper helper) {
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, new ItemStack(Items.STONE));
        inventory.setStackInSlot(1, new ItemStack(Items.DIAMOND, 3));
        ItemStack expected = new ItemStack(Items.DIAMOND, 2);

        ItemStack simulated =
                ItemHelper.extract(inventory, stack -> true, ExtractionCountMode.EXACTLY, 2, true);
        helper.assertTrue(
                ItemStack.matches(expected, simulated), "Exact fallback chose wrong item");
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 1, "Simulation consumed stone");
        helper.assertTrue(
                inventory.getStackInSlot(1).getCount() == 3, "Simulation consumed diamonds");

        ItemStack extracted =
                ItemHelper.extract(inventory, stack -> true, ExtractionCountMode.EXACTLY, 2, false);
        helper.assertTrue(
                ItemStack.matches(expected, extracted), "Exact fallback extracted wrong item");
        helper.assertTrue(
                inventory.getStackInSlot(0).getCount() == 1, "Fallback consumed rejected stone");
        helper.assertTrue(
                inventory.getStackInSlot(1).getCount() == 1, "Fallback consumed wrong amount");
        helper.assertTrue(
                ItemHelper.extract(inventory, stack -> true, ExtractionCountMode.EXACTLY, 2, false)
                        .isEmpty(),
                "Insufficient exact extraction succeeded");
        helper.assertTrue(
                inventory.getStackInSlot(0).getCount() == 1
                        && inventory.getStackInSlot(1).getCount() == 1,
                "Insufficient exact extraction consumed items");
        helper.succeed();
    }

    @GameTest(template = "arm_purgatory")
    public static void armFunnelTransactions(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(3, 2, 1);
        BlockPos funnel = chest.north();
        helper.setBlock(chest, Blocks.AIR);
        helper.setBlock(chest, Blocks.CHEST);
        var chestEntity = helper.getBlockEntity(BlockEntityType.CHEST, chest);
        helper.setBlock(
                funnel,
                AllBlocks.ANDESITE_FUNNEL
                        .get()
                        .defaultBlockState()
                        .setValue(FunnelBlock.FACING, Direction.NORTH)
                        .setValue(FunnelBlock.EXTRACTING, false));
        InvManipulationBehaviour inserter =
                helper.getBehavior(funnel, InvManipulationBehaviour.TYPE);
        inserter.findNewCapability();
        ArmInteractionPoint point =
                ArmInteractionPoint.create(
                        helper.getLevel(),
                        helper.absolutePos(funnel),
                        helper.getBlockState(funnel));
        helper.assertTrue(point != null, "Funnel arm interaction point missing");
        ItemStack stack = new ItemStack(Items.DIAMOND, 4);

        try (Transaction outer = Transaction.openOuter()) {
            helper.assertTrue(
                    point.insert(stack, outer).isEmpty(), "Arm funnel insertion rejected items");
            helper.assertTrue(
                    ItemStack.matches(stack, chestEntity.getItem(0)),
                    "Arm insertion put wrong amount in chest");
        }
        helper.assertContainerEmpty(chest);

        try (Transaction outer = Transaction.openOuter()) {
            helper.assertTrue(
                    inserter.simulate().insert(stack, outer).isEmpty(),
                    "Simulated funnel insertion rejected items");
            helper.assertContainerEmpty(chest);
            outer.commit();
        }
        helper.assertContainerEmpty(chest);

        try (Transaction outer = Transaction.openOuter()) {
            helper.assertTrue(
                    point.insert(stack, outer).isEmpty(), "Committed arm insertion rejected items");
            outer.commit();
        }
        helper.assertTrue(
                ItemStack.matches(stack, chestEntity.getItem(0)),
                "Arm insertion commit lost items or duplicated them");
        helper.assertTrue(stack.getCount() == 4, "Arm funnel insertion mutated caller's stack");
        helper.succeed();
    }

    @GameTest(template = "andesite_tunnel_split")
    public static void andesiteTunnelSplit(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(2, 6, 2);
        helper.pullLever(lever);
        Map<BlockPos, ItemStack> outputs =
                Map.of(
                        new BlockPos(2, 2, 1), new ItemStack(AllItems.BRASS_INGOT.get(), 1),
                        new BlockPos(3, 2, 1), new ItemStack(AllItems.BRASS_INGOT.get(), 1),
                        new BlockPos(4, 2, 2), new ItemStack(AllItems.BRASS_INGOT.get(), 3));
        helper.succeedWhen(() -> outputs.forEach(helper::assertContainerContains));
    }

    @GameTest(template = "arm_multi_output", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void armMultiOutput(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(2, 3, 1);
        BlockPos[] blazeBurners =
                IntStream.rangeClosed(6, 8)
                        .boxed()
                        .flatMap(
                                x ->
                                        IntStream.rangeClosed(1, 3)
                                                .mapToObj(z -> new BlockPos(x, 2, z)))
                        .toArray(BlockPos[]::new);
        helper.pullLever(lever);
        helper.succeedWhen(
                () -> {
                    for (BlockPos pos : blazeBurners)
                        helper.assertBlockState(
                                pos,
                                state ->
                                        state.getValue(BlazeBurnerBlock.HEAT_LEVEL)
                                                == HeatLevel.KINDLED,
                                () -> "Blaze burner isn't lit!");
                });
    }

    @GameTest(template = "arm_purgatory", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void armPurgatory(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(2, 3, 2);
        BlockPos depot1Pos = new BlockPos(3, 2, 1);
        DepotBlockEntity depot1 = helper.getBlockEntity(AllBlockEntityTypes.DEPOT.get(), depot1Pos);
        BlockPos depot2Pos = new BlockPos(1, 2, 1);
        DepotBlockEntity depot2 = helper.getBlockEntity(AllBlockEntityTypes.DEPOT.get(), depot2Pos);
        helper.pullLever(lever);
        helper.succeedWhen(
                () -> {
                    helper.assertSecondsPassed(5);
                    ItemStack held1 = depot1.getHeldItem();
                    boolean held1Empty = held1.isEmpty();
                    int held1Count = held1.getCount();
                    ItemStack held2 = depot2.getHeldItem();
                    boolean held2Empty = held2.isEmpty();
                    int held2Count = held2.getCount();
                    if (held1Empty && held2Empty) helper.fail("No item present");
                    if (!held1Empty && held1Count != 1)
                        helper.fail("Unexpected count on depot 1: " + held1Count);
                    if (!held2Empty && held2Count != 1)
                        helper.fail("Unexpected count on depot 2: " + held2Count);
                    if (held1Count + held2Count != 1)
                        helper.fail("Arm simulation duplicated the transferred item");
                });
    }

    @GameTest(template = "attribute_filters", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void attributeFilters(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(2, 3, 1);
        BlockPos end = new BlockPos(11, 2, 2);
        Holder<Enchantment> PROTECTION_ENCHANT =
                helper.getLevel()
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.PROTECTION);
        Map<BlockPos, ItemStack> outputs =
                Map.of(
                        new BlockPos(3, 2, 1), new ItemStack(AllBlocks.BRASS_BLOCK.get()),
                        new BlockPos(4, 2, 1), new ItemStack(Items.APPLE),
                        new BlockPos(5, 2, 1), new ItemStack(Items.WATER_BUCKET),
                        new BlockPos(6, 2, 1),
                                EnchantedBookItem.createForEnchantment(
                                        new EnchantmentInstance(PROTECTION_ENCHANT, 1)),
                        new BlockPos(7, 2, 1),
                                Util.make(
                                        new ItemStack(Items.NETHERITE_SWORD),
                                        s -> s.setDamageValue(1)),
                        new BlockPos(8, 2, 1), new ItemStack(Items.IRON_HELMET),
                        new BlockPos(9, 2, 1), new ItemStack(Items.COAL),
                        new BlockPos(10, 2, 1), new ItemStack(Items.POTATO));
        helper.pullLever(lever);
        helper.succeedWhen(
                () -> {
                    outputs.forEach(helper::assertContainerContains);
                    helper.assertContainerEmpty(end);
                });
    }

    @GameTest(template = "belt_coaster", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void beltCoaster(CreateGameTestHelper helper) {
        BlockPos input = new BlockPos(1, 5, 6);
        BlockPos output = new BlockPos(3, 8, 6);
        BlockPos lever = new BlockPos(1, 5, 5);
        helper.pullLever(lever);
        helper.succeedWhen(
                () -> {
                    long outputItems = helper.getTotalItems(output);
                    if (outputItems != 27) helper.fail("Expected 27 items, got " + outputItems);
                    long remainingItems = helper.getTotalItems(input);
                    if (remainingItems != 2)
                        helper.fail("Expected 2 items remaining, got " + remainingItems);
                });
    }

    @GameTest(template = "brass_tunnel_filtering")
    public static void brassTunnelFiltering(CreateGameTestHelper helper) {
        Map<BlockPos, ItemStack> outputs =
                Map.of(
                        new BlockPos(3, 2, 2), new ItemStack(Items.COPPER_INGOT, 13),
                        new BlockPos(4, 2, 3), new ItemStack(AllItems.ZINC_INGOT.get(), 4),
                        new BlockPos(4, 2, 4), new ItemStack(Items.IRON_INGOT, 2),
                        new BlockPos(4, 2, 5), new ItemStack(Items.GOLD_INGOT, 24),
                        new BlockPos(3, 2, 6), new ItemStack(Items.DIAMOND, 17));
        BlockPos lever = new BlockPos(2, 3, 2);
        helper.pullLever(lever);
        helper.succeedWhen(() -> outputs.forEach(helper::assertContainerContains));
    }

    @GameTest(
            template = "brass_tunnel_prefer_nearest",
            timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void brassTunnelPreferNearest(CreateGameTestHelper helper) {
        List<BlockPos> tunnels =
                List.of(new BlockPos(3, 3, 1), new BlockPos(3, 3, 2), new BlockPos(3, 3, 3));
        List<BlockPos> out =
                List.of(new BlockPos(5, 2, 1), new BlockPos(5, 2, 2), new BlockPos(5, 2, 3));
        BlockPos lever = new BlockPos(2, 3, 2);
        helper.pullLever(lever);
        // tunnels reconnect and lose their modes
        tunnels.forEach(tunnel -> helper.setTunnelMode(tunnel, SelectionMode.PREFER_NEAREST));
        helper.succeedWhen(
                () ->
                        out.forEach(
                                pos ->
                                        helper.assertContainerContains(
                                                pos, AllBlocks.BRASS_CASING.get())));
    }

    @GameTest(
            template = "brass_tunnel_round_robin",
            timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void brassTunnelRoundRobin(CreateGameTestHelper helper) {
        List<BlockPos> outputs =
                List.of(new BlockPos(7, 3, 1), new BlockPos(7, 3, 2), new BlockPos(7, 3, 3));
        brassTunnelModeTest(helper, SelectionMode.ROUND_ROBIN, outputs);
    }

    @GameTest(template = "brass_tunnel_split")
    public static void brassTunnelSplit(CreateGameTestHelper helper) {
        List<BlockPos> outputs =
                List.of(new BlockPos(7, 2, 1), new BlockPos(7, 2, 2), new BlockPos(7, 2, 3));
        brassTunnelModeTest(helper, SelectionMode.SPLIT, outputs);
    }

    private static void brassTunnelModeTest(
            CreateGameTestHelper helper, SelectionMode mode, List<BlockPos> outputs) {
        BlockPos lever = new BlockPos(2, 3, 2);
        List<BlockPos> tunnels =
                List.of(new BlockPos(3, 3, 1), new BlockPos(3, 3, 2), new BlockPos(3, 3, 3));
        helper.pullLever(lever);
        tunnels.forEach(tunnel -> helper.setTunnelMode(tunnel, mode));
        helper.succeedWhen(
                () -> {
                    long items = 0;
                    for (BlockPos out : outputs) {
                        helper.assertContainerContains(out, AllBlocks.BRASS_CASING.get());
                        items += helper.getTotalItems(out);
                    }
                    if (items != 10) helper.fail("expected 10 items, got " + items);
                });
    }

    @GameTest(template = "brass_tunnel_sync_input", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void brassTunnelSyncInput(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(1, 3, 2);
        List<BlockPos> redstoneBlocks =
                List.of(new BlockPos(3, 4, 1), new BlockPos(3, 4, 2), new BlockPos(3, 4, 3));
        List<BlockPos> tunnels =
                List.of(new BlockPos(5, 3, 1), new BlockPos(5, 3, 2), new BlockPos(5, 3, 3));
        List<BlockPos> outputs =
                List.of(new BlockPos(7, 2, 1), new BlockPos(7, 2, 2), new BlockPos(7, 2, 3));
        helper.pullLever(lever);
        tunnels.forEach(tunnel -> helper.setTunnelMode(tunnel, SelectionMode.SYNCHRONIZE));
        helper.succeedWhen(
                () -> {
                    if (helper.secondsPassed() < 9) {
                        helper.setBlock(redstoneBlocks.get(0), Blocks.AIR);
                        helper.assertSecondsPassed(3);
                        outputs.forEach(helper::assertContainerEmpty);
                        helper.setBlock(redstoneBlocks.get(1), Blocks.AIR);
                        helper.assertSecondsPassed(6);
                        outputs.forEach(helper::assertContainerEmpty);
                        helper.setBlock(redstoneBlocks.get(2), Blocks.AIR);
                        helper.assertSecondsPassed(9);
                    } else {
                        outputs.forEach(
                                out ->
                                        helper.assertContainerContains(
                                                out, AllBlocks.BRASS_CASING.get()));
                    }
                });
    }

    @GameTest(
            template = "smart_observer_belt_and_funnel",
            timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void smartObserverBeltAndFunnel(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(6, 3, 2);
        List<BlockPos> targets =
                List.of(
                        new BlockPos(5, 2, 1), // belt
                        new BlockPos(2, 4, 6) // funnel
                        );
        List<BlockPos> overflows =
                List.of(
                        new BlockPos(6, 2, 1), // belt
                        new BlockPos(1, 3, 6) // funnel
                        );
        helper.pullLever(lever);
        helper.succeedWhen(
                () -> {
                    helper.assertSecondsPassed(9);
                    targets.forEach(pos -> helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, pos));
                    overflows.forEach(pos -> helper.assertBlockPresent(Blocks.AIR, pos));
                });
    }

    @GameTest(template = "smart_observer_chutes")
    public static void smartObserverChutes(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(1, 5, 2);
        BlockPos output = new BlockPos(1, 5, 3);
        helper.pullLever(lever);
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, output));
    }

    @GameTest(template = "smart_observer_counting")
    public static void smartObserverCounting(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(3, 2, 1);
        long totalChestItems = helper.getTotalItems(chest);
        BlockPos chestNixiePos = new BlockPos(2, 3, 1);
        NixieTubeBlockEntity chestNixie =
                helper.getBlockEntity(AllBlockEntityTypes.NIXIE_TUBE.get(), chestNixiePos);

        BlockPos doubleChest = new BlockPos(2, 2, 3);
        long totalDoubleChestItems = helper.getTotalItems(doubleChest);
        BlockPos doubleChestNixiePos = new BlockPos(1, 3, 3);
        NixieTubeBlockEntity doubleChestNixie =
                helper.getBlockEntity(AllBlockEntityTypes.NIXIE_TUBE.get(), doubleChestNixiePos);

        helper.succeedWhen(
                () -> {
                    String chestNixieText = chestNixie.getFullText().getString();
                    long chestNixieReading = Long.parseLong(chestNixieText);
                    if (chestNixieReading != totalChestItems)
                        helper.fail(
                                "Chest nixie detected %s, expected %s"
                                        .formatted(chestNixieReading, totalChestItems));
                    String doubleChestNixieText = doubleChestNixie.getFullText().getString();
                    long doubleChestNixieReading = Long.parseLong(doubleChestNixieText);
                    if (doubleChestNixieReading != totalDoubleChestItems)
                        helper.fail(
                                "Double chest nixie detected %s, expected %s"
                                        .formatted(doubleChestNixieReading, totalDoubleChestItems));
                });
    }

    @GameTest(template = "smart_observer_filtered_storage")
    public static void smartObserverFilteredStorage(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(2, 3, 1);
        BlockPos leftLamp = new BlockPos(3, 2, 3);
        BlockPos rightLamp = new BlockPos(1, 2, 3);
        helper.pullLever(lever);
        helper.succeedWhen(
                () -> {
                    helper.assertBlockProperty(leftLamp, RedstoneLampBlock.LIT, true);
                    helper.assertBlockProperty(rightLamp, RedstoneLampBlock.LIT, false);
                });
    }

    @GameTest(template = "smart_observer_storage")
    public static void smartObserverStorage(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(1, 3, 2);
        BlockPos lamp = new BlockPos(1, 2, 3);
        helper.pullLever(lever);
        helper.succeedWhen(() -> helper.assertBlockProperty(lamp, RedstoneLampBlock.LIT, true));
    }

    @GameTest(template = "depot_display", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void depotDisplay(CreateGameTestHelper helper) {
        BlockPos displayPos = new BlockPos(5, 3, 1);
        List<DepotBlockEntity> depots =
                Stream.of(new BlockPos(2, 2, 1), new BlockPos(1, 2, 1))
                        .map(pos -> helper.getBlockEntity(AllBlockEntityTypes.DEPOT.get(), pos))
                        .toList();
        List<BlockPos> levers = List.of(new BlockPos(2, 5, 0), new BlockPos(1, 5, 0));
        levers.forEach(helper::pullLever);
        FlapDisplayBlockEntity display =
                helper.getBlockEntity(AllBlockEntityTypes.FLAP_DISPLAY.get(), displayPos)
                        .getController();
        helper.succeedWhen(
                () -> {
                    for (int i = 0; i < 2; i++) {
                        FlapDisplayLayout line = display.getLines().get(i);
                        MutableComponent textComponent = Component.empty();
                        line.getSections().stream()
                                .map(FlapDisplaySection::getText)
                                .forEach(textComponent::append);
                        String text = textComponent.getString().toLowerCase(Locale.ROOT).trim();

                        DepotBlockEntity depot = depots.get(i);
                        ItemStack item = depot.getHeldItem();
                        String name = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();

                        if (!name.equals(text))
                            helper.fail("Text mismatch: wanted [" + name + "], got: " + text);
                    }
                });
    }

    @GameTest(template = "threshold_switch")
    public static void thresholdSwitch(CreateGameTestHelper helper) {
        BlockPos chest = new BlockPos(1, 2, 1);
        BlockPos lamp = new BlockPos(2, 3, 1);
        helper.assertBlockProperty(lamp, RedstoneLampBlock.LIT, false);
        Storage<ItemVariant> chestStorage = helper.itemStorageAt(chest);
        ItemStack diamondStack = new ItemStack(Items.DIAMOND, 64);
        for (int i = 0; i < 18; i++) { // insert 18 stacks
            TransferUtil.insert(chestStorage, diamondStack);
        }
        helper.succeedWhenWithDiagnostics(
                () -> helper.assertBlockProperty(lamp, RedstoneLampBlock.LIT, true),
                () -> helper.snapshot(chest, chest.east(), lamp));
    }

    @GameTest(template = "storages", timeoutTicks = CreateGameTestHelper.TEN_SECONDS)
    public static void storages(CreateGameTestHelper helper) {
        BlockPos lever = new BlockPos(12, 3, 2);
        BlockPos startChest = new BlockPos(13, 3, 1);
        Object2LongMap<Item> originalContent = helper.getItemContent(startChest);
        BlockPos endShulker = new BlockPos(1, 3, 1);
        helper.pullLever(lever);
        helper.succeedWhen(() -> helper.assertContentPresent(originalContent, endShulker));
    }

    @GameTest(template = "vault_comparator_output")
    public static void vaultComparatorOutput(CreateGameTestHelper helper) {
        BlockPos smallInput = new BlockPos(1, 4, 1);
        BlockPos smallNixie = new BlockPos(3, 2, 1);
        helper.assertNixiePower(smallNixie, 0);
        helper.whenSecondsPassed(1, () -> helper.spawnItems(smallInput, Items.BREAD, 64 * 9));

        BlockPos medInput = new BlockPos(1, 5, 4);
        BlockPos medNixie = new BlockPos(4, 2, 4);
        helper.assertNixiePower(medNixie, 0);
        helper.whenSecondsPassed(2, () -> helper.spawnItems(medInput, Items.BREAD, 64 * 77));

        BlockPos bigInput = new BlockPos(1, 6, 8);
        BlockPos bigNixie = new BlockPos(5, 2, 7);
        helper.assertNixiePower(bigNixie, 0);
        helper.whenSecondsPassed(3, () -> helper.spawnItems(bigInput, Items.BREAD, 64 * 240));

        helper.succeedWhenWithDiagnostics(
                () -> {
                    helper.assertNixiePower(smallNixie, 7);
                    helper.assertNixiePower(medNixie, 7);
                    helper.assertNixiePower(bigNixie, 7);
                },
                () ->
                        helper.snapshot(
                                new BlockPos(1, 2, 1),
                                new BlockPos(1, 3, 1),
                                smallNixie,
                                new BlockPos(1, 2, 3),
                                new BlockPos(1, 4, 4),
                                medNixie,
                                new BlockPos(1, 2, 6),
                                new BlockPos(1, 5, 8),
                                bigNixie));
    }

    @GameTest(template = "arm_purgatory")
    public static void depotInsertionTransactions(CreateGameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 1);
        helper.setBlock(pos, Blocks.AIR);
        helper.setBlock(pos, AllBlocks.DEPOT.get());
        DepotBlockEntity depot = helper.getBlockEntity(AllBlockEntityTypes.DEPOT.get(), pos);
        DirectBeltInputBehaviour input = helper.getBehavior(pos, DirectBeltInputBehaviour.TYPE);
        ItemStack stack = new ItemStack(Items.DIAMOND, 4);

        helper.assertTrue(
                input.handleInsertion(stack, Direction.NORTH, true).isEmpty(),
                "Standalone simulation rejected insertion");
        helper.assertTrue(depot.getHeldItem().isEmpty(), "Standalone simulation changed depot");

        try (Transaction outer = Transaction.openOuter()) {
            helper.assertTrue(
                    input.handleInsertion(stack, Direction.NORTH, true).isEmpty(),
                    "Nested simulation rejected insertion");
            helper.assertTrue(depot.getHeldItem().isEmpty(), "Nested simulation changed depot");
            outer.commit();
        }
        helper.assertTrue(depot.getHeldItem().isEmpty(), "Outer commit retained simulated items");

        try (Transaction outer = Transaction.openOuter()) {
            helper.assertTrue(
                    input.handleInsertion(stack, Direction.NORTH, false).isEmpty(),
                    "Nested insertion rejected items");
            helper.assertTrue(
                    ItemStack.matches(stack, depot.getHeldItem()),
                    "Nested insertion did not put items on depot");
        }
        helper.assertTrue(depot.getHeldItem().isEmpty(), "Outer abort retained inserted items");

        try (Transaction outer = Transaction.openOuter()) {
            input.handleInsertion(stack, Direction.NORTH, false);
            outer.commit();
        }
        helper.assertTrue(
                ItemStack.matches(stack, depot.getHeldItem()), "Outer commit lost inserted items");

        depot.setHeldItem(ItemStack.EMPTY);
        helper.assertTrue(
                input.handleInsertion(stack, Direction.NORTH, false).isEmpty(),
                "Standalone insertion rejected items");
        helper.assertTrue(
                ItemStack.matches(stack, depot.getHeldItem()),
                "Standalone insertion lost inserted items");
        helper.assertTrue(stack.getCount() == 4, "Insertion mutated caller's stack");
        helper.succeed();
    }

    @GameTest(template = "depot_comparator_output")
    public static void depotComparatorOutput(CreateGameTestHelper helper) {
        BlockPos swordNixie = new BlockPos(7, 2, 1);
        BlockPos diamondNixie = new BlockPos(5, 2, 1);
        BlockPos fullPearlNixie = new BlockPos(3, 2, 1);
        BlockPos halfPearlNixie = new BlockPos(1, 2, 1);

        helper.succeedWhen(
                () -> {
                    helper.assertNixiePower(swordNixie, 15);
                    helper.assertNixiePower(diamondNixie, 15);
                    helper.assertNixiePower(fullPearlNixie, 15);
                    helper.assertNixiePower(halfPearlNixie, 8);
                });
    }

    // fabric: THIRTY_SECONDS instead of upstream's TEN_SECONDS only for extra washing passes; each
    // pass and every other lamp must still finish within upstream's ten seconds (checked below)
    @GameTest(template = "fan_processing", timeoutTicks = CreateGameTestHelper.THIRTY_SECONDS)
    public static void fanProcessing(CreateGameTestHelper helper) {
        // why does the redstone explode
        BlockPos.betweenClosed(new BlockPos(2, 7, 3), new BlockPos(11, 7, 3))
                .forEach(pos -> helper.setBlock(pos, Blocks.REDSTONE_WIRE));
        helper.pullLever(1, 7, 3);
        List<BlockPos> lamps =
                List.of(
                        new BlockPos(1, 2, 1),
                        new BlockPos(5, 2, 1),
                        new BlockPos(7, 2, 1),
                        new BlockPos(9, 2, 1),
                        new BlockPos(11, 2, 1));
        // fabric: the washing lamp waits for flint from 16 gravel, but each gravel becomes flint
        // with p = 0.25, so in 0.75^16 = 1% of runs none does and upstream's test fails (tester
        // round 3: 1 of 44 runs). Made deterministic with more samples, never a looser check: when
        // a pass has washed all its gravel without any flint, the barrel gets 16 more gravel for
        // another pass through the same fan (at most 3 passes, so a false failure is 1e-6). Each
        // pass must finish within upstream's ten seconds, and so must the other four lamps.
        // TestPortRolls.fanWashingRollsFlintFromGravel checks the chances themselves.
        BlockPos washLamp = new BlockPos(5, 2, 1);
        BlockPos washInput = new BlockPos(5, 6, 3);
        List<BlockPos> washPath =
                List.of(
                        washInput,
                        new BlockPos(5, 5, 3),
                        new BlockPos(5, 3, 3),
                        new BlockPos(5, 2, 3));
        // where this column's gravel falls and is washed (column 3's unwashed gravel lies west)
        AABB washArea =
                new AABB(helper.absolutePos(new BlockPos(4, 2, 2)))
                        .minmax(new AABB(helper.absolutePos(new BlockPos(6, 6, 4))));
        int maxPasses = 3;
        long[] pass = {1, 0}; // pass number, tick it started
        helper.onEachTick(
                () -> {
                    if (helper.getBlockState(washLamp).getValue(RedstoneLampBlock.LIT)) return;
                    boolean washing =
                            !helper.getLevel()
                                    .getEntitiesOfClass(
                                            ItemEntity.class,
                                            washArea,
                                            e ->
                                                    e.getItem().is(Items.GRAVEL)
                                                            || e.getItem().is(Items.FLINT))
                                    .isEmpty();
                    for (BlockPos pos : washPath) washing |= helper.getTotalItems(pos) > 0;
                    if (washing) {
                        if (helper.getTick() - pass[1] > CreateGameTestHelper.TEN_SECONDS)
                            helper.fail(
                                    "washing pass " + pass[0] + " did not finish in ten seconds");
                        return;
                    }
                    if (pass[0] >= maxPasses) return;
                    pass[0]++;
                    pass[1] = helper.getTick();
                    try (Transaction t = Transaction.openOuter()) {
                        helper.itemStorageAt(washInput).insert(ItemVariant.of(Items.GRAVEL), 16, t);
                        t.commit();
                    }
                    Create.LOGGER.info(
                            "[test] fanProcessing: 16 gravel washed without flint, pass {}",
                            pass[0]);
                });
        helper.runAtTickTime(
                CreateGameTestHelper.TEN_SECONDS,
                () -> {
                    for (BlockPos lamp : lamps)
                        if (!lamp.equals(washLamp))
                            helper.assertBlockProperty(lamp, RedstoneLampBlock.LIT, true);
                });
        helper.succeedWhen(
                () -> {
                    for (BlockPos lamp : lamps) {
                        helper.assertBlockProperty(lamp, RedstoneLampBlock.LIT, true);
                    }
                });
    }
}
