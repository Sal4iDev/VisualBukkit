package us.donut.visualbukkit.blocks.expressions;

import us.donut.visualbukkit.blocks.ExpressionBlock;
import us.donut.visualbukkit.blocks.annotations.Description;
import us.donut.visualbukkit.blocks.annotations.Name;
import us.donut.visualbukkit.blocks.syntax.SyntaxNode;

@Name("Conditional Expression")
@Description({"Returns a value based on a condition", "Returns: object"})
public class ExprTernary extends ExpressionBlock<Object> {

    @Override
    protected SyntaxNode init() {
        return new SyntaxNode(Object.class, "if", boolean.class, "else", Object.class);
    }

    @Override
    public String toJava() {
        return "(" + arg(1) + "?" + arg(0) + ":" + arg(2) + ")";
    }
}
