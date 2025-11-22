package com.test.minestom.commands;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.entity.metadata.other.FallingBlockMeta;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.scoreboard.TeamManager;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.tag.Tag;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.test.minestom.misc.MessageBuilder.*;

/**
 * Spectator mode command for 1.7 clients.
 * Makes players invisible, sets their hitbox to zero, enables flying,
 * and hides HUD/health/hunger bars.
 */
public class SpectatorCommand extends Command {
    
    /** Tag to track if a player is in spectator mode */
    public static final Tag<Boolean> SPECTATOR_MODE = Tag.Boolean("spectator_mode");
    
    /** Tags to store the original bounding box dimensions before entering spectator mode */
    private static final Tag<Double> ORIGINAL_WIDTH = Tag.Double("spectator_original_width");
    private static final Tag<Double> ORIGINAL_HEIGHT = Tag.Double("spectator_original_height");
    private static final Tag<Double> ORIGINAL_DEPTH = Tag.Double("spectator_original_depth");
    
    /** Tags to store original health/food values */
    private static final Tag<Float> ORIGINAL_HEALTH = Tag.Float("spectator_original_health");
    private static final Tag<Integer> ORIGINAL_FOOD = Tag.Integer("spectator_original_food");
    
    /** Tag to store original custom name */
    private static final Tag<Component> ORIGINAL_CUSTOM_NAME = Tag.Transient("spectator_original_custom_name");
    
    /** Tags to store original armor pieces */
    private static final Tag<ItemStack> ORIGINAL_HELMET = Tag.ItemStack("spectator_original_helmet");
    private static final Tag<ItemStack> ORIGINAL_CHESTPLATE = Tag.ItemStack("spectator_original_chestplate");
    private static final Tag<ItemStack> ORIGINAL_LEGGINGS = Tag.ItemStack("spectator_original_leggings");
    private static final Tag<ItemStack> ORIGINAL_BOOTS = Tag.ItemStack("spectator_original_boots");
    
    /** Zero bounding box for spectators (no collision) */
    private static final BoundingBox ZERO_BOX = new BoundingBox(0, 0, 0);
    
    /** Team for spectator collision control (players can pass through spectators) */
    private static Team spectatorTeam;
    
    /** View distance for block noclip (blocks around player to make appear as entities) */
    private static final int NOCLIP_VIEW_DISTANCE = 5;
    
    /** 
     * Map to store falling block entities for each spectator.
     * Key: player UUID, Value: Map of block positions to falling block entities
     */
    private static final Map<UUID, Map<Pos, Entity>> ghostBlockEntities = new ConcurrentHashMap<>();
    
    private static boolean listenersRegistered = false;
    
    public SpectatorCommand() {
        super("sp", "spectator");
        setDefaultExecutor(this::toggleSpectator);
        
        // Register event listeners once
        if (!listenersRegistered) {
            registerListeners();
            listenersRegistered = true;
        }
    }
    
