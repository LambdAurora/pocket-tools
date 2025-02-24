package dev.emi.pockettools.item;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ClickType;

public class PocketEnderChest extends Item {

	public PocketEnderChest(Settings settings) {
		super(settings);
	}

	@Override
	public boolean onClicked(ItemStack self, ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursor) {
		var world = player.world;
		if (clickType == ClickType.RIGHT && stack.isEmpty()) {
			if (world.isClient()) {
				world.playSound(player, player.getBlockPos(), SoundEvents.BLOCK_ENDER_CHEST_OPEN, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
			} else {
				player.playerScreenHandler.enableSyncing();
				player.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) -> {
					return GenericContainerScreenHandler.createGeneric9x3(i, playerInventory, player.getEnderChestInventory());
				}, new TranslatableText("container.enderchest")));
			}
			return true;
		}
		return false;
	}
}
