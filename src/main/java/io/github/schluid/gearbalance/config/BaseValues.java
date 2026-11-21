package io.github.schluid.gearbalance.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The base every preset starts from (same format as an exported setup). The sword is the main weapon and improves
 * with every material; spears keep vanilla speed and reach (about 2/3 of the sword's damage per second); axes stay vanilla.
 */
final class BaseValues {
	private BaseValues() {
	}

	/** A fresh copy, so a preset can never change the base itself. */
	static JsonObject json() {
		return JsonParser.parseString(JSON).getAsJsonObject();
	}

	private static final String JSON = """
		{
		  "gearbalanceExport": 1,
		  "items": {
		    "minecraft:wooden_sword": {
		      "damage": 2.0
		    },
		    "minecraft:wooden_spear": {
		      "damage": 1.5
		    },
		    "minecraft:stone_sword": {
		      "damage": 4.0,
		      "speed": 1.2
		    },
		    "minecraft:stone_spear": {
		      "damage": 2.5
		    },
		    "minecraft:copper_sword": {
		      "damage": 6.0,
		      "speed": 1.3
		    },
		    "minecraft:copper_spear": {
		      "damage": 4.5
		    },
		    "minecraft:iron_sword": {
		      "damage": 7.0,
		      "speed": 1.4
		    },
		    "minecraft:iron_spear": {
		      "damage": 6.0
		    },
		    "minecraft:golden_sword": {
		      "durability": 192,
		      "damage": 8.0,
		      "speed": 1.5
		    },
		    "minecraft:golden_spear": {
		      "durability": 192,
		      "damage": 7.5
		    },
		    "minecraft:golden_pickaxe": {
		      "durability": 192
		    },
		    "minecraft:golden_axe": {
		      "durability": 192
		    },
		    "minecraft:golden_shovel": {
		      "durability": 192
		    },
		    "minecraft:golden_hoe": {
		      "durability": 192
		    },
		    "minecraft:diamond_sword": {
		      "damage": 9.0
		    },
		    "minecraft:diamond_spear": {
		      "damage": 10.0
		    },
		    "minecraft:netherite_sword": {
		      "durability": 3047,
		      "damage": 12.0,
		      "speed": 1.8
		    },
		    "minecraft:netherite_spear": {
		      "durability": 3047,
		      "damage": 16.5
		    },
		    "minecraft:netherite_pickaxe": {
		      "durability": 3047,
		      "mining": 2.0
		    },
		    "minecraft:netherite_axe": {
		      "durability": 3047,
		      "mining": 2.0
		    },
		    "minecraft:netherite_shovel": {
		      "durability": 3047,
		      "mining": 2.0
		    },
		    "minecraft:netherite_hoe": {
		      "durability": 3047,
		      "mining": 2.0
		    },
		    "gearbalance:titanium_sword": {
		      "durability": 3504,
		      "damage": 13.0,
		      "speed": 1.9
		    },
		    "gearbalance:titanium_spear": {
		      "durability": 3504,
		      "damage": 17.5
		    },
		    "gearbalance:titanium_pickaxe": {
		      "durability": 3504,
		      "mining": 2.3
		    },
		    "gearbalance:titanium_axe": {
		      "durability": 3504,
		      "mining": 2.3
		    },
		    "gearbalance:titanium_shovel": {
		      "durability": 3504,
		      "mining": 2.3
		    },
		    "gearbalance:titanium_hoe": {
		      "durability": 3504,
		      "mining": 2.3
		    },
		    "minecraft:mace": {
		      "damage": 8.0
		    },
		    "minecraft:bow": {
		      "durability": 500
		    },
		    "minecraft:shield": {
		      "durability": 500,
		      "shield_disable": 1.0
		    },
		    "minecraft:leather_helmet": {
		      "durability": 110
		    },
		    "minecraft:leather_chestplate": {
		      "durability": 160
		    },
		    "minecraft:leather_leggings": {
		      "durability": 150
		    },
		    "minecraft:leather_boots": {
		      "durability": 130
		    },
		    "minecraft:chainmail_helmet": {
		      "knockback": 0.15
		    },
		    "minecraft:chainmail_chestplate": {
		      "knockback": 0.15
		    },
		    "minecraft:chainmail_leggings": {
		      "knockback": 0.15
		    },
		    "minecraft:chainmail_boots": {
		      "knockback": 0.15
		    },
		    "minecraft:iron_helmet": {
		      "toughness": 1.0
		    },
		    "minecraft:iron_chestplate": {
		      "toughness": 1.0
		    },
		    "minecraft:iron_leggings": {
		      "toughness": 1.0
		    },
		    "minecraft:iron_boots": {
		      "toughness": 1.0
		    },
		    "minecraft:golden_helmet": {
		      "durability": 231,
		      "knockback": 0.05
		    },
		    "minecraft:golden_chestplate": {
		      "durability": 336,
		      "knockback": 0.05
		    },
		    "minecraft:golden_leggings": {
		      "durability": 315,
		      "knockback": 0.05
		    },
		    "minecraft:golden_boots": {
		      "durability": 273,
		      "knockback": 0.05
		    },
		    "minecraft:diamond_helmet": {
		      "knockback": 0.1
		    },
		    "minecraft:diamond_chestplate": {
		      "knockback": 0.1
		    },
		    "minecraft:diamond_leggings": {
		      "knockback": 0.1
		    },
		    "minecraft:diamond_boots": {
		      "knockback": 0.1
		    },
		    "minecraft:netherite_helmet": {
		      "durability": 611,
		      "toughness": 4.0,
		      "knockback": 0.2
		    },
		    "minecraft:netherite_chestplate": {
		      "durability": 888,
		      "toughness": 4.0,
		      "knockback": 0.2
		    },
		    "minecraft:netherite_leggings": {
		      "durability": 833,
		      "toughness": 4.0,
		      "knockback": 0.2
		    },
		    "minecraft:netherite_boots": {
		      "durability": 722,
		      "toughness": 4.0,
		      "knockback": 0.2
		    },
		    "gearbalance:titanium_helmet": {
		      "durability": 703,
		      "toughness": 4.5,
		      "knockback": 0.25
		    },
		    "gearbalance:titanium_chestplate": {
		      "durability": 1021,
		      "toughness": 4.5,
		      "knockback": 0.25
		    },
		    "gearbalance:titanium_leggings": {
		      "durability": 958,
		      "toughness": 4.5,
		      "knockback": 0.25
		    },
		    "gearbalance:titanium_boots": {
		      "durability": 830,
		      "toughness": 4.5,
		      "knockback": 0.25
		    }
		  },
		  "materials": {
		    "wood": {
		      "repair": 30
		    },
		    "leather": {
		      "repair": 50
		    },
		    "stone": {
		      "repair": 10
		    },
		    "copper": {
		      "level": 5,
		      "weight": 2.0
		    },
		    "chainmail": {
		      "level": 10,
		      "weight": 3.0
		    },
		    "iron": {
		      "level": 15,
		      "weight": 6.0
		    },
		    "gold": {
		      "level": 20,
		      "weight": 4.0
		    },
		    "diamond": {
		      "level": 25,
		      "weight": 5.0
		    },
		    "netherite": {
		      "level": 35,
		      "weight": 3.0
		    },
		    "titanium": {
		      "level": 45,
		      "weight": 2.0
		    }
		  },
		  "sets": {
		    "leather": {
		      "bonus": "speed",
		      "strength": 10.0
		    },
		    "copper": {
		      "bonus": "attack_speed",
		      "strength": 8.0
		    },
		    "chainmail": {
		      "bonus": "knockback_resistance",
		      "strength": 20.0
		    },
		    "iron": {
		      "bonus": "health",
		      "strength": 4.0
		    },
		    "gold": {
		      "bonus": "luck",
		      "strength": 4.0
		    },
		    "diamond": {
		      "bonus": "toughness",
		      "strength": 4.0
		    },
		    "netherite": {
		      "bonus": "burning",
		      "strength": 50.0
		    },
		    "titanium": {
		      "bonus": "toughness",
		      "strength": 5.0
		    }
		  },
		  "switches": {
		    "setBonuses": true,
		    "tridentRecipe": true,
		    "chainmailRecipes": true,
		    "rabbitHideRecipe": true,
		    "rabbitHideArmor": true,
		    "anvilNoLimit": true,
		    "anvilMaxCost": 39,
		    "zombieLeather": true,
		    "zombieLeatherChance": 33,
		    "lootTemplates": true,
		    "lootMaterials": true,
		    "lootNoCheap": true,
		    "shieldWear": false,
		    "shieldWearStart": 50,
		    "shieldWearMin": 60
		  }
		}
		""";
}