    /**
     * Register event listeners to maintain spectator mode state
     */
    private static void registerListeners() {
        var handler = MinecraftServer.getGlobalEventHandler();
        
        // Initialize spectator team for player collision control
        TeamManager teamManager = MinecraftServer.getTeamManager();
        spectatorTeam = teamManager.createTeam("spectator_collision");
        spectatorTeam.setCollisionRule(TeamsPacket.CollisionRule.NEVER); // Spectators can pass through players
        
        // Keep ghost block entities stationary and hide them from non-spectators
        handler.addListener(EntityTickEvent.class, event -> {
            Entity entity = event.getEntity();
            if (entity.getEntityType() != EntityType.FALLING_BLOCK) return;
            
            // Check if this is one of our ghost block entities
            UUID ownerId = findGhostBlockOwner(entity);
            if (ownerId == null) return;
            
            // Find the original block position for this entity
            Pos targetPos = findGhostBlockPosition(entity);
            if (targetPos == null) return;
            
            // Calculate target position - falling blocks render at block position (not center)
            // X and Z are centered, but Y should be at the block's bottom
            Pos blockCenter = new Pos(targetPos.blockX() + 0.5, targetPos.blockY(), targetPos.blockZ() + 0.5);
            
            // Always teleport to ensure position is correct (client-side physics may drift)
            // Teleport every tick to override any client-side movement
            entity.teleport(blockCenter);
            
            // Reset velocity and ensure no gravity every tick
            // This must be done every tick as client-side physics may override it
            entity.setNoGravity(true);
            entity.setVelocity(Vec.ZERO);
            
            // Ensure bounding box stays zero to prevent interference with block placement
            if (!entity.getBoundingBox().equals(ZERO_BOX)) {
                entity.setBoundingBox(ZERO_BOX);
            }
            
            // Hide entity from all players except the owner
            Player owner = null;
            for (Player p : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                if (p.getUuid().equals(ownerId)) {
                    owner = p;
                    break;
                }
            }
            
            if (owner != null) {
                for (Player viewer : entity.getViewers()) {
                    if (viewer != owner) {
                        // Hide entity from this viewer
                        viewer.sendPacket(new DestroyEntitiesPacket(entity.getEntityId()));
                    }
                }
            }
        });
        
        // Hide ghost block entities from newly spawned players
        handler.addListener(PlayerSpawnEvent.class, event -> {
            Player newPlayer = event.getPlayer();
            if (Boolean.TRUE.equals(newPlayer.getTag(SPECTATOR_MODE))) {
                return; // Don't hide from other spectators
            }
            
            // Hide all ghost block entities from this new player
            for (Map<Pos, Entity> entities : ghostBlockEntities.values()) {
                for (Entity entity : entities.values()) {
                    if (entity != null && !entity.isRemoved()) {
                        newPlayer.sendPacket(new DestroyEntitiesPacket(entity.getEntityId()));
                    }
                }
            }
        });
        
        // Allow placing blocks inside spectators - run at HIGH priority to override other cancellations
        handler.addListener(PlayerBlockPlaceEvent.class, event -> {
            Instance instance = event.getPlayer().getInstance();
            if (instance == null) return;
            
            // Check if there's a spectator near the placement position
            net.minestom.server.coordinate.BlockVec placePos = event.getBlockPosition();
            Pos placePosCenter = new Pos(placePos.x() + 0.5, placePos.y() + 0.5, placePos.z() + 0.5);
            
            for (Player spectator : instance.getPlayers()) {
                if (Boolean.TRUE.equals(spectator.getTag(SPECTATOR_MODE))) {
                    Pos spectatorPos = spectator.getPosition();
                    // Check if block is being placed near spectator (within 1.5 blocks)
                    double distance = Math.sqrt(
                        Math.pow(placePosCenter.x() - spectatorPos.x(), 2) +
                        Math.pow(placePosCenter.y() - spectatorPos.y(), 2) +
                        Math.pow(placePosCenter.z() - spectatorPos.z(), 2)
                    );
                    // If very close to spectator, ensure event is not cancelled
                    // Spectators have zero bounding box so blocks can be placed inside them
                    if (distance < 1.5) {
                        event.setCancelled(false);
                        return;
                    }
                }
            }
        });
        
        // Ensure spectators stay in flying mode and nametag stays hidden
        handler.addListener(PlayerTickEvent.class, event -> {
            Player player = event.getPlayer();
            if (Boolean.TRUE.equals(player.getTag(SPECTATOR_MODE))) {
                // Ensure bounding box stays zero (may be reset by other systems)
                if (!player.getBoundingBox().equals(ZERO_BOX)) {
                    player.setBoundingBox(ZERO_BOX);
                }
                
                // Force enable flying if disabled
                if (!player.isFlying()) {
                    player.setAllowFlying(true);
                    player.setFlying(true);
                }
                
                // Ensure nametag stays hidden
                if (player.isCustomNameVisible()) {
                    player.set(DataComponents.CUSTOM_NAME, Component.empty());
                    player.setCustomNameVisible(false);
                }
                
                // Ensure invisibility is maintained
                if (player.getEntityMeta() instanceof LivingEntityMeta livingMeta) {
                    if (!livingMeta.isInvisible()) {
                        livingMeta.setInvisible(true);
                        livingMeta.setCustomNameVisible(false);
                        player.sendPacketToViewers(player.getMetadataPacket());
                    }
                }
                
                // Prevent sprinting to avoid sprint particles
                if (player.isSprinting()) {
                    player.setSprinting(false);
                }
                
                // Ensure armor stays hidden
                if (!player.getEquipment(EquipmentSlot.HELMET).isAir()) {
                    player.setHelmet(ItemStack.AIR);
                }
                if (!player.getEquipment(EquipmentSlot.CHESTPLATE).isAir()) {
                    player.setChestplate(ItemStack.AIR);
                }
                if (!player.getEquipment(EquipmentSlot.LEGGINGS).isAir()) {
                    player.setLeggings(ItemStack.AIR);
                }
                if (!player.getEquipment(EquipmentSlot.BOOTS).isAir()) {
                    player.setBoots(ItemStack.AIR);
                }
                
                // Update block noclip - make blocks around player appear as air
                updateBlockNoclip(player);
            }
        });
        
        // Allow movement through blocks for spectators
        handler.addListener(PlayerMoveEvent.class, event -> {
            Player player = event.getPlayer();
            if (Boolean.TRUE.equals(player.getTag(SPECTATOR_MODE))) {
                // Allow movement through blocks - uncancel if it was cancelled
                if (event.isCancelled()) {
                    event.setCancelled(false);
                }
            }
        });
    }
    
