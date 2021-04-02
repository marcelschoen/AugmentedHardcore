package com.backtobedrock.augmentedhardcore.eventListeners;

import com.backtobedrock.augmentedhardcore.guis.CustomHolder;
import com.backtobedrock.augmentedhardcore.guis.Icon;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryClickListener extends AbstractEventListener {
    @EventHandler
    public void onCustomInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CustomHolder))
            return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR)
            return;

        CustomHolder customHolder = (CustomHolder) event.getView().getTopInventory().getHolder();

        Icon icon = customHolder.getIcon(event.getRawSlot());
        if (icon == null)
            return;

        icon.getClickActions().forEach(clickAction -> clickAction.execute(player));
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}