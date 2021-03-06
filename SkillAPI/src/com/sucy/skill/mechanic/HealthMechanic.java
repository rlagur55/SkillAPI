package com.sucy.skill.mechanic;

import com.sucy.skill.BukkitHelper;
import com.sucy.skill.api.PlayerSkills;
import com.sucy.skill.api.dynamic.DynamicSkill;
import com.sucy.skill.api.dynamic.IMechanic;
import com.sucy.skill.api.dynamic.Target;
import com.sucy.skill.api.util.effects.TimedEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

/**
 * Mechanic for healing all targets
 */
public class HealthMechanic implements IMechanic {

    private static final String HEALTH = "Health";

    private HashMap<String, Integer> playerBonuses = new HashMap<String, Integer>();
    private HashMap<Integer, Integer> mobBonuses = new HashMap<Integer, Integer>();

    /**
     * Grants bonus health to all targets
     *
     * @param player  player using the skill
     * @param data    data of the player using the skill
     * @param skill   skill being used
     * @param target  target type of the skill
     * @param targets targets for the effects
     * @return        true if there were targets, false otherwise
     */
    @Override
    public boolean resolve(Player player, PlayerSkills data, DynamicSkill skill, Target target, List<LivingEntity> targets) {

        // Grant health to all targets
        int level = data.getSkillLevel(skill.getName());
        int amount = (int)skill.getAttribute(HEALTH, target, level);
        for (LivingEntity t : targets) {

            // Players
            if (t instanceof Player) {
                Player p = (Player)t;
                data.getAPI().getPlayer(p.getName()).addMaxHealth(amount);
                playerBonuses.put(p.getName(), amount);
            }

            // Non-players
            else {
                double maxHealth = t.getMaxHealth() + amount;
                BukkitHelper.setMaxHealth(t, maxHealth);
                mobBonuses.put(t.getEntityId(), amount);
            }
        }

        return true;
    }

    /**
     * Sets default attributes for the skill
     *
     * @param skill  skill to apply to
     * @param prefix prefix to add to the name
     */
    @Override
    public void applyDefaults(DynamicSkill skill, String prefix) {
        skill.checkDefault(prefix + HEALTH, 5, 2);
    }

    /**
     * @return names of all attributes used by this mechanic
     */
    @Override
    public String[] getAttributeNames() {
        return new String[] {HEALTH};
    }

    /**
     * Health effect for players
     */
    private class PlayerHealthEffect extends TimedEffect {

        private PlayerSkills player;
        private double bonus;

        /**
         * Constructor
         *
         * @param player player reference
         * @param ticks  ticks to last for
         * @param bonus  bonus health to give
         */
        public PlayerHealthEffect(PlayerSkills player, int ticks, double bonus) {
            super(ticks);
            this.player = player;
            this.bonus = bonus;
        }

        /**
         * Adds maximum health to the player
         */
        @Override
        protected void setup() {
            player.addMaxHealth((int)bonus);
        }

        /**
         * Removes maximum health from the player
         */
        @Override
        protected void clear() {
            player.addMaxHealth(-(int)bonus);
        }
    }

    /**
     * Health effect for mobs
     */
    private class MobHealthEffect extends TimedEffect {

        private LivingEntity entity;
        private double bonus;

        /**
         * Constructor
         *
         * @param entity entity to apply to
         * @param ticks  number of ticks to last
         * @param bonus  amount of health to give
         */
        public MobHealthEffect(LivingEntity entity, int ticks, double bonus) {
            super(ticks);
            this.entity = entity;
            this.bonus = bonus;
        }

        /**
         * Gives health to the entity
         */
        @Override
        protected void setup() {
            double maxHealth = entity.getMaxHealth() + bonus;
            if (maxHealth < 1) {
                maxHealth = 1;
                bonus = maxHealth - entity.getMaxHealth();
            }
            BukkitHelper.setMaxHealth(entity, maxHealth);
        }

        /**
         * Removes the health bonus for the entity
         */
        @Override
        protected void clear() {
            BukkitHelper.setMaxHealth(entity, entity.getMaxHealth() - bonus);
        }
    }
}
