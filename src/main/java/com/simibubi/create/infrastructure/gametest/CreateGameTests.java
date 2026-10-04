package com.simibubi.create.infrastructure.gametest;

import com.simibubi.create.infrastructure.gametest.tests.TestContraptions;
import com.simibubi.create.infrastructure.gametest.tests.TestFluids;
import com.simibubi.create.infrastructure.gametest.tests.TestItems;
import com.simibubi.create.infrastructure.gametest.tests.TestMisc;
import com.simibubi.create.infrastructure.gametest.tests.TestProcessing;
import com.simibubi.create.infrastructure.gametest.tests.TestRegressions;

import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;

import java.util.Collection;

public class CreateGameTests {
    private static final Class<?>[] testHolders = {
        TestContraptions.class,
        TestFluids.class,
        TestItems.class,
        TestMisc.class,
        TestProcessing.class,
        TestRegressions.class
    };

    @GameTestGenerator
    public static Collection<TestFunction> generateTests() {
        return CreateTestFunction.getTestsFrom(testHolders);
    }
}
