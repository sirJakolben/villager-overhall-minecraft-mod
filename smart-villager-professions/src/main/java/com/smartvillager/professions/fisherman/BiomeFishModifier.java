package com.smartvillager.professions.fisherman;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.smartvillager.professions.SmartVillagerProfessionsMod;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;

/**
 * Lets the player catch the biome fish too (Obsidian Fisherman.md, 2026-09-30): where the rod's catch is a fish (item tag
 * minecraft:fishes) and the bobber floats in a biome group with its own fish (BiomeFish), there is a chance that this fish takes the
 * catch's place - never on top of it, so the catch count stays Vanilla's and junk and treasure stay untouched. Bound to
 * minecraft:gameplay/fishing by its conditions (data/smartvillager_professions/loot_modifiers/biome_fish.json); the chance is the file's "chance".
 */
public class BiomeFishModifier extends LootModifier {

    public static final MapCodec<BiomeFishModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(Codec.doubleRange(0.0, 1.0).fieldOf("chance").forGetter(modifier -> modifier.chance))
            .apply(instance, BiomeFishModifier::new));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, SmartVillagerProfessionsMod.MODID);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<BiomeFishModifier>> BIOME_FISH =
            LOOT_MODIFIERS.register("biome_fish", () -> CODEC);

    private final double chance;

    public BiomeFishModifier(LootItemCondition[] conditions, double chance) {
        super(conditions);
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Vec3 origin = context.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin == null) {
            return generatedLoot;
        }
        Optional<BiomeFish> fish = BiomeFish.at(context.getLevel(), BlockPos.containing(origin));
        if (fish.isEmpty() || context.getRandom().nextDouble() >= chance) {
            return generatedLoot;
        }
        for (int i = 0; i < generatedLoot.size(); i++) {
            if (generatedLoot.get(i).is(ItemTags.FISHES)) {
                generatedLoot.set(i, new ItemStack(fish.get().item()));
                break;
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
