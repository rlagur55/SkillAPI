package com.sucy.skill.mechanic;

import com.sucy.skill.api.PlayerSkills;
import com.sucy.skill.api.dynamic.DynamicSkill;
import com.sucy.skill.api.dynamic.IMechanic;
import com.sucy.skill.api.dynamic.Target;
import com.sucy.skill.api.dynamic.TimedEmbedData;
import com.sucy.skill.api.event.AttackType;
import com.sucy.skill.api.event.PlayerOnDamagedEvent;
import com.sucy.skill.api.event.PlayerOnHitEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Mechanic for applying embedded effects on attack
 */
public class DefenseModifierMechanic implements IMechanic, Listener {

    private static final String
            TYPE = "Attack Type",
            ATTACKS = "Attacks",
            DURATION = "Modifier Duration",
            CHANCE = "Modifier Chance";

    private static final int
            MELEE = 1,
            PROJECTILE = 2;

    private final HashMap<Integer, TimedEmbedData> activeEffects = new HashMap<Integer, TimedEmbedData>();
    private final Random random = new Random();

    /**
     * Constructor
     */
    public DefenseModifierMechanic() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("SkillAPI");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Grants a temporary damage bonus to the targets
     *
     * @param player  player using the skill
     * @param data    data of the player using the skill
     * @param skill   skill being used
     * @param target  target type of the skill
     * @param targets targets for the effects
     * @return        true if was able to use
     */
    @Override
    public boolean resolve(Player player, PlayerSkills data, DynamicSkill skill, Target target, List<LivingEntity> targets) {

        // Add the player to the map
        int level = data.getSkillLevel(skill.getName());
        int duration = (int)(skill.getAttribute(DURATION, target, level) * 1000);
        int attacks = (int)skill.getAttribute(ATTACKS, target, level);
        int chance = skill.hasAttribute(CHANCE, target) ? (int)skill.getAttribute(CHANCE, target, level) : 100;
        for (LivingEntity t : targets) {
            TimedEmbedData embedData = new TimedEmbedData(player, data, skill, System.currentTimeMillis() + duration);
            embedData.setValue(ATTACKS, attacks);
            embedData.setValue(CHANCE, chance);
            activeEffects.put(t.getEntityId(), embedData);
        }
        return true;
    }

    /**
     * Applies effects on attack
     *
     * @param event event details
     */
    @EventHandler
    public void onAttack(PlayerOnDamagedEvent event) {

        // Make sure the player is embedded
        int id = event.getPlayer().getEntityId();
        if (!activeEffects.containsKey(id)) return;

        // Check expiration time
        TimedEmbedData data = activeEffects.get(id);
        if (data.isExpired()) {
            activeEffects.remove(id);
            return;
        }

        // Roll the chance
        if (random.nextDouble() * 100 >= data.getValue(CHANCE)) {
            return;
        }

        // Make sure its the correct attack type
        int attackType = data.getSkill().getValue(TYPE);
        if (event.getAttackType() == AttackType.MELEE && attackType == PROJECTILE) return;
        if (event.getAttackType() == AttackType.PROJECTILE && attackType == MELEE) return;

        // Decrement the number of attacks left
        data.subtractValue(ATTACKS, 1);

        // Remove it from the map if necessary
        if (data.getValue(ATTACKS) == 0) {
            activeEffects.remove(id);
        }

        // Apply the embedded effects
        data.getSkill().beginUsage();
        data.resolveNonTarget(event.getAttacker().getLocation());
        data.resolveTarget(event.getAttacker());
        data.getSkill().stopUsage();
    }

    /**
     * Clears embed data for a quitting player
     *
     * @param event event details
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        int id = event.getPlayer().getEntityId();
        if (activeEffects.containsKey(id)) {
            activeEffects.remove(id);
        }
    }

    /**
     * Applies default values for the mechanic attributes
     *
     * @param skill  skill to apply to
     * @param prefix prefix to add to the attribute
     */
    @Override
    public void applyDefaults(DynamicSkill skill, String prefix) {
        skill.checkDefault(prefix + ATTACKS, 1, 0);
        skill.checkDefault(prefix + DURATION, 5, 0);
        if (!skill.isSet(TYPE)) skill.setValue(TYPE, 0);
    }

    /**
     * @return names of the attributes used by the mechanic
     */
    @Override
    public String[] getAttributeNames() {
        return new String[] { ATTACKS, DURATION, CHANCE };
    }
}
