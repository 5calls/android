package org.a5calls.android.a5calls.test;

import android.os.Bundle;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Ordering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomOrdering extends Ordering {
    private static final String TAG = "RandomOrdering";
    private static final String SEED_KEY = "testSeed";

    private long seed = 0;

    public RandomOrdering() {
        // Look for a custom seed passed via Android Instrumentation arguments
        Bundle arguments = InstrumentationRegistry.getArguments();
        if (arguments.containsKey(SEED_KEY)) {
            String seedStr = arguments.getString(SEED_KEY);
            if (seedStr != null) {
                this.seed = Long.parseLong(seedStr);
            Log.w(TAG, "🎲 USING EXPLICIT SEED FOR REPRODUCIBILITY: " + this.seed);
            }
        }
        if (this.seed == 0){
            // No seed provided, dynamically generate one
            this.seed = new Random().nextLong();
            Log.w(TAG, "🎲 NO SEED PROVIDED. GENERATED NEW RANDOM SEED: " + this.seed);
            Log.w(TAG, "👉 To reproduce this exact order, run with: -Pandroid.testInstrumentationRunnerArguments.testSeed=" + this.seed);
        }
    }

    @Override
    protected List<Description> orderItems(Collection<Description> descriptions) {
        // Initialize the Random generator using our exact seed
        Random seededRandom = new Random(this.seed);
        List<Description> shuffledList = new ArrayList<>(descriptions);
        // Randomly shuffles the test methods before execution
        Collections.shuffle(shuffledList, seededRandom);
        return shuffledList;
    }

    public static class Factory implements Ordering.Factory {
        @Override
        public Ordering create(Context context) {
            return new RandomOrdering();
        }
    }
}
