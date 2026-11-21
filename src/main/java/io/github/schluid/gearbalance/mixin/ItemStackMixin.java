package io.github.schluid.gearbalance.mixin;

import io.github.schluid.gearbalance.comfort.BreakProtection;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.schluid.gearbalance.level.LevelRules;
import io.github.schluid.gearbalance.padded.PaddedArmor;
import io.github.schluid.gearbalance.padded.Tailoring;
import io.github.schluid.gearbalance.shield.Shields;
import io.github.schluid.gearbalance.socket.Sockets;
import io.github.schluid.gearbalance.tool.ToolDurability;
import io.github.schluid.gearbalance.upgrade.Upgrades;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.locale.Language;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Locked tools don't wear out. Padded armor counts as freeze immune (like leather).
 * Upgrades and gems change modifiers and durability; padding and upgrades change the name.
 * Smithing tools and GearBalance shields take their uses from the settings.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "mineBlock", at = @At("HEAD"), cancellable = true)
	private void gearbalance$noWearWhenLocked(Level level, BlockState state, BlockPos pos, Player player, CallbackInfo ci) {
		ItemStack stack = (ItemStack) (Object) this;
		if (LevelRules.isLocked(player, stack)) {
			LevelRules.showLockedHint(player, stack);
			ci.cancel();
		}
	}

	/** Own is(TagKey) with the vanilla result, except for padded armor and the freeze tag. */
	public boolean is(TagKey<Item> tag) {
		ItemStack self = (ItemStack) (Object) this;
		if (tag == ItemTags.FREEZE_IMMUNE_WEARABLES && PaddedArmor.isActive(self) && !BreakProtection.wornOut(self)) {
			return true;
		}
		return self.typeHolder().is(tag);
	}

	/** "Gepolsterte Eisenbrustplatte", "Gehärtetes Eisenschwert", or both. */
	@ModifyReturnValue(method = "getItemName", at = @At("RETURN"))
	private Component gearbalance$name(Component name) {
		ItemStack self = (ItemStack) (Object) this;
		Component result = name;
		int level = Upgrades.level(self);
		if (PaddedArmor.isActive(self)) {
			String key = PaddedArmor.nameKey(self);
			if (key != null) {
				result = Component.translatable(key);
				// German writes "Meisterhafte gepolsterte ...": the padded word loses its capital after a prefix
				Language language = Language.getInstance();
				if (level > 0 && "true".equals(language.getOrDefault("gearbalance.upgrade.lowercase_after", "false"))) {
					String text = language.getOrDefault(key);
					result = Component.literal(text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1));
				}
			}
		}
		// Padded horse armor: "Eiserner Rossharnisch (gepolstert)"
		if (Tailoring.horsePaddingActive(self)) {
			result = Component.translatable("gearbalance.horse_padding.name", result);
		}
		if (level > 0) {
			result = Component.translatable("gearbalance.upgrade.name",
					Component.translatable("gearbalance.upgrade.word." + gearbalance$upgradeKind(self) + "." + level),
					Component.translatable(gearbalance$endingKey(self)), result);
		}
		return result;
	}

	/** Adjective ending of the prefix (German gender). An entry for the exact item wins over the piece. */
	@Unique
	private static String gearbalance$endingKey(ItemStack stack) {
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		String exact = "gearbalance.upgrade.ending." + id.getNamespace() + "." + id.getPath();
		if (Language.getInstance().has(exact)) {
			return exact;
		}
		String piece = Upgrades.piece(stack.getItem());
		return "gearbalance.upgrade.ending." + (piece == null ? "other" : piece);
	}

	/** Word set for the upgrade name: armor, blade, tool, heavy (mace, trident) or ranged. */
	@Unique
	private static String gearbalance$upgradeKind(ItemStack stack) {
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		if (path.endsWith("_sword") || path.endsWith("_axe") || path.endsWith("_spear")) {
			return "blade";
		}
		if (path.equals("mace") || path.equals("trident")) {
			return "heavy";
		}
		if (path.equals("bow") || path.equals("crossbow")) {
			return "ranged";
		}
		if (path.endsWith("_pickaxe") || path.endsWith("_shovel") || path.endsWith("_hoe")) {
			return "tool";
		}
		return "armor";
	}

	/** Both forEachModifier methods (equipment and tooltip) read the modifiers here. */
	@ModifyExpressionValue(method = {
			"forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V",
			"forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V"},
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object gearbalance$upgradeModifiers(Object modifiers) {
		if (!(modifiers instanceof ItemAttributeModifiers original)) {
			return modifiers;
		}
		ItemStack self = (ItemStack) (Object) this;
		// Worn out by the break protection: no values until it is repaired
		if (BreakProtection.wornOut(self)) {
			return ItemAttributeModifiers.EMPTY;
		}
		return Sockets.modifiers(self, Upgrades.modifiers(self, original));
	}

	/** Break protection: a damaged piece can't be used (shield, bow, crossbow, trident, hoe, flint and steel ...). */
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void gearbalance$damagedUse(Level level, Player player, InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (BreakProtection.wornOut((ItemStack) (Object) this)) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}

	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void gearbalance$damagedUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (BreakProtection.wornOut((ItemStack) (Object) this)) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}

	@Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
	private void gearbalance$damagedInteract(Player player, LivingEntity target, InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (BreakProtection.wornOut((ItemStack) (Object) this)) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}

	/** Break protection: a valuable piece that would break keeps 1 durability and becomes damaged. */
	@ModifyVariable(method = "applyDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int gearbalance$breakProtection(int damage) {
		return BreakProtection.limit((ItemStack) (Object) this, damage);
	}

	@ModifyReturnValue(method = "getMaxDamage", at = @At("RETURN"))
	private int gearbalance$upgradeDurability(int maxDamage) {
		ItemStack self = (ItemStack) (Object) this;
		int toolUses = ToolDurability.max(self);
		if (toolUses > 0) {
			return toolUses;
		}
		// GearBalance shields take their durability from the settings, the upgrade adds to it
		int shield = Shields.maxDamage(self);
		return Upgrades.maxDamage(self, shield > 0 ? shield : maxDamage);
	}
}
