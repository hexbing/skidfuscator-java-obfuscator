package dev.skidfuscator.obfuscator.predicate.renderer.impl;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.predicate.renderer.IntegerBlockPredicateRenderer;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidBlock;
import dev.skidfuscator.obfuscator.skidasm.cfg.SkidControlFlowGraph;
import dev.skidfuscator.obfuscator.skidasm.stmt.SkidCopyVarStmt;
import org.mapleir.flowgraph.edges.UnconditionalJumpEdge;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.code.expr.ConstantExpr;
import org.mapleir.ir.code.expr.VarExpr;
import org.mapleir.ir.code.stmt.UnconditionalJumpStmt;
import org.mapleir.ir.locals.Local;
import org.objectweb.asm.Type;

public class UnconditionalJumpRenderer extends AbstractInstructionRenderer<UnconditionalJumpStmt> {
    @Override
    public void transform(Skidfuscator base, UnconditionalJumpStmt stmt) {
        final SkidBlock block = (SkidBlock) stmt.getBlock();
        final SkidControlFlowGraph cfg = (SkidControlFlowGraph) block.getGraph();
        final SkidMethodNode methodNode = (SkidMethodNode) cfg.getMethodNode();

        final int index = block.indexOf(stmt);

        final SkidBlock seededBlock = (SkidBlock) block;
        final BasicBlock target = stmt.getTarget();
        final SkidBlock targetSeeded = (SkidBlock) target;

        // Add jump and seed
        final SkidBlock basicBlock = new SkidBlock(block.cfg);
        basicBlock.setFlag(SkidBlock.FLAG_PROXY, true);

        methodNode.getFlowPredicate()
                .set(basicBlock, methodNode.getBlockPredicate(targetSeeded));

        this.addSeedLoader(
                basicBlock,
                targetSeeded,
                0,
                methodNode.getFlowPredicate(),
                methodNode.getBlockPredicate(seededBlock),
                "Unconditional"
        );

        final UnconditionalJumpEdge<BasicBlock> edge = new UnconditionalJumpEdge<>(basicBlock, target);
        final UnconditionalJumpStmt proxy = new UnconditionalJumpStmt(target, edge);
        proxy.setFlag(SkidBlock.FLAG_PROXY, true);

        basicBlock.add(proxy);

        // Add edge
        basicBlock.cfg.addVertex(basicBlock);
        basicBlock.cfg.addEdge(edge);

        // Replace successor
        stmt.setTarget(basicBlock);
        basicBlock.cfg.addEdge(new UnconditionalJumpEdge<>(block, basicBlock));

        if (IntegerBlockPredicateRenderer.DEBUG) {
            final Local local1 = block.cfg.getLocals().get(block.cfg.getLocals().getMaxLocals() + 2);
            block.add(index, new SkidCopyVarStmt(
                            new VarExpr(local1, Type.getType(String.class)),
                            new ConstantExpr(
                                    block.getDisplayName()
                                            + " -> " + target.getDisplayName()
                                            + " : c-loc - uncond : "
                                            + methodNode.getBlockPredicate(targetSeeded)
                            )
                    )
            );
        }
    }
}
