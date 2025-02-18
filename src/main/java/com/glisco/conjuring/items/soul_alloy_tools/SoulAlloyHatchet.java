package com.glisco.conjuring.items.soul_alloy_tools;

import com.glisco.conjuring.Conjuring;
import com.glisco.conjuring.entities.SoulFellerEntity;
import com.glisco.conjuring.items.ConjuringItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SoulAlloyHatchet extends AxeItem implements SoulAlloyTool {

    public SoulAlloyHatchet() {
        super(SoulAlloyToolMaterial.INSTANCE, 5.0f, -3.0f, new Settings().group(Conjuring.CONJURING_GROUP).rarity(Rarity.UNCOMMON));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (SoulAlloyTool.isSecondaryEnabled(context.getStack())) {
            return ActionResult.PASS;
        } else {
            return super.useOnBlock(context);
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!SoulAlloyTool.isSecondaryEnabled(user.getStackInHand(hand))) return TypedActionResult.pass(user.getStackInHand(hand));

        if (!world.isClient()) {
            SoulFellerEntity feller = new SoulFellerEntity(world, user);
            feller.refreshPositionAndAngles(user.getX(), user.getEyeY(), user.getZ(), 0, 0);
            feller.setVelocity(user, user.getPitch(), user.getYaw(), 0f, 1.5f, 1);

            feller.setItem(user.getStackInHand(hand));

            int scopeGems = SoulAlloyTool.getModifierLevel(user.getStackInHand(hand), SoulAlloyModifier.SCOPE);
            if (scopeGems > 0) {
                feller.setMaxBlocks((int) (8 + Math.pow(scopeGems, Conjuring.CONFIG.tools_config.axe_scope_exponent) * 8));
            }

            world.spawnEntity(feller);

            user.getItemCooldownManager().set(ConjuringItems.SOUL_ALLOY_HATCHET, Conjuring.CONFIG.tools_config.axe_secondary_cooldown);
            user.getStackInHand(hand).damage(Conjuring.CONFIG.tools_config.axe_secondary_base_durability_cost + Conjuring.CONFIG.tools_config.axe_secondary_per_scope_durability_cost * scopeGems, user, player -> player.sendToolBreakStatus(hand));

        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.addAll(SoulAlloyTool.getTooltip(stack));
    }

}
