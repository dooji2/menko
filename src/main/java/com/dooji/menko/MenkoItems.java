package com.dooji.menko;

import com.dooji.menko.item.MenkoCardItem;
import com.dooji.menko.item.MenkoLeaveItem;
import com.dooji.menko.item.MenkoStartItem;
import com.dooji.menko.item.NonFunctionalMenkoItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;

public class MenkoItems {
	private static final Identifier MENKO_SAKURAFROG_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "menko_sakurafrog");
	private static final Identifier MENKO_BEACH_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "menko_beach");
	private static final Identifier MENKO_MARS_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "menko_mars");
	private static final Identifier MENKO_NIGHTSKY_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "menko_nightsky");
	private static final Identifier MENKO_SPACE_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "menko_space");
	private static final Identifier MENKO_SAKURAFROG_NONFUNC_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "menko_sakurafrog_nonfunc");
	private static final Identifier MENKO_BEACH_NONFUNC_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "menko_beach_nonfunc");
	private static final Identifier MENKO_MARS_NONFUNC_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "menko_mars_nonfunc");
	private static final Identifier MENKO_NIGHTSKY_NONFUNC_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "menko_nightsky_nonfunc");
	private static final Identifier MENKO_SPACE_NONFUNC_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "menko_space_nonfunc");
	private static final Identifier START_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "start");
	private static final Identifier LEAVE_ID = Identifier.fromNamespaceAndPath(Menko.MOD_ID, "leave");
	private static final ResourceKey<Item> MENKO_SAKURAFROG_ITEM_KEY = ResourceKey.create(Registries.ITEM, MENKO_SAKURAFROG_ID);
	private static final ResourceKey<Item> MENKO_BEACH_ITEM_KEY = ResourceKey.create(Registries.ITEM, MENKO_BEACH_ID);
	private static final ResourceKey<Item> MENKO_MARS_ITEM_KEY = ResourceKey.create(Registries.ITEM, MENKO_MARS_ID);
	private static final ResourceKey<Item> MENKO_NIGHTSKY_ITEM_KEY = ResourceKey.create(Registries.ITEM, MENKO_NIGHTSKY_ID);
	private static final ResourceKey<Item> MENKO_SPACE_ITEM_KEY = ResourceKey.create(Registries.ITEM, MENKO_SPACE_ID);
	private static final ResourceKey<Item> MENKO_SAKURAFROG_NONFUNC_ITEM_KEY = ResourceKey.create(Registries.ITEM, MENKO_SAKURAFROG_NONFUNC_ID);
	private static final ResourceKey<Item> MENKO_BEACH_NONFUNC_ITEM_KEY = ResourceKey.create(Registries.ITEM, MENKO_BEACH_NONFUNC_ID);
	private static final ResourceKey<Item> MENKO_MARS_NONFUNC_ITEM_KEY = ResourceKey.create(Registries.ITEM, MENKO_MARS_NONFUNC_ID);
	private static final ResourceKey<Item> MENKO_NIGHTSKY_NONFUNC_ITEM_KEY = ResourceKey.create(Registries.ITEM, MENKO_NIGHTSKY_NONFUNC_ID);
	private static final ResourceKey<Item> MENKO_SPACE_NONFUNC_ITEM_KEY = ResourceKey.create(Registries.ITEM, MENKO_SPACE_NONFUNC_ID);
	private static final ResourceKey<Item> START_ITEM_KEY = ResourceKey.create(Registries.ITEM, START_ID);
	private static final ResourceKey<Item> LEAVE_ITEM_KEY = ResourceKey.create(Registries.ITEM, LEAVE_ID);

	public static final Item MENKO_SAKURAFROG = registerItem(MENKO_SAKURAFROG_ID, new MenkoCardItem(new Item.Properties().setId(MENKO_SAKURAFROG_ITEM_KEY).useItemDescriptionPrefix()));
	public static final Item MENKO_BEACH = registerItem(MENKO_BEACH_ID, new MenkoCardItem(new Item.Properties().setId(MENKO_BEACH_ITEM_KEY).useItemDescriptionPrefix()));
	public static final Item MENKO_MARS = registerItem(MENKO_MARS_ID, new MenkoCardItem(new Item.Properties().setId(MENKO_MARS_ITEM_KEY).useItemDescriptionPrefix()));
	public static final Item MENKO_NIGHTSKY = registerItem(MENKO_NIGHTSKY_ID, new MenkoCardItem(new Item.Properties().setId(MENKO_NIGHTSKY_ITEM_KEY).useItemDescriptionPrefix()));
	public static final Item MENKO_SPACE = registerItem(MENKO_SPACE_ID, new MenkoCardItem(new Item.Properties().setId(MENKO_SPACE_ITEM_KEY).useItemDescriptionPrefix()));
	public static final Item MENKO_SAKURAFROG_NONFUNC = registerItem(MENKO_SAKURAFROG_NONFUNC_ID, new NonFunctionalMenkoItem(new Item.Properties().setId(MENKO_SAKURAFROG_NONFUNC_ITEM_KEY).useItemDescriptionPrefix()));
	public static final Item MENKO_BEACH_NONFUNC = registerItem(MENKO_BEACH_NONFUNC_ID, new NonFunctionalMenkoItem(new Item.Properties().setId(MENKO_BEACH_NONFUNC_ITEM_KEY).useItemDescriptionPrefix()));
	public static final Item MENKO_MARS_NONFUNC = registerItem(MENKO_MARS_NONFUNC_ID, new NonFunctionalMenkoItem(new Item.Properties().setId(MENKO_MARS_NONFUNC_ITEM_KEY).useItemDescriptionPrefix()));
	public static final Item MENKO_NIGHTSKY_NONFUNC = registerItem(MENKO_NIGHTSKY_NONFUNC_ID, new NonFunctionalMenkoItem(new Item.Properties().setId(MENKO_NIGHTSKY_NONFUNC_ITEM_KEY).useItemDescriptionPrefix()));
	public static final Item MENKO_SPACE_NONFUNC = registerItem(MENKO_SPACE_NONFUNC_ID, new NonFunctionalMenkoItem(new Item.Properties().setId(MENKO_SPACE_NONFUNC_ITEM_KEY).useItemDescriptionPrefix()));
	public static final Item START = registerItem(START_ID, new MenkoStartItem(new Item.Properties().setId(START_ITEM_KEY).stacksTo(1).useItemDescriptionPrefix()));
	public static final Item LEAVE = registerItem(LEAVE_ID, new MenkoLeaveItem(new Item.Properties().setId(LEAVE_ITEM_KEY).stacksTo(1).useItemDescriptionPrefix()));
	public static final Item[] MENKO_CARDS = {MENKO_SAKURAFROG, MENKO_BEACH, MENKO_MARS, MENKO_NIGHTSKY, MENKO_SPACE};

	public static void init() {
	}

	public static Item randomMenko(RandomSource random) {
		return MENKO_CARDS[random.nextInt(MENKO_CARDS.length)];
	}

	private static <T extends Item> T registerItem(Identifier id, T item) {
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}
}
