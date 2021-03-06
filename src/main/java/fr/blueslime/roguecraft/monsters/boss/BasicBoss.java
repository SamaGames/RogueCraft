package fr.blueslime.roguecraft.monsters.boss;

import fr.blueslime.roguecraft.monsters.boss.attacks.Attack;
import fr.blueslime.roguecraft.RogueCraft;
import fr.blueslime.roguecraft.arena.Arena;
import fr.blueslime.roguecraft.monsters.BasicMonster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import me.confuser.barapi.BarAPI;
import net.samagames.gameapi.GameUtils;
import net.samagames.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;

/*
 * This file is part of RogueCraft.
 *
 * RogueCraft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RogueCraft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RogueCraft.  If not, see <http://www.gnu.org/licenses/>.
 */
public class BasicBoss extends BasicMonster
{
    public final ArrayList<Attack> attacks;
    private String displayName;
    private String userHead;
    private int exploadingID;
    private int secondAttackID;
    private boolean canDoSecondAttack;
    
    public BasicBoss(EntityType typeOfMob)
    {
        super(typeOfMob);
        this.displayName = "";
        this.userHead = "";
        this.attacks = new ArrayList<>();
        this.canDoSecondAttack = true;
    }
    
    public LivingEntity spawnMob(final Arena arena, Location location, int waveCount)
    {
        final LivingEntity lEntity = location.getWorld().spawnCreature(location, this.typeOfMob);
        lEntity.setMaxHealth(this.getCalculatedHealth(waveCount) + 1);
        lEntity.setHealth(this.getCalculatedHealth(waveCount));
        lEntity.setMetadata("RC-MOBUUID", new FixedMetadataValue(RogueCraft.getPlugin(), this.uuid.toString()));
        lEntity.setMetadata("RC-BOSS", new FixedMetadataValue(RogueCraft.getPlugin(), true));
        lEntity.setCustomName(ChatColor.GOLD + this.displayName);
                
        EntityEquipment ee = lEntity.getEquipment();

        ee.setHelmet(this.getArmorHelmet());
        ee.setChestplate(this.getArmorChestplate());
        ee.setLeggings(this.getArmorLeggings());
        ee.setBoots(this.getArmorBoots());

        ee.setItemInHand(this.getAtttackWeapon());
        
        location.getWorld().strikeLightningEffect(location);
        GameUtils.broadcastSound(Sound.EXPLODE);
        
        for(Player player : Bukkit.getOnlinePlayers())
            BarAPI.setMessage(player, ChatColor.GOLD + "" + ChatColor.BOLD + this.displayName, 100.0F);
        
        this.secondAttackID = Bukkit.getScheduler().scheduleSyncRepeatingTask(RogueCraft.getPlugin(), new Runnable()
        {
            @Override
            public void run()
            {
                doSecondAttack(arena, lEntity);
            }
        }, 20L * 10, 20L * 10);

        return lEntity;
    }
    
    public void doSecondAttack(Arena arena, Entity entity)
    {
        if(this.canDoSecondAttack)
        {
            Collections.shuffle(this.attacks, new Random(System.nanoTime()));
            arena.getWorld().strikeLightningEffect(entity.getLocation());
            this.attacks.get(0).use(arena, entity);
            this.canDoSecondAttack = false;

            Bukkit.getScheduler().scheduleSyncDelayedTask(RogueCraft.getPlugin(), new Runnable()
            {
                @Override
                public void run()
                {
                    canDoSecondAttack = true;
                }
            }, 20L * 5);
        }
    }
    
    @Override
    public void onDeath(final Location location)
    {        
        Bukkit.getScheduler().cancelTask(this.secondAttackID);
        
        this.exploadingID = Bukkit.getScheduler().scheduleSyncRepeatingTask(RogueCraft.getPlugin(), new Runnable()
        {
            int compteur = 0;
            
            public void run()
            {
                if(compteur == 15)
                {
                    onDeathCallback();
                }
                else
                {
                    Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
                    FireworkMeta fwm = fw.getFireworkMeta();

                    Random r = new Random();   

                    int rt = r.nextInt(4) + 1;
                    Type type = Type.BALL;       
                    if (rt == 1) type = Type.BALL;
                    if (rt == 2) type = Type.BALL_LARGE;
                    if (rt == 3) type = Type.BURST;
                    if (rt == 4) type = Type.CREEPER;
                    if (rt == 5) type = Type.STAR;

                    int r1i = r.nextInt(17) + 1;
                    int r2i = r.nextInt(17) + 1;
                    Color c1 = ColorUtils.getColor(r1i);
                    Color c2 = ColorUtils.getColor(r2i);

                    FireworkEffect effect = FireworkEffect.builder().flicker(r.nextBoolean()).withColor(c1).withFade(c2).with(type).trail(r.nextBoolean()).build();
                    fwm.addEffect(effect);

                    int rp = r.nextInt(2) + 1;
                    fwm.setPower(rp);

                    fw.setFireworkMeta(fwm);

                    compteur++;
                }
            }
        }, 2L, 2L);
    }
    
    public void onDeathCallback()
    {
        Bukkit.getScheduler().cancelTask(this.exploadingID);
    }
    
    public void setCustomName(String displayName)
    {
        this.displayName = displayName;
    }
    
    public void setCustomHead(String userHead)
    {
        this.userHead = userHead;
    }
    
    @Override
    public double getCalculatedHealth(int waveCount)
    {
        return 150.0D + (0.05 * waveCount);
    }
    
    public double getCalculatedDamage(int waveCount)
    {
        return 5.0D + (0.05 * waveCount);
    }
    
    public String getCustomHead()
    {
        return this.userHead;
    }
    
    public boolean hasCustomHead()
    {
        return !this.userHead.equals("");
    }
}
