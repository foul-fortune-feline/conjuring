package com.glisco.conjuring.items;

import com.glisco.conjuring.Conjuring;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.List;

public class ConjuringFocus extends Item {

    private final boolean hasGlint;

    public ConjuringFocus(boolean hasGlint) {
        super(new Settings().group(Conjuring.CONJURING_GROUP).maxCount(1).rarity(Rarity.UNCOMMON));
        this.hasGlint = hasGlint;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        if (!stack.getOrCreateNbt().contains("Entity")) {
            return;
        }

        String entityName = "entity." + stack.getNbt().getCompound("Entity").getString("id").replace(':', '.');
        tooltip.add(MutableText.of(new TranslatableTextContent(entityName)).formatted(Formatting.GRAY));
    }

    public static ItemStack writeData(ItemStack focus, EntityType<?> entityType) {
        NbtCompound stackTag = focus.getOrCreateNbt();

        NbtCompound entityTag = new NbtCompound();
        entityTag.putString("id", Registry.ENTITY_TYPE.getId(entityType).toString());

        stackTag.put("Entity", entityTag);
        focus.setNbt(stackTag);
        return focus;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return hasGlint;
    }
}
