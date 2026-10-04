package com.simibubi.create.infrastructure.gametest.tests;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.SequencedAssembly;
import com.simibubi.create.infrastructure.gametest.CreateGameTestHelper;
import com.simibubi.create.infrastructure.gametest.GameTestGroup;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Tester round 3: the chance outputs that two upstream tests rely on, made deterministic (a fixed
 * seed on the level's random right before rolling, in one tick).
 *
 * <p>TestProcessing.precisionMechanismCrafting was made deterministic in 18a4ad9a6b by also
 * accepting "all 16 sheets became mechanisms"; that version passes when junk never rolls at all
 * (mutant: rollResult always picks the first entry survived the whole suite). Upstream's intent,
 * "the sequenced assembly also yields junk from the result pool", is checked here instead.
 */
@GameTestGroup(path = "qa")
public class TestPortRolls {

    static final long SEED = 0x6010L;

    /**
     * The last deploying step of the precision mechanism (step 15 of 15), looked up the way the
     * deployer looks it up, rolls the sequenced assembly's result pool: about 120 of 150 weight is
     * a mechanism and the rest is junk from the pool, never anything else.
     */
    @GameTest(template = "flat_7x6x7")
    public static void sequencedAssemblyRollsJunkFromItsPool(CreateGameTestHelper helper) {
        var level = helper.getLevel();
        ResourceLocation id = Create.asResource("sequenced_assembly/precision_mechanism");
        SequencedAssemblyRecipe assembly =
                (SequencedAssemblyRecipe)
                        level.getRecipeManager()
                                .byKey(id)
                                .orElseThrow(() -> new AssertionError("recipe " + id + " missing"))
                                .value();
        Item mechanism = assembly.getResultItem(level.registryAccess()).getItem();
        Set<Item> pool = new HashSet<>();
        for (ProcessingOutput out : assembly.resultPool) pool.add(out.getStack().getItem());
        helper.assertTrue(pool.size() >= 5, "result pool " + pool);

        int steps = 15; // 3 deploying steps x 5 loops
        ItemStack lastStep = AllItems.INCOMPLETE_PRECISION_MECHANISM.asStack();
        lastStep.set(
                AllDataComponents.SEQUENCED_ASSEMBLY,
                new SequencedAssembly(id, steps - 1, (steps - 1f) / steps));

        int rolls = 400;
        Map<String, Integer> seen = new TreeMap<>();
        int mechanisms = 0, junk = 0;
        level.random.setSeed(SEED);
        for (int i = 0; i < rolls; i++) {
            RecipeHolder<DeployerApplicationRecipe> step =
                    SequencedAssemblyRecipe.getRecipe(
                                    level,
                                    lastStep,
                                    AllRecipeTypes.DEPLOYING.getType(),
                                    DeployerApplicationRecipe.class)
                            .orElseThrow(
                                    () -> new AssertionError("no deploying step for " + lastStep));
            List<ItemStack> out = step.value().rollResults(level.random);
            helper.assertTrue(out.size() == 1, "one result per roll, got " + out);
            Item item = out.get(0).getItem();
            seen.merge(item.toString(), 1, Integer::sum);
            if (!pool.contains(item)) helper.fail("rolled " + item + ", not in the pool " + pool);
            if (item == mechanism) mechanisms++;
            else junk++;
        }
        // expected 320 / 80 (p = 0.8); the bounds are > 6 standard deviations wide
        helper.assertTrue(
                mechanisms >= 270 && mechanisms <= 360 && junk >= 40,
                "400 rolls gave " + mechanisms + " mechanisms and " + junk + " junk: " + seen);
        helper.assertTrue(
                seen.size() >= 5, "junk comes from several pool entries, rolled only " + seen);
        Create.LOGGER.info(
                "[qa] precision mechanism: {} rolls -> {} mechanisms, {} junk {}",
                rolls,
                mechanisms,
                junk,
                seen);
        helper.succeed();
    }

    /**
     * Fan washing (the splashing type the fan uses in the world) turns gravel into flint 25% and
     * iron nuggets 12.5% of the time, per gravel, and nothing else; TestItems.fanProcessing waits
     * for one flint out of 16 gravel.
     */
    @GameTest(template = "flat_7x6x7")
    public static void fanWashingRollsFlintFromGravel(CreateGameTestHelper helper) {
        var level = helper.getLevel();
        level.random.setSeed(SEED);
        Map<String, Integer> seen = new TreeMap<>();
        int gravel = 0;
        for (int i = 0; i < 5; i++) {
            ItemStack stack = new ItemStack(Items.GRAVEL, 64);
            helper.assertTrue(
                    AllFanProcessingTypes.SPLASHING.canProcess(stack, level),
                    "washing accepts gravel");
            List<ItemStack> out = AllFanProcessingTypes.SPLASHING.process(stack, level);
            helper.assertTrue(out != null, "washing processed gravel");
            gravel += 64;
            for (ItemStack s : out) seen.merge(s.getItem().toString(), s.getCount(), Integer::sum);
        }
        int flint = seen.getOrDefault(Items.FLINT.toString(), 0);
        int nuggets = seen.getOrDefault(Items.IRON_NUGGET.toString(), 0);
        helper.assertTrue(
                seen.keySet().stream()
                        .allMatch(
                                k ->
                                        k.equals(Items.FLINT.toString())
                                                || k.equals(Items.IRON_NUGGET.toString())),
                "washing gravel gave only flint and iron nuggets: " + seen);
        // expected 80 flint, 40 nuggets from 320 gravel
        helper.assertTrue(
                flint >= 45 && flint <= 120 && nuggets >= 15 && nuggets <= 70,
                gravel + " gravel washed into " + seen);
        Create.LOGGER.info("[qa] fan washing: {} gravel -> {}", gravel, seen);
        helper.succeed();
    }
}
