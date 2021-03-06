package growthcraft.cellar;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import growthcraft.api.cellar.CellarRegistry;
import growthcraft.cellar.block.BlockBrewKettle;
import growthcraft.cellar.block.BlockFermentBarrel;
import growthcraft.cellar.block.BlockFruitPress;
import growthcraft.cellar.block.BlockFruitPresser;
import growthcraft.cellar.event.ItemCraftedEventCellar;
import growthcraft.cellar.event.LivingUpdateEventCellar;
import growthcraft.cellar.network.CommonProxy;
import growthcraft.cellar.network.PacketPipeline;
import growthcraft.cellar.potion.PotionCellar;
import growthcraft.cellar.tileentity.TileEntityBrewKettle;
import growthcraft.cellar.tileentity.TileEntityFermentBarrel;
import growthcraft.cellar.tileentity.TileEntityFruitPress;
import growthcraft.cellar.tileentity.TileEntityFruitPresser;
import growthcraft.cellar.village.ComponentVillageTavern;
import growthcraft.cellar.village.VillageHandlerCellar;
import growthcraft.core.AchievementPageGrowthcraft;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.VillagerRegistry;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.stats.Achievement;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.oredict.ShapedOreRecipe;

@Mod(modid = "Growthcraft|Cellar",name = "Growthcraft Cellar",version = "@VERSION@",dependencies = "required-after:Growthcraft")
public class GrowthCraftCellar
{
	@Instance("Growthcraft|Cellar")
	public static GrowthCraftCellar instance;

	@SidedProxy(clientSide="growthcraft.cellar.network.ClientProxy", serverSide="growthcraft.cellar.network.CommonProxy")
	public static CommonProxy proxy;

	public static CreativeTabs tab;

	public static Block fruitPress;
	public static Block fruitPresser;
	public static Block brewKettle;
	public static Block fermentBarrel;

	public static Potion potionTipsy;

	///////IDS////////

	public static int potionTipsy_id;

	public static int villagerBrewer_id;

	// Constants
	public static ItemStack residue = new ItemStack(Items.dye, 1, 15);
	public static final int BOTTLE_VOLUME = 333;
	public static final ItemStack EMPTY_BOTTLE = new ItemStack(Items.glass_bottle);

	// Achievments
	public static Item chievItemDummy;

	public static Achievement craftBarrel;
	public static Achievement fermentBooze;
	public static Achievement getDrunk;

	public static int ferment_speed;

	public static final PacketPipeline packetPipeline = new PacketPipeline();

