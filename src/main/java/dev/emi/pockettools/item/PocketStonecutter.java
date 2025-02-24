package dev.emi.pockettools.item;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.pockettools.tooltip.ConvertibleTooltipData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ClickType;
import net.minecraft.util.math.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PocketStonecutter extends Item {

	public PocketStonecutter(Settings settings) {
		super(settings);
	}

	@Override
	public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType,  PlayerEntity player, StackReference cursorStackReference) {
		var world = player.world;
		var nbt = stack.getOrCreateTag();
		if (clickType == ClickType.RIGHT) {
			if (otherStack.isEmpty()) {
				if (nbt.contains("base")) {
					var base = ItemStack.fromNbt(nbt.getCompound("base"));
					var list = world.getRecipeManager().getAllMatches(RecipeType.STONECUTTING, new SimpleInventory(base), world);
					int offset = -1;
					if (nbt.contains("offset")) {
						offset = nbt.getInt("offset");
					}
					offset++;
					if (offset >= list.size()) {
						offset = 0;
					}
					nbt.putInt("offset", offset);
					if (world.isClient) {
						world.playSound(player, player.getBlockPos(), SoundEvents.UI_STONECUTTER_SELECT_RECIPE, SoundCategory.BLOCKS, 1.0F, 1.0F);
					}
					return true;
				}
			} else {
				var list = world.getRecipeManager().getAllMatches(RecipeType.STONECUTTING, new SimpleInventory(otherStack), world);
				if (!list.isEmpty()) {
					nbt.put("base", otherStack.writeNbt(new NbtCompound()));
					nbt.putInt("offset", 0);
					if (world.isClient) {
						world.playSound(player, player.getBlockPos(), SoundEvents.UI_STONECUTTER_SELECT_RECIPE, SoundCategory.BLOCKS, 1.0F, 1.0F);
					}
					return true;
				}
			}
		}
		return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference);
	}

	@Override
	public boolean onStackClicked(ItemStack self, Slot slot, ClickType clickType, PlayerEntity player) {
		var world = player.world;
		var stack = slot.getStack();
		if (clickType == ClickType.RIGHT) {
			var tag = self.getOrCreateTag();
			if (tag.contains("base")) {
				ItemStack base = ItemStack.fromNbt(tag.getCompound("base"));
				if (base.isItemEqual(stack)) {
					List<StonecuttingRecipe> list = world.getRecipeManager().getAllMatches(RecipeType.STONECUTTING, new SimpleInventory(stack), world);
					int offset = 0;
					if (tag.contains("offset")) {
						offset = tag.getInt("offset");
					}
					if (offset < list.size()) {
						ItemStack output = list.get(offset).getOutput().copy();
						int count = output.getCount() * stack.getCount();
						output.setCount(Math.min(count, output.getMaxCount()));
						count -= output.getCount();
						if (slot.canInsert(output)) {
							slot.setStack(output);
							while (count > 0) {
								output = output.copy();
								output.setCount(Math.min(count, output.getMaxCount()));
								count -= output.getCount();
								player.getInventory().offerOrDrop(output);
							}
							if (world.isClient) {
								world.playSound(player, player.getBlockPos(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundCategory.BLOCKS, 1.0F, 1.0F);
							}
							return true;
						}
					}
				}
			}
		}
		return super.onStackClicked(self, slot, clickType, player);
	}

	@Override
	public Optional<TooltipData> getTooltipData(ItemStack stack) {
		return Optional.of(new PocketStonecutterTooltip(stack));
	}

	class PocketStonecutterTooltip implements ConvertibleTooltipData, TooltipComponent {
		public List<StonecuttingRecipe> list = new ArrayList<>();
		public ItemStack stack;

		public PocketStonecutterTooltip(ItemStack stack) {
			this.stack = stack;
			var nbt = stack.getOrCreateTag();
			if (nbt.contains("base")) {
				var base = ItemStack.fromNbt(nbt.getCompound("base"));
				var client = MinecraftClient.getInstance();
				var world = client.world;
				list = world.getRecipeManager().getAllMatches(RecipeType.STONECUTTING, new SimpleInventory(base), world);
			}
		}

		@Override
		public TooltipComponent getComponent() {
			return this;
		}

		@Override
		public int getHeight() {
			if (!list.isEmpty()) {
				return ((list.size() - 1) / 4 + 1) * 18 + 4;
			}
			return 0;
		}

		@Override
		public int getWidth(TextRenderer textRenderer) {
			return 18 * 4 + 4;
		}

		@Override
		public void drawItems(TextRenderer textRenderer, int x, int y, MatrixStack matrices, ItemRenderer itemRenderer, int z, TextureManager textureManager) {
			var nbt = stack.getOrCreateTag();
			if (!list.isEmpty()) {
				int offset = 0;
				if (nbt.contains("offset")) {
					offset = nbt.getInt("offset");
				}
				int sx = 0;
				int sy = 0;
				for (var recipe : list) {
					itemRenderer.renderGuiItemIcon(recipe.getOutput(), x + sx * 18 + 2, y + sy * 18 + 2);
					itemRenderer.renderGuiItemOverlay(textRenderer, recipe.getOutput(), x + sx * 18 + 2, y + sy * 18 + 2);
					sx++;
					if (sx >= 4) {
						sx = 0;
						sy++;
					}
				}
				sx = offset % 4;
				sy = offset / 4;
				RenderSystem.setShaderColor(0.0F, 0.6F, 0.7F, 1.0F);
				RenderSystem.setShaderTexture(0, DrawableHelper.STATS_ICON_TEXTURE);
				DrawableHelper.drawTexture(matrices, x + sx * 18 + 1, y + sy * 18 + 1, z, 0.0F, 0.0F, 18, 18, 128, 128);
			}
			TooltipComponent.super.drawItems(textRenderer, x, y, matrices, itemRenderer, z, textureManager);
		}
		@Override
		public void drawText(TextRenderer textRenderer, int x, int y, Matrix4f matrix4f, Immediate immediate) {
			TooltipComponent.super.drawText(textRenderer, x, y, matrix4f, immediate);
		}
	}
}
