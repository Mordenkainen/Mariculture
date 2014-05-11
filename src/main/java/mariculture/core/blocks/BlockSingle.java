package mariculture.core.blocks;

import java.util.Random;

import mariculture.Mariculture;
import mariculture.api.core.MaricultureTab;
import mariculture.core.Core;
import mariculture.core.blocks.TileAirPump.Type;
import mariculture.core.helpers.BlockHelper;
import mariculture.core.helpers.FluidHelper;
import mariculture.core.helpers.SpawnItemHelper;
import mariculture.core.helpers.cofh.ItemHelper;
import mariculture.core.lib.CraftingMeta;
import mariculture.core.lib.Extra;
import mariculture.core.lib.GuiIds;
import mariculture.core.lib.MaricultureDamage;
import mariculture.core.lib.Modules;
import mariculture.core.lib.RenderIds;
import mariculture.core.lib.RenderMeta;
import mariculture.core.network.Packet120ItemSync;
import mariculture.core.network.Packets;
import mariculture.core.util.Rand;
import mariculture.factory.Factory;
import mariculture.factory.blocks.TileFLUDDStand;
import mariculture.factory.blocks.TileGeyser;
import mariculture.factory.blocks.TileTurbineBase;
import mariculture.factory.blocks.TileTurbineGas;
import mariculture.factory.blocks.TileTurbineHand;
import mariculture.factory.blocks.TileTurbineWater;
import mariculture.factory.items.ItemArmorFLUDD;
import mariculture.fishery.blocks.TileFeeder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.FakePlayer;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockSingle extends BlockMachine {
	public BlockSingle(int i) {
		super(i, Material.piston);
		this.setCreativeTab(MaricultureTab.tabMariculture);
	}

	@Override
	public void onBlockAdded(World world, int par2, int par3, int par4) {
		super.onBlockAdded(world, par2, par3, par4);
	}

	@Override
	public float getBlockHardness(World world, int x, int y, int z) {
		switch (world.getBlockMetadata(x, y, z)) {
			case RenderMeta.AIR_PUMP: 		return 4F;
			case RenderMeta.FISH_FEEDER: 	return 0.5F;
			case RenderMeta.TURBINE_WATER: 	return 2.5F;
			case RenderMeta.FLUDD_STAND: 	return 3F;
			case RenderMeta.TURBINE_GAS: 	return 5F;
			case RenderMeta.GEYSER: 		return 1F;
			case RenderMeta.ANVIL_1: 		return 6F;
			case RenderMeta.ANVIL_2: 		return 6F;
			case RenderMeta.ANVIL_3: 		return 6F;
			case RenderMeta.ANVIL_4: 		return 6F;
			case RenderMeta.INGOT_CASTER: 	return 1F;
			case RenderMeta.NUGGET_CASTER: 	return 0.9F;
			case RenderMeta.BLOCK_CASTER: 	return 1.1F;
		}

		return 1F;
	}

	@Override
	public boolean isBlockSolidOnSide(World world, int x, int y, int z, ForgeDirection side) {
		TileEntity tile = world.getBlockTileEntity(x, y, z);
		if (tile != null) {
			if (tile instanceof TileTurbineBase) {
				return ((TileTurbineBase) tile).direction.getOpposite() == side;
			}
		}
		return false;
	}

	@Override
	public boolean rotateBlock(World world, int x, int y, int z, ForgeDirection axis) {
		TileEntity tile = world.getBlockTileEntity(x, y, z);
		if (tile != null) {
			if (tile instanceof TileTurbineBase) {
				return ((TileTurbineBase) tile).switchOrientation();
			}
			
			if(tile instanceof TileAirPump) {
				return ((TileAirPump) tile).rotate();
			}
			
			if(tile instanceof TileGeyser) {
				((TileGeyser)tile).orientation = BlockHelper.rotate(((TileGeyser)tile).orientation);
				Packets.updateTile(((TileGeyser)tile), ((TileGeyser)tile).getDescriptionPacket());
				world.markBlockForRenderUpdate(x, y, z);
			}
		}
		return false;
	}
	
	@Override
	public void onPostBlockPlaced(World world, int x, int y, int z, int side) {
		TileEntity tile = world.getBlockTileEntity(x, y, z);
		if (tile != null) {
			if (tile instanceof TileTurbineBase) {
				TileTurbineBase turbine = (TileTurbineBase) tile;
				turbine.direction = ForgeDirection.UP;
				turbine.switchOrientation();
			}
		}
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack) {
		TileEntity tile = world.getBlockTileEntity(x, y, z);
		int facing = BlockPistonBase.determineOrientation(world, x, y, z, entity);
		if (tile != null) {
			if (tile instanceof TileFLUDDStand) {
				TileFLUDDStand fludd = (TileFLUDDStand) tile;
				fludd.orientation = ForgeDirection.getOrientation(facing);
				int water = 0;
				if (stack.hasTagCompound()) {
					water = stack.stackTagCompound.getInteger("water");
				}

				fludd.tank.setCapacity(ItemArmorFLUDD.STORAGE);
				fludd.tank.setFluidID(Core.highPressureWater.getID());
				fludd.tank.setFluidAmount(water);
				Packets.updateTile(fludd, fludd.getDescriptionPacket());
			}
			
			if(tile instanceof TileGeyser) {
				((TileGeyser)tile).orientation = ForgeDirection.getOrientation(facing);
				Packets.updateTile(((TileGeyser)tile), ((TileGeyser)tile).getDescriptionPacket());
			}
		}
		
		int meta = stack.getItemDamage();
		if(meta >= RenderMeta.ANVIL_1 && meta <= RenderMeta.ANVIL_4) {
	        int l = MathHelper.floor_double((double)(entity.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
	        int i1 = world.getBlockMetadata(x, y, z) >> 2;
	        ++l;
	        l %= 4;
	
	        if (l == 0) {
	            world.setBlockMetadataWithNotify(x, y, z, RenderMeta.ANVIL_3, 2);
	        }
	
	        if (l == 1) {
	            world.setBlockMetadataWithNotify(x, y, z, RenderMeta.ANVIL_4, 2);
	        }
	
	        if (l == 2) {
	            world.setBlockMetadataWithNotify(x, y, z, RenderMeta.ANVIL_1, 2);
	        }
	
	        if (l == 3) {
	            world.setBlockMetadataWithNotify(x, y, z, RenderMeta.ANVIL_2, 2);
	        }
		}
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float f, float g, float t) {
		TileEntity tile = world.getBlockTileEntity(x, y, z);
		if (tile == null || (player.isSneaking() && !world.isRemote)) {
			return false;
		}
		
		ItemStack held = player.getCurrentEquippedItem();
		if(held != null) {
			if(held.itemID == Core.crafting.itemID && held.getItemDamage() == CraftingMeta.THERMOMETER) {
				return false;
			}
		}
		
		if (tile instanceof TileAirPump && Extra.ACTIVATE_PUMP) {	
			TileAirPump pump = (TileAirPump) tile;
			if (pump.animate == false) {
				if(Modules.isActive(Modules.diving)) {
					if(pump.updateAirArea(Type.CHECK)) {
						if(!world.isRemote)
							pump.supplyWithAir(300, 64.0D, 64.0D, 64.0D);
						pump.animate = true;
					}
				}
				if(pump.suckUpGas(1024)) {
					pump.animate = true;
				}
			}
			
			if(world.isRemote && player.isSneaking())
				((TileAirPump) tile).updateAirArea(Type.DISPLAY);
			return true;
		}
		
		if(player.isSneaking())
			return false;
		
		if(tile instanceof TileTurbineHand) {
			if(player.username.equals("[CoFH]"))
				return false;
			if(player instanceof FakePlayer) {
				return false;
			}

            TileTurbineHand turbine = (TileTurbineHand)tile;
			
			turbine.energyStorage.modifyEnergyStored(((TileTurbineHand)tile).getEnergyGenerated());
			turbine.isCreatingPower = true;
			turbine.cooldown = 5;

            player.getFoodStats().addStats(0, (float)-world.difficultySetting * 1.5F);

            if(turbine.produced >= 1200) {
                player.attackEntityFrom(MaricultureDamage.turbine, world.difficultySetting);
            }

			return true;
		}

		if (tile instanceof TileFLUDDStand) {
			player.openGui(Mariculture.instance, GuiIds.FLUDD_BLOCK, world, x, y, z);
			return true;
		}

		if (tile instanceof TileTurbineWater) {
			player.openGui(Mariculture.instance, GuiIds.TURBINE, world, x, y, z);
			return true;
		}
		
		if (tile instanceof TileTurbineGas) {
			player.openGui(Mariculture.instance, GuiIds.TURBINE_GAS, world, x, y, z);
			return true;
		}

		if (tile instanceof TileFeeder) {
			((TileFeeder) tile).updateTankSize();
			player.openGui(Mariculture.instance, GuiIds.FEEDER, world, x, y, z);
			return true;
		}
		
		if(tile instanceof TileAnvil) {
			if(player.username.equals("[CoFH]"))
				return false;
			if(player instanceof FakePlayer)
				return false;
			TileAnvil anvil = (TileAnvil) tile;
			if(anvil.getStackInSlot(0) != null) {
				new Packet120ItemSync(x, y, z, anvil.getInventory()).build();
				if (!player.inventory.addItemStackToInventory(anvil.getStackInSlot(0))) {
					if(!world.isRemote) {
						SpawnItemHelper.spawnItem(world, x, y + 1, z, anvil.getStackInSlot(0));
					}
				}
					
				anvil.setInventorySlotContents(0, null);
			} else if(player.getCurrentEquippedItem() != null) {
				ItemStack stack = player.getCurrentEquippedItem().copy();
				stack.stackSize = 1;
				anvil.setInventorySlotContents(0, stack);
				player.inventory.decrStackSize(player.inventory.currentItem, 1);
			}
			
			
			return true;
		}
		
		if(tile instanceof TileCooling) {
			if (!world.isRemote) {
				TileCooling caster = (TileCooling) tile;
				for(int i = 0; i < caster.getSizeInventory(); i++) {
					if(caster.getStackInSlot(i) != null) {
						SpawnItemHelper.spawnItem(world, x, y + 1, z, caster.getStackInSlot(i));
						caster.setInventorySlotContents(i, null);
						caster.onInventoryChanged();
					}
				}
			}
			
			return FluidHelper.handleFillOrDrain((IFluidHandler) world.getBlockTileEntity(x, y, z), player, ForgeDirection.UP);
		}
		
		if(tile instanceof TileGeyser) {
			return FluidHelper.handleFillOrDrain((IFluidHandler) world.getBlockTileEntity(x, y, z), player, ForgeDirection.UP);
		}

		return false;
	}
	
	@Override
	public int getLightValue(IBlockAccess world, int x, int y, int z) {
		TileEntity tile = world.getBlockTileEntity(x, y, z);
		if(tile instanceof TileFeeder) {
			return ((TileFeeder)tile).getLightValue();
		} else return 0;
    }
	
	@Override
	public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
		TileEntity tile = world.getBlockTileEntity(x, y, z);
		if(tile instanceof TileAnvil && ItemHelper.isPlayerHoldingItem(Core.hammer, player)) {
			if(player.username.equals("[CoFH]"))
				return;
			if(player instanceof FakePlayer)
				return;
			ItemStack hammer = player.getCurrentEquippedItem();
			if (((TileAnvil)tile).workItem(player, hammer)) {
				if(hammer.attemptDamageItem(1, Rand.rand))
					player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
			}
		}
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess block, int x, int y, int z) {
		int meta = block.getBlockMetadata(x, y, z);
		ForgeDirection facing;

		switch (meta) {
		case RenderMeta.AIR_PUMP:
			setBlockBounds(0.2F, 0F, 0.2F, 0.8F, 0.9F, 0.8F);
			break;
		case RenderMeta.GEYSER:
			TileGeyser geyser = (TileGeyser)block.getBlockTileEntity(x, y, z);
			if(geyser.orientation == ForgeDirection.UP)
				setBlockBounds(0.1F, 0.0F, 0.1F, 0.9F, 0.25F, 0.9F);
			if(geyser.orientation == ForgeDirection.DOWN)
				setBlockBounds(0.1F, 0.75F, 0.1F, 0.9F, 1.0F, 0.9F);
			if(geyser.orientation == ForgeDirection.EAST)
				setBlockBounds(0.0F, 0.1F, 0.1F, 0.25F, 0.9F, 0.9F);
			if(geyser.orientation == ForgeDirection.WEST)
				setBlockBounds(0.75F, 0.1F, 0.1F, 1F, 0.9F, 0.9F);
			if(geyser.orientation == ForgeDirection.SOUTH)
				setBlockBounds(0.1F, 0.1F, 0.0F, 0.9F, 0.9F, 0.25F);
			if(geyser.orientation == ForgeDirection.NORTH)
				setBlockBounds(0.1F, 0.1F, 0.75F, 0.9F, 0.9F, 1.0F);
			break;
		case RenderMeta.ANVIL_1:
			setBlockBounds(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F);
			break;
		case RenderMeta.ANVIL_2:
			setBlockBounds(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F);
			break;
		case RenderMeta.ANVIL_3:
			setBlockBounds(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F);
			break;
		case RenderMeta.ANVIL_4:
			setBlockBounds(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F);
		default:
			setBlockBounds(0F, 0F, 0F, 1F, 0.95F, 1F);
		}
	}

	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		if (meta == RenderMeta.GEYSER || (meta >= RenderMeta.ANVIL_1 && meta <= RenderMeta.ANVIL_4)) {
			return AxisAlignedBB.getAABBPool().getAABB((double) x + this.minX, (double) y + this.minY,
					(double) z + this.minZ, (double) x + this.maxX, (double) y + this.maxY, (double) z + this.maxZ);
		}

		return super.getCollisionBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	public TileEntity createTileEntity(World world, int meta) {
		switch (meta) {
		case RenderMeta.AIR_PUMP:
			return new TileAirPump();
		case RenderMeta.FISH_FEEDER:
			return new TileFeeder();
		case RenderMeta.TURBINE_WATER:
			return new TileTurbineWater();
		case RenderMeta.FLUDD_STAND:
			return new TileFLUDDStand();
		case RenderMeta.TURBINE_GAS:
			return new TileTurbineGas();
		case RenderMeta.TURBINE_HAND:
			return new TileTurbineHand();
		case RenderMeta.GEYSER:
			return new TileGeyser();
		case RenderMeta.INGOT_CASTER:
			return new TileIngotCaster();
		case RenderMeta.BLOCK_CASTER:
			return new TileBlockCaster();
		case RenderMeta.NUGGET_CASTER:
			return new TileNuggetCaster();
		}
		
		if(meta >= RenderMeta.ANVIL_1 && meta <= RenderMeta.ANVIL_4)
			return new TileAnvil();

		return null;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public int getRenderType() {
		return RenderIds.BLOCK_SINGLE;
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, int i, int j) {
		BlockHelper.dropItems(world, x, y, z);
		super.breakBlock(world, x, y, z, i, j);
	}

	@Override
	public boolean removeBlockByPlayer(World world, EntityPlayer player, int x, int y, int z) {
		if (!world.isRemote) {
			TileEntity tile = world.getBlockTileEntity(x, y, z);
			if (tile instanceof TileFLUDDStand) {
				if (!player.capabilities.isCreativeMode) {
					dropFLUDD(world, x, y, z);
				}
			}
		}

		return world.setBlockToAir(x, y, z);
	}

	private void dropFLUDD(World world, int x, int y, int z) {
		TileFLUDDStand tile = (TileFLUDDStand) world.getBlockTileEntity(x, y, z);
		ItemStack drop = new ItemStack(Factory.fludd);

		if (!drop.hasTagCompound()) {
			drop.setTagCompound(new NBTTagCompound());
		}

		if (tile != null) {
			drop.stackTagCompound.setInteger("water", tile.tank.getFluidAmount());
		}

		EntityItem entityitem = new EntityItem(world, (x), (float) y + 1, (z), new ItemStack(drop.itemID, 1,
				drop.getItemDamage()));

		if (drop.hasTagCompound()) {
			entityitem.getEntityItem().setTagCompound((NBTTagCompound) drop.getTagCompound().copy());
		}

		world.spawnEntityInWorld(entityitem);
	}

	@Override
	public int idDropped(int i, Random random, int j) {
		if (i == RenderMeta.FLUDD_STAND) {
			return 0;
		}

		return this.blockID;
	}
	
	@Override
	public Icon getIcon(int side, int meta) {
		if(meta == RenderMeta.GEYSER)
			return Block.hopperBlock.getIcon(0, 0);
		if(meta == RenderMeta.INGOT_CASTER || meta == RenderMeta.BLOCK_CASTER)
			return super.getIcon(side, RenderMeta.INGOT_CASTER);
		if(meta >= RenderMeta.ANVIL_1 && meta <= RenderMeta.ANVIL_4)
			return super.getIcon(side, RenderMeta.INGOT_CASTER);
		
		return icons[meta];
	}

	@Override
	public int damageDropped(int i) {
		if(i >= RenderMeta.ANVIL_1 && i <= RenderMeta.ANVIL_4)
			return RenderMeta.ANVIL_1;
		return i;
	}

	@Override
	public boolean isActive(int meta) {
		switch (meta) {
		case RenderMeta.FISH_FEEDER:
			return Modules.isActive(Modules.fishery);
		case RenderMeta.TURBINE_WATER:
			return Modules.isActive(Modules.factory);
		case RenderMeta.FLUDD_STAND:
			return false;
		case RenderMeta.TURBINE_GAS:
			return Modules.isActive(Modules.factory);
		case RenderMeta.GEYSER:
			return Modules.isActive(Modules.factory);
		case RenderMeta.TURBINE_HAND:
			return Modules.isActive(Modules.factory);
		case RenderMeta.ANVIL_2:
			return false;
		case RenderMeta.ANVIL_3:
			return false;
		case RenderMeta.ANVIL_4:
			return false;
		case RenderMeta.NUGGET_CASTER:
			return false;
		default:
			return true;
		}
	}

	@Override
	public int getMetaCount() {
		return RenderMeta.COUNT;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IconRegister iconRegister) {
		icons = new Icon[getMetaCount()];

		for (int i = 0; i < icons.length; i++) {
			if(i <= RenderMeta.ANVIL_1 || i > RenderMeta.ANVIL_4) {
				if(i != RenderMeta.BLOCK_CASTER && i != RenderMeta.NUGGET_CASTER)
				icons[i] = iconRegister.registerIcon(Mariculture.modid + ":" + getName(new ItemStack(this.blockID, 1, i)));
			}
		}
	}
}