	@EventHandler
	public void preload(FMLPreInitializationEvent event)
	{
		//====================
		// CONFIGURATION
		//====================
		Configuration config = new Configuration(new File(event.getModConfigurationDirectory(), "growthcraft/cellar.conf"));
		try
		{
			config.load();

			potionTipsy_id = config.get("Potions", "Potion Tipsy ID", 50).getInt();

			villagerBrewer_id = config.get("Villager", "Brewer ID", 10).getInt();

			int v = 24000;
			Property cfgA = config.get(Configuration.CATEGORY_GENERAL, "Ferment Barrel fermenting time", v);
			cfgA.comment = "[Higher -> Slower] Default : " + v;
			this.ferment_speed = cfgA.getInt(v);
		}
		finally
		{
			if (config.hasChanged()) { config.save(); }
		}

		//====================
		// INIT
		//====================
		fermentBarrel = (new BlockFermentBarrel());
		tab = new CreativeTabCellar("tabGrCCellar");
		fermentBarrel.setCreativeTab(GrowthCraftCellar.tab);
		fruitPress    = (new BlockFruitPress());
		fruitPresser  = (new BlockFruitPresser());
		brewKettle    = (new BlockBrewKettle());

		chievItemDummy = (new ItemChievDummy());

		//====================
		// REGISTRIES
		//====================
		GameRegistry.registerBlock(fruitPress, "grc.fruitPress");
		GameRegistry.registerBlock(fruitPresser, "grc.fruitPresser");
		GameRegistry.registerBlock(brewKettle, "grc.brewKettle");
		GameRegistry.registerBlock(fermentBarrel, "grc.fermentBarrel");

		GameRegistry.registerItem(chievItemDummy, "grc.chievItemDummy");

		GameRegistry.registerTileEntity(TileEntityFruitPress.class, "grc.tileentity.fruitPress");
		GameRegistry.registerTileEntity(TileEntityFruitPresser.class, "grc.tileentity.fruitPresser");
		GameRegistry.registerTileEntity(TileEntityBrewKettle.class, "grc.tileentity.brewKettle");
		GameRegistry.registerTileEntity(TileEntityFermentBarrel.class, "grc.tileentity.fermentBarrel");

		try
		{
			MapGenStructureIO.func_143031_a(ComponentVillageTavern.class, "grc.tavern");
		}
		catch (Throwable e) {}

		//====================
		// CRAFTING
		//====================
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(fruitPress, 1), "ABA", "CCC", "AAA", 'A', "plankWood", 'B', Blocks.piston,'C', "ingotIron"));
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(brewKettle, 1), "A", 'A', Items.cauldron));
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(fermentBarrel, 1), "AAA", "BBB", "AAA", 'B', "plankWood", 'A', "ingotIron"));

		//====================
		// POTION
		//====================
		Potion[] potionTypes = null;

		for (Field f : Potion.class.getDeclaredFields())
		{
			f.setAccessible(true);
			try
			{
				if (f.getName().equals("potionTypes") || f.getName().equals("field_76425_a"))
				{
					Field modfield = Field.class.getDeclaredField("modifiers");
					modfield.setAccessible(true);
					modfield.setInt(f, f.getModifiers() & ~Modifier.FINAL);

					potionTypes = (Potion[])f.get(null);
					final Potion[] newPotionTypes = new Potion[256];
					System.arraycopy(potionTypes, 0, newPotionTypes, 0, potionTypes.length);
					f.set(null, newPotionTypes);
				}
			}
			catch (Exception e)
			{
				System.err.println("Severe error, please report this to the mod author:");
				System.err.println(e);
			}
		}

		potionTipsy = (new PotionCellar(potionTipsy_id, false, 0)).setIconIndex(0, 0).setPotionName("grc.potion.tipsy");

		//====================
		// ACHIEVEMENTS
		//====================
		craftBarrel  = (new Achievement("grc.achievement.craftBarrel", "craftBarrel", -4, -4, fermentBarrel, (Achievement)null)).initIndependentStat().registerStat();
		fermentBooze = (new Achievement("grc.achievement.fermentBooze", "fermentBooze", -2, -4, Items.nether_wart, craftBarrel)).registerStat();
		getDrunk     = (new Achievement("grc.achievement.getDrunk", "getDrunk", 0, -4, new ItemStack(chievItemDummy, 1, 0), fermentBooze)).setSpecial().registerStat();

		AchievementPageGrowthcraft.chievMasterList.add(craftBarrel);
		AchievementPageGrowthcraft.chievMasterList.add(fermentBooze);
		AchievementPageGrowthcraft.chievMasterList.add(getDrunk);

		CellarRegistry.instance().addHeatSource(Blocks.fire);
		CellarRegistry.instance().addHeatSource(Blocks.lava);
		CellarRegistry.instance().addHeatSource(Blocks.flowing_lava);
	}

	@EventHandler
	public void load(FMLInitializationEvent event)
	{
		proxy.initRenders();

		packetPipeline.initialise();
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandlerCellar());

		VillagerRegistry.instance().registerVillageCreationHandler(new VillageHandlerCellar());
		proxy.registerVillagerSkin();
	}

	@EventHandler
	public void postload(FMLPostInitializationEvent event)
	{
		packetPipeline.postInitialise();
		MinecraftForge.EVENT_BUS.register(new ItemCraftedEventCellar());
		MinecraftForge.EVENT_BUS.register(new LivingUpdateEventCellar());

		/*String modid;

		modid = "Thaumcraft";
		if (Loader.isModLoaded(modid))
		{
			try
			{
				ThaumcraftApi.registerObjectTag(fruitPress.blockID, -1, new AspectList().add(Aspect.CRAFT, 2).add(Aspect.MECHANISM, 2));
				ThaumcraftApi.registerObjectTag(brewKettle.blockID, -1, new AspectList().add(Aspect.CRAFT, 2).add(Aspect.WATER, 2));
				ThaumcraftApi.registerObjectTag(fermentBarrel.blockID, -1, new AspectList().add(Aspect.CRAFT, 2).add(Aspect.WATER, 2));

				FMLLog.info("[Growthcraft|Cellar] Successfully integrated with Thaumcraft.", new Object[0]);
			}
			catch (Exception e)
			{
				FMLLog.info("[Growthcraft|Cellar] Thaumcraft not found. No integration made.", new Object[0]);
			}
		}*/
	}
}
