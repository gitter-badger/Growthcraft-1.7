package growthcraft.cellar.event;

import growthcraft.cellar.GrowthCraftCellar;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraft.item.Item;

public class ItemCraftedEventCellar
{
	@SubscribeEvent
	public void onItemCrafting(ItemCraftedEvent event)
	{
		if (event.crafting.getItem() == Item.getItemFromBlock(GrowthCraftCellar.fermentBarrel))
		{
			event.player.addStat(GrowthCraftCellar.craftBarrel, 1);
		}
	}
}
