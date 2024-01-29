package com.gmail.visualbukkit.blocks.definitions.core;

import com.gmail.visualbukkit.blocks.BlockDefinition;
import com.gmail.visualbukkit.blocks.ExpressionBlock;
import com.gmail.visualbukkit.project.BuildInfo;
import com.gmail.visualbukkit.reflection.ClassInfo;

@BlockDefinition(id = "expr-loop-number", name = "Loop Number", description = "The current loop number")
public class ExprLoopNumber extends ExpressionBlock {

    @Override
    public ClassInfo getReturnType() {
        return ClassInfo.of(int.class);
    }

    @Override
    public void updateState() {
        super.updateState();
        checkForContainer(StatNumberLoop.class, StatAdvancedNumberLoop.class);
    }

    @Override
    public String generateJava(BuildInfo buildInfo) {
        return "$FINAL_loopNumber" + getNestedContainers(StatNumberLoop.class, StatAdvancedNumberLoop.class);
    }
}
