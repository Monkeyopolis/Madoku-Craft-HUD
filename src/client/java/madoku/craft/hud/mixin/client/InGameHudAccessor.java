package madoku.craft.hud.mixin.client;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InGameHud.class)
public interface InGameHudAccessor {
	@Accessor("heartJumpEndTick")
	long madokucrafthud$getHeartJumpEndTick();

	@Accessor("ticks")
	int madokucrafthud$getTicks();
}
