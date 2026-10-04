package com.simibubi.create.infrastructure.gametest;

import com.simibubi.create.infrastructure.gametest.tests.TestContraptions;
import com.simibubi.create.infrastructure.gametest.tests.TestFluids;
import com.simibubi.create.infrastructure.gametest.tests.TestItems;
import com.simibubi.create.infrastructure.gametest.tests.TestMisc;
import com.simibubi.create.infrastructure.gametest.tests.TestPortBugLoot;
import com.simibubi.create.infrastructure.gametest.tests.TestPortBugPackager;
import com.simibubi.create.infrastructure.gametest.tests.TestPortBugSounds;
import com.simibubi.create.infrastructure.gametest.tests.TestPortContraptions;
import com.simibubi.create.infrastructure.gametest.tests.TestPortData;
import com.simibubi.create.infrastructure.gametest.tests.TestPortFluids;
import com.simibubi.create.infrastructure.gametest.tests.TestPortLogistics;
import com.simibubi.create.infrastructure.gametest.tests.TestPortPersistence;
import com.simibubi.create.infrastructure.gametest.tests.TestPortProcessing;
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
        TestPortBugLoot.class,
        TestPortBugPackager.class,
        TestPortBugSounds.class,
        TestPortContraptions.class,
        TestPortData.class,
        TestPortFluids.class,
        TestPortLogistics.class,
        TestPortPersistence.class,
        TestPortProcessing.class,
        TestProcessing.class,
        TestRegressions.class
    };

    @GameTestGenerator
    public static Collection<TestFunction> generateTests() {
        return CreateTestFunction.getTestsFrom(testHolders);
    }
}
