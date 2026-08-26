package com.github.willrees23.listener;

import com.github.willrees23.config.ZiplineConfig;
import com.github.willrees23.util.ChatUtil;
import com.github.willrees23.zipline.Zipline;
import com.github.willrees23.zipline.ZiplineProtection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Keeps the blocks of a zipline's path in the world, for lines set to be unbreakable.
 *
 * <p>Every way a block can be taken out from under the plugin is covered, not merely mining it:
 * a line strung from a wooden fence that burns down, or that a creeper takes a bite out of, is
 * just as broken as one that was dug out by hand, and the plugin would go on riding through the
 * gap either way.
 *
 * <p>Deleting a zipline is unaffected. The renderer puts its blocks back by writing to the world
 * directly, which raises none of these events.
 */
public class ZiplineProtectionListener implements Listener {

    private final ZiplineProtection protection;
    private final ZiplineConfig config;

    public ZiplineProtectionListener(ZiplineProtection protection, ZiplineConfig config) {
        this.protection = protection;
        this.config = config;
    }

    /**
     * The only case a player is told about, since it is the only one they went and did on purpose.
     *
     * <p>The break is refused either way; the message is only how they find out why.
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Zipline zipline = protection.owner(event.getBlock());
        if (zipline == null) {
            return;
        }

        event.setCancelled(true);

        String message = config.getUnbreakableMessage();
        if (message != null) {
            ChatUtil.sendHighlighted(event.getPlayer(), message, zipline.getId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (protection.isProtected(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancelling an explosion outright would spare everything else caught in it, so the protected
     * blocks are taken off the list and the rest of the blast is left to go ahead.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(protection::isProtected);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(protection::isProtected);
    }

    /**
     * Endermen carrying a block off, and anything else that turns one block into another.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (protection.isProtected(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /**
     * A piston moves every block it is pushing at once, so there is no moving the rest of them
     * without moving the protected one; the whole push is stopped instead.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(protection::isProtected)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(protection::isProtected)) {
            event.setCancelled(true);
        }
    }
}