    /**
     * Find which spectator owns a ghost block entity
     */
    private static UUID findGhostBlockOwner(Entity entity) {
        for (Map.Entry<UUID, Map<Pos, Entity>> entry : ghostBlockEntities.entrySet()) {
            for (Entity e : entry.getValue().values()) {
                if (e == entity) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
    
    /**
     * Find the original block position for a ghost block entity
     */
    private static Pos findGhostBlockPosition(Entity entity) {
        for (Map.Entry<UUID, Map<Pos, Entity>> entry : ghostBlockEntities.entrySet()) {
            for (Map.Entry<Pos, Entity> blockEntry : entry.getValue().entrySet()) {
                if (blockEntry.getValue() == entity) {
                    return blockEntry.getKey();
                }
            }
        }
        return null;
    }
    
    /**
     * Update block noclip - make blocks appear as falling block entities and set blocks to air
     */
    private static void updateBlockNoclip(Player player) {
        Instance instance = player.getInstance();
        if (instance == null) return;
        
        Pos playerPos = player.getPosition();
        UUID playerId = player.getUuid();
        
        // Get current map of ghost block entities for this player
        Map<Pos, Entity> currentEntities = ghostBlockEntities.getOrDefault(playerId, new ConcurrentHashMap<>());
        Map<Pos, Entity> newEntities = new ConcurrentHashMap<>();
        
        // Calculate range of blocks to check around player
        int minX = (int) Math.floor(playerPos.x() - NOCLIP_VIEW_DISTANCE);
        int minY = (int) Math.floor(playerPos.y() - NOCLIP_VIEW_DISTANCE);
        int minZ = (int) Math.floor(playerPos.z() - NOCLIP_VIEW_DISTANCE);
        int maxX = (int) Math.floor(playerPos.x() + NOCLIP_VIEW_DISTANCE);
        int maxY = (int) Math.floor(playerPos.y() + NOCLIP_VIEW_DISTANCE);
        int maxZ = (int) Math.floor(playerPos.z() + NOCLIP_VIEW_DISTANCE);
        
        // Make blocks in range appear as falling block entities
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = instance.getBlock(x, y, z);
                    if (block.isSolid()) {
                        Pos blockPos = new Pos(x, y, z);
                        
                        // Check if we already have an entity for this block
                        Entity existingEntity = currentEntities.get(blockPos);
                        if (existingEntity != null && !existingEntity.isRemoved()) {
                            // Keep existing entity
                            newEntities.put(blockPos, existingEntity);
                        } else {
                            // Create new falling block entity
                            Entity fallingBlock = new Entity(EntityType.FALLING_BLOCK);
                            
                            // Set the block data on the falling block
                            if (fallingBlock.getEntityMeta() instanceof FallingBlockMeta fallingMeta) {
                                fallingMeta.setBlock(block);
                            }
                            
                            // Disable gravity and set velocity to zero to prevent falling
                            fallingBlock.setNoGravity(true);
                            fallingBlock.setVelocity(Vec.ZERO);
                            
                            // Set bounding box to zero so entities don't interfere with block placement
                            fallingBlock.setBoundingBox(ZERO_BOX);
                            
                            // Position falling block - X and Z centered, Y at block bottom
                            // Falling blocks render with their origin at the bottom of the block
                            Pos entityPos = new Pos(x + 0.5, y, z + 0.5);
                            fallingBlock.setInstance(instance, entityPos);
                            
                            // Hide entity from all players except the spectator
                            for (Player otherPlayer : instance.getPlayers()) {
                                if (otherPlayer != player) {
                                    otherPlayer.sendPacket(new DestroyEntitiesPacket(fallingBlock.getEntityId()));
                                }
                            }
                            
                            newEntities.put(blockPos, fallingBlock);
                            
                            // Make the actual block appear as air to this player
                            BlockChangePacket packet = new BlockChangePacket(
                                    new Pos(x, y, z),
                                    Block.AIR
                            );
                            player.sendPacket(packet);
                        }
                    }
                }
            }
        }
        
        // Remove entities for blocks that are no longer in range
        for (Map.Entry<Pos, Entity> entry : currentEntities.entrySet()) {
            Pos oldPos = entry.getKey();
            Entity entity = entry.getValue();
            
            if (!newEntities.containsKey(oldPos)) {
                // Restore the original block
                Block originalBlock = instance.getBlock(oldPos.blockX(), oldPos.blockY(), oldPos.blockZ());
                BlockChangePacket packet = new BlockChangePacket(
                        new Pos(oldPos.blockX(), oldPos.blockY(), oldPos.blockZ()),
                        originalBlock
                );
                player.sendPacket(packet);
                
                // Remove the entity
                if (entity != null && !entity.isRemoved()) {
                    entity.remove();
                }
            }
        }
        
        // Update the map of ghost block entities
        ghostBlockEntities.put(playerId, newEntities);
    }
    
