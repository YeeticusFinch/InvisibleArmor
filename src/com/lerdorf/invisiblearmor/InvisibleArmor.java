package com.lerdorf.invisiblearmor;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public class InvisibleArmor extends JavaPlugin implements Listener, TabExecutor {
    
    @Override
    public void onEnable() {
        
        getServer().getPluginManager().registerEvents(this, this);
        
        
    	new BukkitRunnable() {
            @Override
            public void run() {
            	for (Player p : Bukkit.getOnlinePlayers()) {
            		if (!p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
	            		for (ItemStack e : p.getEquipment().getArmorContents()) {
	            			removeInvisModel(e);
	            		}
	            		for (ItemStack e : p.getInventory().getContents()) {
	            			removeInvisModel(e);
	            		}
            		}
            	}
            }
        }.runTaskTimer(this, 0L, 1200L); // Run every minute
        
        
        getLogger().info("InvisibleArmor enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("InvisibleArmor disabled!");
    }
    
    @EventHandler
    public void onPotion(EntityPotionEffectEvent event) {
    	if (event.getEntity() instanceof Player p && event.getNewEffect() != null && event.getNewEffect().getType() == PotionEffectType.INVISIBILITY) {
    		for (ItemStack e : p.getEquipment().getArmorContents()) {
    			if (e != null) {
    				ItemMeta meta = e.getItemMeta();
    				if (meta != null) {
    					if (meta.hasEquippable()) {
    						EquippableComponent equippable = meta.getEquippable();
    						String ogModel = equippable.getModel().toString();
    						if (ogModel.toLowerCase().contains("invisible")) return;
	    					List<String> lore = meta.getLore();
	    					if (lore == null)
	    						lore = new ArrayList<String>();
	    					if (lore.size() > 0 && lore.get(lore.size()-1).contains("Model:")) return;
	    					lore.add("Model:" + ogModel);
	    					equippable.setModel(NamespacedKey.fromString("invisible"));
	    					meta.setEquippable(equippable);
	    					meta.setLore(lore);
	    					e.setItemMeta(meta);
	    					getLogger().info("Adding invisibility to " + meta.getItemName());
    					} else {
    						NBT.modifyComponents(e, components -> {
    			                ReadWriteNBT equippable = components.getOrCreateCompound("minecraft:equippable");
    			                
    			                // Set the asset_id
    			                equippable.setString("asset_id", "invisible");
    			                
    			                // If this is a new equippable component, set defaults based on item type
    			                if (!equippable.hasTag("slot")) {
    			                    setDefaultEquippableData(equippable, e.getType());
    			                }
    			                
    			            });

			                List<String> lore = meta.getLore();
	    					if (lore == null)
	    						lore = new ArrayList<String>();
	    					lore.add("Model:" + "null");
	    					meta = e.getItemMeta();
	    					meta.setLore(lore);
	    					e.setItemMeta(meta);
	    					getLogger().info("Adding invisibility to " + meta.getItemName() + " using NBTs");
    					}
    				}
    				
    			}
    		}
    	} else if (event.getEntity() instanceof Player p && event.getOldEffect() != null && event.getOldEffect().getType() == PotionEffectType.INVISIBILITY) {
    		for (ItemStack e : p.getEquipment().getArmorContents()) {
    			removeInvisModel(e);
    		}
    	}
    }
    
    private void setDefaultEquippableData(ReadWriteNBT equippable, Material material) {
        String materialName = material.name().toLowerCase();
        
        // Set slot based on material type
        if (materialName.contains("helmet") || materialName.contains("cap")) {
            equippable.setString("slot", "head");
        } else if (materialName.contains("chestplate") || materialName.contains("tunic")) {
            equippable.setString("slot", "chest");
        } else if (materialName.contains("leggings") || materialName.contains("pants")) {
            equippable.setString("slot", "legs");
        } else if (materialName.contains("boots") || materialName.contains("shoes")) {
            equippable.setString("slot", "feet");
        } else {
            // Default to chest if we can't determine
            equippable.setString("slot", "chest");
        }
        
        // Set equip sound based on material
        String equipSound = getEquipSoundString(material);
        if (equipSound != null) {
            equippable.setString("equip_sound", equipSound);
        }
        
        // Set default properties
        equippable.setBoolean("dispensable", true);
        equippable.setBoolean("swappable", true);
        equippable.setBoolean("damage_on_hurt", true);
    }
    
    private String getEquipSoundString(Material material) {
        String name = material.name().toLowerCase();
        
        if (name.contains("leather")) {
            return "minecraft:item.armor.equip_leather";
        } else if (name.contains("chain")) {
            return "minecraft:item.armor.equip_chain";
        } else if (name.contains("iron")) {
            return "minecraft:item.armor.equip_iron";
        } else if (name.contains("diamond")) {
            return "minecraft:item.armor.equip_diamond";
        } else if (name.contains("gold")) {
            return "minecraft:item.armor.equip_gold";
        } else if (name.contains("netherite")) {
            return "minecraft:item.armor.equip_netherite";
        }
        
        return "minecraft:item.armor.equip_generic";
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();
		if (item == null)
			return;
		ItemStack downgradedItem = removeInvisModel(item.clone());
		if (downgradedItem != null) {
			int slot = event.getSlot();
			HumanEntity player = event.getWhoClicked();
			Inventory inv = event.getClickedInventory();
			event.setCancelled(true);
			
			/*
			player.closeInventory();
			
			Bukkit.getScheduler().runTaskLater(this, () -> {
				player.sendMessage(ChatColor.RED + "Protection 4 is not permitted on this server");
				
	        }, 1L); // Delay of 1 tick
			*/
			//player.sendMessage(ChatColor.RED + "Protection IV is not permitted on this server");
			event.getClickedInventory().setItem(slot, downgradedItem);
			//event.setCurrentItem(null);
			//event.setCursor(null);
			//event.setResult(Result.DENY);
			

		}
	}
    
    @EventHandler
	public void dispenseArmorEvent(BlockDispenseArmorEvent event){
		ItemStack downgradedItem = removeInvisModel(event.getItem().clone());
		if (downgradedItem != null) {
			event.setItem(downgradedItem);
		}
	}
	
	 @EventHandler
	    public void PickupItem(EntityPickupItemEvent e) {
		 if (e.getItem() != null && e.getItem().getItemStack() != null) {
			 
				 ItemStack downgraded = removeInvisModel(e.getItem().getItemStack());
				 if (downgraded != null)
			        e.getItem().setItemStack(downgraded);
		 }
	}
	 

	@EventHandler
	public void playerInteractEvent(PlayerInteractEvent e){
		if(e.getAction() == Action.PHYSICAL) return;
		if((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && e.getItem() != null) {
			
			removeInvisModel(e.getItem());
		}
	}
	
	private ItemStack removeInvisModel(ItemStack e) {
		if (e != null) {
			ItemMeta meta = e.getItemMeta();
			if (meta != null) {
				if (meta.hasEquippable()) {
					EquippableComponent equippable = meta.getEquippable();
					String currentModel = equippable.getModel().toString();
					List<String> lore = meta.getLore();
					if (lore == null)
						lore = new ArrayList<String>();
					if (currentModel.toLowerCase().contains("invisible") && lore != null && lore.size() > 0 && lore.get(lore.size()-1).contains("Model:")) {
						String ogModel = lore.get(lore.size()-1).substring(lore.get(lore.size()-1).indexOf(':')+1);
						if (ogModel.contains("null")) {
							meta.setEquippable(null);
						} else {
							equippable.setModel(NamespacedKey.fromString(ogModel));
							meta.setEquippable(equippable);
						}
						//while (lore.get(lore.size()-1).contains("Model:"))
							lore.remove(lore.size()-1);
						if (lore.size() == 0)
							lore = null;
						meta.setLore(lore);
						e.setItemMeta(meta);
						getLogger().info("Removing invisibility from " + meta.getItemName());
					}
				} else {
					List<String> lore = meta.getLore();
					if (lore == null)
						lore = new ArrayList<String>();
					if (lore != null && lore.size() > 0 && lore.get(lore.size()-1).contains("Model:")) {
						String ogModel = lore.get(lore.size()-1).substring(lore.get(lore.size()-1).indexOf(':')+1);
						if (!ogModel.toLowerCase().contains("null")) {
							NBT.modifyComponents(e, components -> {
				                ReadWriteNBT equippable = components.getOrCreateCompound("minecraft:equippable");
				                
				                // Set the asset_id
				                equippable.setString("asset_id", ogModel);
				                
				                // If this is a new equippable component, set defaults based on item type
				                if (!equippable.hasTag("slot")) {
				                    setDefaultEquippableData(equippable, e.getType());
				                }
				                
				            });
						}
						lore.remove(lore.size()-1);
						if (lore.size() == 0)
							lore = null;
						meta.setLore(lore);
						e.setItemMeta(meta);
						getLogger().info("Removing invisibility from " + meta.getItemName());
					}
				}
			}
			
		}
		return null;
	}
}