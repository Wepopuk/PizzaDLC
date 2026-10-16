package pizza.dlc;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
import pizza.dlc.client.modules.impl.combat.*;

public final class AnchorPlacementCheck {
   private static void check(boolean value,String text) { if (!value) throw new AssertionError(text); }
   private static Object allocate(Class<?> type) throws Exception {
      var unsafe=Class.forName("sun.misc.Unsafe");var field=unsafe.getDeclaredField("theUnsafe");field.setAccessible(true);
      return unsafe.getMethod("allocateInstance",Class.class).invoke(field.get(null),type);
   }
   private static void set(Class<?> type,Object target,String name,Object value) throws Exception {
      var field=type.getDeclaredField(name);field.setAccessible(true);field.set(target,value);
   }
   private static Object get(Object target,String name) throws Exception {
      var field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);
   }
   private static Object call(Object target,String name,Class<?>[] types,Object... args) throws Exception {
      var method=target.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(target,args);
   }
   private static TestLevel world() throws Exception { var world=(TestLevel)allocate(TestLevel.class);world.blocks=new HashMap<>();return world; }
   private static Object support(AnchorAura aura,TestLevel world,BlockPos pos,Vec3 eye) throws Exception {
      return call(aura,"findSupport",new Class<?>[]{ClientLevel.class,BlockPos.class,Vec3.class,double.class},world,pos,eye,4.5);
   }
   @SuppressWarnings("unchecked") public static void main(String[] args) throws Exception {
      net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
      var mc=(Minecraft)allocate(Minecraft.class);set(Minecraft.class,null,"instance",mc);
      var pizzadlc=new PizzaDLC();set(PizzaDLC.class,null,"instance",pizzadlc);
      pizzadlc.getBus().registerLambdaFactory("pizza.dlc",(lookup,type) -> MethodHandles.privateLookupIn(type,MethodHandles.lookup()));
      var world=world();mc.level=world;var self=(LocalPlayer)allocate(LocalPlayer.class);mc.player=self;
      set(Entity.class,self,"position",new Vec3(2.5,1,.5));set(Entity.class,self,"eyeHeight",1.6F);
      set(Entity.class,self,"bb",new AABB(2.2,1,.2,2.8,2.8,.8));
      Vec3 eye=self.getEyePosition();BlockPos pos=new BlockPos(0,1,0);
      var aura=new AnchorAura();var crystal=new CrystalAura();
      world.put(pos.below(),Blocks.STONE.defaultBlockState());
      check(AnchorPlacement.allowed(world,pos) && support(aura,world,pos,eye)!=null,"Ordinary open stone ground lost its anchor placement");
      world.put(pos.below(),Blocks.OBSIDIAN.defaultBlockState());world.put(pos.north(),Blocks.STONE.defaultBlockState());
      check(!AnchorPlacement.allowed(world,pos) && support(aura,world,pos,eye)==null,"Alternate side support bypassed the obsidian floor ban");
      world.blocks.clear();world.put(pos.west(),Blocks.OBSIDIAN.defaultBlockState());
      check(!AnchorPlacement.supportAllowed(world,pos.west()) && support(aura,world,pos,eye)==null,"Obsidian was selected as a side support");
      world.blocks.clear();world.put(pos.below(),Blocks.STONE.defaultBlockState());
      for (Direction direction:Direction.Plane.HORIZONTAL) world.put(pos.relative(direction),Blocks.STONE.defaultBlockState());
      check(!AnchorPlacement.allowed(world,pos) && support(aura,world,pos,eye)==null,"One-block excavation accepted an anchor");
      world.blocks.remove(pos.east());check(!AnchorPlacement.allowed(world,pos),"Three-sided open pit accepted an anchor");
      world.blocks.clear();world.put(pos.below(),Blocks.STONE.defaultBlockState());world.put(pos.north(),Blocks.STONE.defaultBlockState());world.put(pos.south(),Blocks.STONE.defaultBlockState());
      check(AnchorPlacement.allowed(world,pos),"Open ground between two walls was incorrectly classified as a pit");
      world.put(pos.above(2),Blocks.STONE.defaultBlockState());check(!AnchorPlacement.allowed(world,pos),"Covered excavation accepted an anchor");
      world.blocks.clear();world.put(pos.below(),Blocks.STONE.defaultBlockState());crystal.setEnabled(true);
      set(CrystalAura.class,crystal,"minePos",pos);set(CrystalAura.class,crystal,"excavationWorld",world);
      ((Set<BlockPos>)get(crystal,"excavationSpots")).add(pos);
      check(!AnchorPlacement.allowed(world,pos),"Active Crystal Aura excavation was not reserved");
      call(crystal,"finishMining",new Class<?>[]{});
      check(!AnchorPlacement.allowed(world,pos) && support(aura,world,pos,eye)==null,"Finishing mining released the hole to Anchor Aura");
      crystal.setEnabled(false);check(!AnchorPlacement.allowed(world,pos),"Disabling Crystal Aura allowed filling its excavation");
      world.put(pos,Blocks.STONE.defaultBlockState());check(!CrystalAura.isReservedForExcavation(pos),"Filled excavation remained reserved");world.blocks.remove(pos);
      check(AnchorPlacement.allowed(world,pos),"Reopened ordinary terrain retained a stale excavation reservation");
      crystal.setEnabled(true);set(CrystalAura.class,crystal,"lastData",new CrystalAura.PlaceData(pos,Direction.UP,10,CrystalAura.PlaceResult.PLACE_OBSIDIAN,null));
      check(!AnchorPlacement.allowed(world,pos) && !AnchorPlacement.allowed(world,pos.above()),"Pending obsidian/crystal plan accepted an anchor before the obsidian existed");
      set(CrystalAura.class,crystal,"lastData",new CrystalAura.PlaceData(pos.below(),Direction.UP,10,CrystalAura.PlaceResult.PLACE_CRYSTAL,null));
      check(!AnchorPlacement.allowed(world,pos),"Pending crystal space accepted an anchor");set(CrystalAura.class,crystal,"lastData",null);
      ((Set<BlockPos>)get(crystal,"excavationSpots")).add(pos);set(CrystalAura.class,crystal,"excavationWorld",world);
      var another=world();mc.level=another;check(!CrystalAura.isReservedForExcavation(pos),"Excavation reservation leaked into another world");mc.level=world;
      world.put(pos.below(),Blocks.OBSIDIAN.defaultBlockState());world.put(pos,Blocks.RESPAWN_ANCHOR.defaultBlockState().setValue(RespawnAnchorBlock.CHARGE,1));
      set(AnchorAura.class,aura,"anchorPos",pos);
      var stageType=Arrays.stream(AnchorAura.class.getDeclaredClasses()).filter(type -> type.getSimpleName().equals("Stage")).findFirst().orElseThrow();
      set(AnchorAura.class,aura,"stage",Enum.valueOf((Class)stageType,"CHARGE"));
      call(aura,"tickSequence",new Class<?>[]{LocalPlayer.class,ClientLevel.class},self,world);
      check(get(aura,"anchorPos")==null && get(aura,"stage").toString().equals("IDLE"),"Active sequence retained a newly forbidden location");
      set(AnchorAura.class,aura,"parkedAnchor",pos);
      check(!(boolean)call(aura,"detonateParked",new Class<?>[]{LocalPlayer.class,ClientLevel.class,net.minecraft.world.entity.LivingEntity.class},self,world,self)
         && get(aura,"parkedAnchor")==null,"Parked sequence retained a forbidden location");
      crystal.setEnabled(false);
      System.out.println("Anchor placement: obsidian floors/side supports, pits, covered excavations, mining completion/disable, crystal reservations, refill/world reset and active/parked sequence cancellation passed; open stone ground stays usable.");
   }
   private static final class TestLevel extends ClientLevel {
      Map<BlockPos,BlockState> blocks;private TestLevel() { super(null,null,null,null,0,0,null,false,0,0); }
      void put(BlockPos pos,BlockState state) { blocks.put(pos.immutable(),state); }
      @Override public BlockState getBlockState(BlockPos pos) { return blocks.getOrDefault(pos,Blocks.AIR.defaultBlockState()); }
   }
}
