package dev.emi.pockettools.item;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ClickType;
import net.minecraft.world.World;

import java.util.Map;
import java.util.stream.Collectors;

public class PocketGrindstone extends Item {

	public PocketGrindstone(Settings settings) {
		super(settings);
	}
	
	@Override
	public boolean onClicked(ItemStack stack, ItemStack applied, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursor) {
		if (clickType == ClickType.RIGHT && (applied.hasEnchantments() || applied.isOf(Items.ENCHANTED_BOOK))) {
			var world = player.world;
			if (!world.isClient()) {
				ExperienceOrbEntity.spawn((ServerWorld) world, player.getPos(), getExperience(applied, world));
			}
			cursor.set(grind(applied));
			world.syncWorldEvent(1042, player.getBlockPos(), 0);
			return true;
		}
		return false;
	}

	private ItemStack grind(ItemStack stack) {
		var copy = stack.copy();
		stack.removeSubTag("Enchantments");
		stack.removeSubTag("StoredEnchantments");

		Map<Enchantment, Integer> map = EnchantmentHelper.get(copy).entrySet().stream()
				.filter(entry -> entry.getKey().isCursed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		EnchantmentHelper.set(map, stack);
		stack.setRepairCost(0);
		if (stack.isOf(Items.ENCHANTED_BOOK) && map.size() == 0) {
			stack = new ItemStack(Items.BOOK);
			if (copy.hasCustomName()) {
				stack.setCustomName(copy.getName());
			}
		}

		for(int i = 0; i < map.size(); ++i) {
			stack.setRepairCost(AnvilScreenHandler.getNextCost(stack.getRepairCost()));
		}
		return stack;
	}

	private int getExperience(ItemStack stack, World world) {
		int i = 0;
		Map<Enchantment, Integer> map = EnchantmentHelper.get(stack);

		for (var entry : map.entrySet()) {
			var enchantment = entry.getKey();
			var integer = entry.getValue();
			if (!enchantment.isCursed()) {
				i += enchantment.getMinPower(integer);
			}
		}
		int j = (int)Math.ceil((double)i / 2.0D);
		return j + world.random.nextInt(j);
	}
}