    private void toggleSpectator(CommandSender sender, Object context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(error("This command can only be used by players."));
            return;
        }
        
        boolean isSpectator = Boolean.TRUE.equals(player.getTag(SPECTATOR_MODE));
        
        if (isSpectator) {
            // Exit spectator mode
            exitSpectatorMode(player);
            player.sendMessage(success("You have exited spectator mode."));
        } else {
            // Enter spectator mode
            enterSpectatorMode(player);
            player.sendMessage(success("You have entered spectator mode."));
        }
    }
    
    /**
     * Enter spectator mode: make invisible, set hitbox to zero, enable flying, hide HUD
     */
    private void enterSpectatorMode(Player player) {
        // Store original bounding box dimensions
        BoundingBox originalBox = player.getBoundingBox();
        player.setTag(ORIGINAL_WIDTH, originalBox.width());
        player.setTag(ORIGINAL_HEIGHT, originalBox.height());
        player.setTag(ORIGINAL_DEPTH, originalBox.depth());
        
        // Store original health/food values
        player.setTag(ORIGINAL_HEALTH, player.getHealth());
        player.setTag(ORIGINAL_FOOD, player.getFood());
        
        // Store original custom name (if any)
        Component originalCustomName = player.get(DataComponents.CUSTOM_NAME);
        if (originalCustomName != null) {
            player.setTag(ORIGINAL_CUSTOM_NAME, originalCustomName);
        }
        
        // Store original armor pieces
        ItemStack helmet = player.getEquipment(EquipmentSlot.HELMET);
        ItemStack chestplate = player.getEquipment(EquipmentSlot.CHESTPLATE);
        ItemStack leggings = player.getEquipment(EquipmentSlot.LEGGINGS);
        ItemStack boots = player.getEquipment(EquipmentSlot.BOOTS);
        
        if (!helmet.isAir()) {
            player.setTag(ORIGINAL_HELMET, helmet);
        }
        if (!chestplate.isAir()) {
            player.setTag(ORIGINAL_CHESTPLATE, chestplate);
        }
        if (!leggings.isAir()) {
            player.setTag(ORIGINAL_LEGGINGS, leggings);
        }
        if (!boots.isAir()) {
            player.setTag(ORIGINAL_BOOTS, boots);
        }
        
        // Set hitbox to zero (prevents arrows/projectiles from hitting)
        player.setBoundingBox(ZERO_BOX);
        
        // Keep original GameMode (don't change to SPECTATOR - we're emulating it)
        // This is important for 1.7 clients which don't have SPECTATOR mode
        
        // Enable flying (spectators should always be able to fly)
        player.setAllowFlying(true);
        player.setFlying(true);
        
        // Set health/food to max to minimize HUD visibility
        // Note: In 1.7, we can't fully hide health/hunger bars, but we can make them full
        // Default max health is 20.0 in Minecraft
        player.setHealth(20.0f);
        player.setFood(20);
        
        // Hide nametag by setting empty custom name and making it invisible
        // This should hide the default player name
        player.set(DataComponents.CUSTOM_NAME, Component.empty());
        player.setCustomNameVisible(false);
        
        // Hide armor (set to air)
        player.setHelmet(ItemStack.AIR);
        player.setChestplate(ItemStack.AIR);
        player.setLeggings(ItemStack.AIR);
        player.setBoots(ItemStack.AIR);
        
        // Disable sprinting to prevent sprint particles
        player.setSprinting(false);
        
        // Also set via entity metadata to ensure it's hidden
        if (player.getEntityMeta() instanceof LivingEntityMeta livingMeta) {
            livingMeta.setInvisible(true);
            livingMeta.setCustomNameVisible(false);
            // Send metadata update to all viewers
            player.sendPacketToViewers(player.getMetadataPacket());
        }
        
        // Add player to spectator team for collision control
        if (spectatorTeam != null) {
            spectatorTeam.addMember(player.getUsername());
        }
        
        // Mark as spectator
        player.setTag(SPECTATOR_MODE, true);
    }
    
    /**
     * Exit spectator mode: restore visibility, original hitbox, health/food, and disable flying
     */
    private void exitSpectatorMode(Player player) {
        // Restore original bounding box
        Double width = player.getTag(ORIGINAL_WIDTH);
        Double height = player.getTag(ORIGINAL_HEIGHT);
        Double depth = player.getTag(ORIGINAL_DEPTH);
        
        if (width != null && height != null && depth != null) {
            player.setBoundingBox(new BoundingBox(width, height, depth));
        } else {
            // Fallback to default player bounding box
            player.setBoundingBox(new BoundingBox(0.6, 1.8, 0.6));
        }
        
        // Restore original health/food values
        Float originalHealth = player.getTag(ORIGINAL_HEALTH);
        Integer originalFood = player.getTag(ORIGINAL_FOOD);
        
        if (originalHealth != null) {
            player.setHealth(originalHealth);
        }
        if (originalFood != null) {
            player.setFood(originalFood);
        }
        
        // Disable flying
        player.setFlying(false);
        player.setAllowFlying(false);
        
        // Restore original armor
        ItemStack originalHelmet = player.getTag(ORIGINAL_HELMET);
        ItemStack originalChestplate = player.getTag(ORIGINAL_CHESTPLATE);
        ItemStack originalLeggings = player.getTag(ORIGINAL_LEGGINGS);
        ItemStack originalBoots = player.getTag(ORIGINAL_BOOTS);
        
        if (originalHelmet != null && !originalHelmet.isAir()) {
            player.setHelmet(originalHelmet);
        } else {
            player.setHelmet(ItemStack.AIR);
        }
        if (originalChestplate != null && !originalChestplate.isAir()) {
            player.setChestplate(originalChestplate);
        } else {
            player.setChestplate(ItemStack.AIR);
        }
        if (originalLeggings != null && !originalLeggings.isAir()) {
            player.setLeggings(originalLeggings);
        } else {
            player.setLeggings(ItemStack.AIR);
        }
        if (originalBoots != null && !originalBoots.isAir()) {
            player.setBoots(originalBoots);
        } else {
            player.setBoots(ItemStack.AIR);
        }
        
        // Restore original custom name
        Component originalCustomName = player.getTag(ORIGINAL_CUSTOM_NAME);
        if (originalCustomName != null && !originalCustomName.equals(Component.empty())) {
            player.set(DataComponents.CUSTOM_NAME, originalCustomName);
            player.setCustomNameVisible(true);
        } else {
            // If there was no original custom name, just set custom name visible to true
            // This should restore the default player name display
            player.setCustomNameVisible(true);
        }
        
        // Remove invisibility via entity metadata
        if (player.getEntityMeta() instanceof LivingEntityMeta livingMeta) {
            livingMeta.setInvisible(false);
            livingMeta.setCustomNameVisible(true);
            // Send metadata update to all viewers
            player.sendPacketToViewers(player.getMetadataPacket());
        }
        
        // Remove all ghost block entities and restore blocks
        removeGhostBlockEntities(player);
        
        // Remove player from spectator team
        if (spectatorTeam != null) {
            spectatorTeam.removeMember(player.getUsername());
        }
        
        // Remove spectator tags
        player.removeTag(SPECTATOR_MODE);
        player.removeTag(ORIGINAL_WIDTH);
        player.removeTag(ORIGINAL_HEIGHT);
        player.removeTag(ORIGINAL_DEPTH);
        player.removeTag(ORIGINAL_HEALTH);
        player.removeTag(ORIGINAL_FOOD);
        player.removeTag(ORIGINAL_CUSTOM_NAME);
        player.removeTag(ORIGINAL_HELMET);
        player.removeTag(ORIGINAL_CHESTPLATE);
        player.removeTag(ORIGINAL_LEGGINGS);
        player.removeTag(ORIGINAL_BOOTS);
    }
    
    /**
     * Remove all ghost block entities and restore original blocks for a spectator
     */
    private static void removeGhostBlockEntities(Player player) {
        UUID playerId = player.getUuid();
        Map<Pos, Entity> entities = ghostBlockEntities.remove(playerId);
        if (entities == null || entities.isEmpty()) return;
        
        Instance instance = player.getInstance();
        if (instance == null) return;
        
        // Remove all entities and restore blocks
        for (Map.Entry<Pos, Entity> entry : entities.entrySet()) {
            Pos pos = entry.getKey();
            Entity entity = entry.getValue();
            
            // Remove the entity
            if (entity != null && !entity.isRemoved()) {
                entity.remove();
            }
            
            // Restore the original block
            Block originalBlock = instance.getBlock(pos.blockX(), pos.blockY(), pos.blockZ());
            BlockChangePacket packet = new BlockChangePacket(
                    new Pos(pos.blockX(), pos.blockY(), pos.blockZ()),
                    originalBlock
            );
            player.sendPacket(packet);
        }
    }
}

