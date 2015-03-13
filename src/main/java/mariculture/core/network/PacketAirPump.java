package mariculture.core.network;

import mariculture.core.tile.TileAirPump;
import mariculture.lib.helpers.ClientHelper;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class PacketAirPump extends PacketCoords implements IMessageHandler<PacketCoords, IMessage> {
    public PacketAirPump() {}

    public PacketAirPump(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public IMessage onMessage(PacketCoords message, MessageContext ctx) {
        ((TileAirPump) ClientHelper.getPlayer().worldObj.getTileEntity(message.x, message.y, message.z)).isAnimating = true;
        return null;
    }
}
