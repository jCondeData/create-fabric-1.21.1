package com.simibubi.create.infrastructure.gametest.tests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data the port generates or converts: every recipe file must load, the recipe fixes from the
 * upstream 6.0.3-6.0.10 changelog must hold, every block must drop what its loot table says, tags
 * must contain what upstream puts in them, and the /create server commands must work and respect
 * permissions.
 */
@GameTestGroup(path = "qa")
public class TestPortData {

    static RecipeManager recipes(CreateGameTestHelper helper) {
        return helper.getLevel().getRecipeManager();
    }

    static Item item(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item == Items.AIR) throw new IllegalStateException("no item " + id);
        return item;
    }

    /**
     * Every recipe JSON Create ships must be in the RecipeManager unless its own Fabric load
     * conditions say otherwise. Minecraft only logs a line when a recipe fails to parse, so a
     * broken conversion (fluid amounts, ingredients, conditions) would otherwise go unnoticed.
     */
    @GameTest(template = "flat_7x6x7")
    public static void everyCreateRecipeFileLoads(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    MinecraftServer server = helper.getLevel().getServer();
                    var registries = server.registryAccess();
                    Map<ResourceLocation, Resource> files =
                            server.getResourceManager()
                                    .listResources("recipe", rl -> rl.getPath().endsWith(".json"));
                    int expected = 0, conditionedOut = 0;
                    List<String> missing = new ArrayList<>();
                    for (var entry : files.entrySet()) {
                        ResourceLocation file = entry.getKey();
                        if (!file.getNamespace().equals(Create.ID)) continue;
                        String path = file.getPath();
                        ResourceLocation id =
                                ResourceLocation.fromNamespaceAndPath(
                                        file.getNamespace(),
                                        path.substring(
                                                "recipe/".length(),
                                                path.length() - ".json".length()));
                        JsonObject json;
                        try (Reader reader = entry.getValue().openAsReader()) {
                            json = JsonParser.parseReader(reader).getAsJsonObject();
                        } catch (Exception e) {
                            missing.add(id + " (unreadable: " + e + ")");
                            continue;
                        }
                        if (json.has("neoforge:conditions"))
                            missing.add(id + " (still has neoforge:conditions, ignored on Fabric)");
                        boolean load = true;
                        JsonElement conditions = json.get(ResourceConditions.CONDITIONS_KEY);
                        if (conditions != null) {
                            var parsed =
                                    ResourceCondition.LIST_CODEC.parse(
                                            JsonOps.INSTANCE, conditions);
                            if (parsed.isError()) {
                                missing.add(id + " (bad load conditions: " + conditions + ")");
                                continue;
                            }
                            for (ResourceCondition c : parsed.getOrThrow())
                                load &= c.test(registries);
                        }
                        if (!load) {
                            conditionedOut++;
                            continue;
                        }
                        expected++;
                        if (recipes(helper).byKey(id).isEmpty()) missing.add(id.toString());
                    }
                    if (!missing.isEmpty())
                        helper.fail(
                                missing.size()
                                        + " of "
                                        + expected
                                        + " Create recipes did not load: "
                                        + String.join(
                                                ", ",
                                                missing.subList(0, Math.min(10, missing.size()))));
                    helper.assertTrue(expected > 1500, "only " + expected + " recipe files found");
                    Create.LOGGER.info(
                            "[qa] {} Create recipe files loaded, {} skipped by their load"
                                + " conditions",
                            expected,
                            conditionedOut);
                    helper.succeed();
                });
    }

    /** 6.0.10 #9510: waxed copper shingles/tiles stonecut into waxed variants only. */
    @GameTest(template = "flat_7x6x7")
    public static void waxedCopperStonecuttingStaysWaxed(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    for (String base : new String[] {"copper_shingles", "copper_tiles"}) {
                        for (String prefix :
                                new String[] {"", "waxed_", "exposed_", "waxed_weathered_"}) {
                            Item input = item("create:" + prefix + base);
                            List<RecipeHolder<?>> found = new ArrayList<>();
                            found.addAll(
                                    recipes(helper)
                                            .getRecipesFor(
                                                    RecipeType.STONECUTTING,
                                                    new SingleRecipeInput(new ItemStack(input)),
                                                    helper.getLevel()));
                            helper.assertTrue(
                                    found.size() >= 2,
                                    input + " has " + found.size() + " stonecutting recipes");
                            boolean waxed = prefix.startsWith("waxed_");
                            for (RecipeHolder<?> r : found) {
                                ItemStack out =
                                        r.value().getResultItem(helper.getLevel().registryAccess());
                                String outId =
                                        BuiltInRegistries.ITEM.getKey(out.getItem()).getPath();
                                helper.assertTrue(
                                        outId.startsWith("waxed_") == waxed,
                                        input + " stonecuts into " + outId + " via " + r.id());
                                if (!prefix.isEmpty())
                                    helper.assertTrue(
                                            outId.contains(prefix.replace("waxed_", "")),
                                            input
                                                    + " stonecuts into "
                                                    + outId
                                                    + " (wrong oxidation)");
                            }
                        }
                    }
                    helper.succeed();
                });
    }

    /**
     * Milling: clay gives 4 clay balls every time (6.0.9), pink petals, torchflower and pitcher
     * plant give their dyes (6.0.7 #8914). Rolled many times with a fixed seed.
     */
    @GameTest(template = "flat_7x6x7")
    public static void millingRecipesFromChangelog(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    RandomSource random = RandomSource.create(42);
                    Object[][] cases = {
                        {Items.CLAY, Items.CLAY_BALL, 4},
                        {Items.PINK_PETALS, Items.PINK_DYE, 1},
                        {Items.TORCHFLOWER, Items.ORANGE_DYE, 1},
                        {Items.PITCHER_PLANT, Items.CYAN_DYE, 1},
                    };
                    for (Object[] c : cases) {
                        Item input = (Item) c[0];
                        Item output = (Item) c[1];
                        int min = (Integer) c[2];
                        var holder =
                                recipes(helper)
                                        .getRecipeFor(
                                                AllRecipeTypes.MILLING.getType(),
                                                new SingleRecipeInput(new ItemStack(input)),
                                                helper.getLevel());
                        helper.assertTrue(holder.isPresent(), "no milling recipe for " + input);
                        ProcessingRecipe<?, ?> recipe =
                                (ProcessingRecipe<?, ?>) holder.get().value();
                        for (int i = 0; i < 50; i++) {
                            int got =
                                    recipe.rollResults(random).stream()
                                            .filter(s -> s.is(output))
                                            .mapToInt(ItemStack::getCount)
                                            .sum();
                            helper.assertTrue(
                                    got >= min,
                                    input
                                            + " milled into "
                                            + got
                                            + " "
                                            + output
                                            + " (roll "
                                            + i
                                            + ")");
                            if (input == Items.CLAY)
                                helper.assertTrue(got == 4, "clay gave " + got + " clay balls");
                        }
                    }
                    helper.succeed();
                });
    }

    /** 6.0.9 #9501: bound cardboard accepts anything in #c:strings, e.g. plain string. */
    @GameTest(template = "flat_7x6x7")
    public static void boundCardboardCraftsWithStringTag(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    TagKey<Item> strings =
                            TagKey.create(Registries.ITEM, ResourceLocation.parse("c:strings"));
                    helper.assertTrue(
                            new ItemStack(Items.STRING).is(strings), "string not in #c:strings");
                    CraftingInput input =
                            CraftingInput.of(
                                    2,
                                    1,
                                    List.of(
                                            new ItemStack(AllBlocks.CARDBOARD_BLOCK.get()),
                                            new ItemStack(Items.STRING)));
                    var result =
                            recipes(helper)
                                    .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
                    helper.assertTrue(
                            result.isPresent(), "cardboard block + string does not craft");
                    ItemStack out =
                            result.get()
                                    .value()
                                    .assemble(input, helper.getLevel().registryAccess());
                    helper.assertTrue(
                            out.is(AllBlocks.BOUND_CARDBOARD_BLOCK.get().asItem()),
                            "crafted " + out);
                    helper.succeed();
                });
    }

    /**
     * Diving armor is not trimmable (upstream removes it from #minecraft:trimmable_armor; the port
     * does it with SmithingTrimRecipeMixin), while vanilla armor still is.
     */
    @GameTest(template = "flat_7x6x7")
    public static void divingArmorUntrimmableVanillaArmorTrimmable(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    ItemStack template = new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
                    ItemStack material = new ItemStack(Items.GOLD_INGOT);
                    for (Item base :
                            List.of(
                                    AllItems.COPPER_DIVING_HELMET.get(),
                                    AllItems.NETHERITE_BACKTANK.get().asItem(),
                                    AllItems.COPPER_DIVING_BOOTS.get())) {
                        var r =
                                recipes(helper)
                                        .getRecipeFor(
                                                RecipeType.SMITHING,
                                                new SmithingRecipeInput(
                                                        template, new ItemStack(base), material),
                                                helper.getLevel());
                        helper.assertTrue(
                                r.isEmpty(),
                                base + " can be trimmed via " + r.map(RecipeHolder::id));
                    }
                    var vanilla =
                            recipes(helper)
                                    .getRecipeFor(
                                            RecipeType.SMITHING,
                                            new SmithingRecipeInput(
                                                    template,
                                                    new ItemStack(Items.IRON_HELMET),
                                                    material),
                                            helper.getLevel());
                    helper.assertTrue(vanilla.isPresent(), "iron helmet can no longer be trimmed");
                    helper.succeed();
                });
    }

    /** Tag fixes from the 6.0.3-6.0.10 changelog. */
    @GameTest(template = "flat_7x6x7")
    public static void tagsCarryUpstreamFixes(CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    // 6.0.10 #9831: dough tagged correctly
                    TagKey<Item> dough =
                            TagKey.create(Registries.ITEM, ResourceLocation.parse("c:foods/dough"));
                    helper.assertTrue(
                            new ItemStack(AllItems.DOUGH.get()).is(dough),
                            "create:dough not in #c:foods/dough");
                    // 6.0.10 #9907: seats tags
                    TagKey<Item> seatItems =
                            TagKey.create(Registries.ITEM, Create.asResource("seats"));
                    TagKey<Block> seatBlocks =
                            TagKey.create(Registries.BLOCK, Create.asResource("seats"));
                    helper.assertTrue(
                            new ItemStack(
                                            AllBlocks.SEATS
                                                    .get(net.minecraft.world.item.DyeColor.RED)
                                                    .get())
                                    .is(seatItems),
                            "red seat not in #create:seats (item)");
                    helper.assertTrue(
                            AllBlocks.SEATS
                                    .get(net.minecraft.world.item.DyeColor.RED)
                                    .getDefaultState()
                                    .is(seatBlocks),
                            "red seat not in #create:seats (block)");
                    helper.assertTrue(
                            !Blocks.OAK_STAIRS.defaultBlockState().is(seatBlocks),
                            "stairs are seats");
                    // 6.0.6 #8407: contraptions are not teleportable
                    TagKey<EntityType<?>> noTeleport =
                            TagKey.create(
                                    Registries.ENTITY_TYPE,
                                    ResourceLocation.parse("c:teleporting_not_supported"));
                    helper.assertTrue(
                            AllEntityTypes.ORIENTED_CONTRAPTION.get().is(noTeleport)
                                    && AllEntityTypes.CARRIAGE_CONTRAPTION.get().is(noTeleport),
                            "contraption entities missing from #c:teleporting_not_supported");
                    helper.assertTrue(!EntityType.PIG.is(noTeleport), "pigs not teleportable?");
                    // 6.0.6 #8589: cardboard has the plates tag
                    TagKey<Item> plates =
                            TagKey.create(Registries.ITEM, ResourceLocation.parse("c:plates"));
                    helper.assertTrue(
                            new ItemStack(AllItems.CARDBOARD.get()).is(plates),
                            "cardboard not in #c:plates");
                    // 6.0.10 #7162: plough blacklist / whitelist tags exist and are usable
                    TagKey<Block> ploughBlacklist =
                            TagKey.create(Registries.BLOCK, Create.asResource("plough_blacklist"));
                    TagKey<Block> ploughWhitelist =
                            TagKey.create(Registries.BLOCK, Create.asResource("plough_whitelist"));
                    helper.assertTrue(
                            BuiltInRegistries.BLOCK.getTag(ploughBlacklist).isPresent()
                                    && BuiltInRegistries.BLOCK.getTag(ploughWhitelist).isPresent(),
                            "plough tags not loaded");
                    // honey / chocolate are fluids Create owns, with the c: tags recipes use
                    TagKey<net.minecraft.world.level.material.Fluid> honey =
                            TagKey.create(Registries.FLUID, ResourceLocation.parse("c:honey"));
                    helper.assertTrue(
                            com.simibubi.create.AllFluids.HONEY
                                    .get()
                                    .getSource()
                                    .defaultFluidState()
                                    .is(honey),
                            "create:honey not in #c:honey");
                    helper.succeed();
                });
    }

    /**
     * /create clone (server command, op level 2) copies blocks with their block entity data; a
     * non-op player is refused.
     */
    @GameTest(template = "flat_7x6x7")
    public static void createCloneCommandCopiesBlockEntitiesAndNeedsOp(
            CreateGameTestHelper helper) {
        helper.runAfterDelay(
                1,
                () -> { // qa-wrap: assertion messages survive
                    BlockPos tank = new BlockPos(1, 1, 1);
                    BlockPos copy = new BlockPos(4, 1, 1);
                    helper.setBlock(tank, AllBlocks.FLUID_TANK.getDefaultState());
                    try (Transaction t = Transaction.openOuter()) {
                        helper.fluidStorageAt(tank)
                                .insert(FluidVariant.of(Fluids.LAVA), 3 * FluidConstants.BUCKET, t);
                        t.commit();
                    }
                    MinecraftServer server = helper.getLevel().getServer();
                    BlockPos a = helper.absolutePos(tank);
                    BlockPos d = helper.absolutePos(copy);
                    String cmd =
                            "create clone %d %d %d %d %d %d %d %d %d"
                                    .formatted(
                                            a.getX(), a.getY(), a.getZ(), a.getX(), a.getY(),
                                            a.getZ(), d.getX(), d.getY(), d.getZ());

                    ServerPlayer player = helper.makeMockServerPlayerInLevel();
                    CommandSourceStack playerSource =
                            player.createCommandSourceStack().withPermission(0);
                    boolean refused;
                    try {
                        server.getCommands().getDispatcher().execute(cmd, playerSource);
                        refused = false;
                    } catch (CommandSyntaxException e) {
                        refused = true;
                    }
                    helper.assertTrue(refused, "non-op player could run /" + cmd);
                    helper.assertBlockNotPresent(AllBlocks.FLUID_TANK.get(), copy);

                    CommandSourceStack op =
                            server.createCommandSourceStack()
                                    .withPermission(2)
                                    .withLevel(helper.getLevel())
                                    .withSuppressedOutput();
                    int result;
                    try {
                        result = server.getCommands().getDispatcher().execute(cmd, op);
                    } catch (CommandSyntaxException e) {
                        helper.fail("/" + cmd + " failed: " + e.getMessage());
                        return;
                    }
                    helper.assertTrue(result > 0, "/create clone returned " + result);
                    helper.assertBlockPresent(AllBlocks.FLUID_TANK.get(), copy);
                    long lava = helper.getTankContents(copy).getAmount();
                    helper.assertTrue(
                            lava == 3 * FluidConstants.BUCKET,
                            "cloned tank holds " + lava + " lava droplets");
                    helper.succeed();
                });
    }
}
