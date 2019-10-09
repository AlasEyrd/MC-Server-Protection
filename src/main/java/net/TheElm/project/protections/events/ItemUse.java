/*
 * This software is licensed under the MIT License
 * https://github.com/GStefanowich/MC-Server-Protection
 *
 * Copyright (c) 2019 Gregory Stefanowich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.TheElm.project.protections.events;

import net.TheElm.project.interfaces.ItemUseCallback;
import net.TheElm.project.utilities.BlockUtils;
import net.TheElm.project.utilities.ChunkUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.StairShape;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class ItemUse {
    
    private ItemUse() {}
    
    /**
     * Initialize our callback listener for Item Usage
     */
    public static void init() {
        ItemUseCallback.EVENT.register(ItemUse::blockInteract);
    }
    
    private static ActionResult blockInteract(ServerPlayerEntity player, World world, Hand hand, ItemStack itemStack) {
        // If item is stick and player has build permission
        if (itemStack.getItem() == Items.STICK) {
            BlockHitResult blockHitResult = BlockUtils.getLookingBlock( world, player );
            BlockPos blockPos = blockHitResult.getBlockPos();
            
            if ((blockHitResult.getType() == HitResult.Type.MISS) || (!ChunkUtils.canPlayerBreakInChunk( player, blockPos )))
                return ActionResult.PASS;
            
            BlockState blockState = world.getBlockState( blockPos );
            Block block = blockState.getBlock();
            
            if ((!player.isSneaking()) && blockState.contains(HorizontalFacingBlock.FACING)) {
                /*
                 * Rotate blocks
                 */
                Direction rotation = blockState.get(HorizontalFacingBlock.FACING).rotateYClockwise();
                world.setBlockState(blockPos, blockState.with(HorizontalFacingBlock.FACING, rotation));
                
                return ActionResult.SUCCESS;
            } else if (player.isSneaking() && blockState.contains(PillarBlock.AXIS)) {
                /*
                 * Change block axis
                 */
                world.setBlockState(blockPos, ((PillarBlock) block).rotate( blockState, BlockRotation.CLOCKWISE_90 ));
                
                return ActionResult.SUCCESS;
            } else if (block instanceof StairsBlock) {
                /* 
                 * If block is a stairs block
                 */
                StairShape shape = blockState.get(StairsBlock.SHAPE);
                
                world.setBlockState( blockPos, blockState.with(StairsBlock.SHAPE, rotateStairShape(shape)));
                
                return ActionResult.SUCCESS;
            } else if (block instanceof DoorBlock) {
                /* 
                 * Switch door hinges
                 */
                DoubleBlockHalf doorHalf = blockState.get(DoorBlock.HALF);
                
                BlockPos otherHalf = blockPos.offset(doorHalf == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
                
                // Change the doors hinge
                DoorHinge hinge = blockState.get(DoorBlock.HINGE) == DoorHinge.LEFT ? DoorHinge.RIGHT : DoorHinge.LEFT;
                
                world.setBlockState( blockPos, blockState.with(DoorBlock.HINGE, hinge));
                world.setBlockState( otherHalf, world.getBlockState(otherHalf).with(DoorBlock.HINGE, hinge));
                
                return ActionResult.SUCCESS;
            } else if (player.isSneaking() && blockState.contains(HorizontalFacingBlock.FACING)) {
                /*
                 * Catch for block rotating
                 */
    
                Direction rotation = blockState.get(HorizontalFacingBlock.FACING).rotateYClockwise();
                world.setBlockState(blockPos, blockState.with(HorizontalFacingBlock.FACING, rotation));
                
                return ActionResult.SUCCESS;
            }
        }
        
        return ActionResult.PASS;
    }
    private static StairShape rotateStairShape(StairShape shape) {
        switch (shape) {
            case STRAIGHT:
                return StairShape.INNER_LEFT;
            case INNER_LEFT:
                return StairShape.INNER_RIGHT;
            case INNER_RIGHT:
                return StairShape.OUTER_LEFT;
            case OUTER_LEFT:
                return StairShape.OUTER_RIGHT;
            default:
                return StairShape.STRAIGHT;
        }
    }
}
