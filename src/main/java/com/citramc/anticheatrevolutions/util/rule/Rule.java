/*
 * AntiCheatRevolutions for Bukkit and Spigot.
 * Copyright (c) 2012-2015 AntiCheat Team
 * Copyright (c) 2016-2022 Rammelkast
 * Copyright (c) 2024 CitraMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.citramc.anticheatrevolutions.util.rule;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.util.Group;
import com.citramc.anticheatrevolutions.util.User;
import com.citramc.anticheatrevolutions.util.Utilities;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A rule is a scriptable function used to customize the functions of
 * AntiCheat<br />
 * Rules are executed whenever a user's level rises.<br />
 * <br />
 * <p/>
 * There are various variables you can use to interact with AntiCheat through
 * your rules<br />
 * Variables are denoted with the type followed by a _ followed by the variable
 * name.<br />
 * For instance, a variable coming from a user who has used FLY 5 times and is
 * checked by a rule containing 'Check_FLY' will produce a value of 5.
 * <br />
 * <br />
 * <p/>
 * <b>Types of variables:</b><br />
 * <ul>
 * <li>Check
 * <ul>
 * <li>Contains all valid checks as listed in
 * {@link com.citramc.anticheatrevolutions.check.CheckType}</li>
 * <li>Will return the number of times this user has failed the given check</li>
 * <li><b>Example:</b> Check_SPRINT</li>
 * </ul>
 * </li>
 * <li>Player
 * <ul>
 * <li>Contains NAME, the name of the player</li>
 * <li>Contains LEVEL*, the player's current level</li>
 * <li>Contains GROUP*, the name of the player's current hack group</li>
 * <li>Contains CHECK, the check that was just failed</li>
 * <li>Contains GAMEMODE*, the player's current Game Mode (Survival, Creative,
 * Adventure)/li>
 * <li>Contains WORLD, the name of the world the player is in/li>
 * <li>Contains HEALTH*, the player's current health/li>
 * <li>A * denotes that this value can be set, for example Player_HEALTH =
 * 20.0/li>
 * </ul>
 * </li>
 * </ul>
 * <p/>
 * There are also functions you can use to execute an action within
 * AntiCheat<br />
 * Functions are denoted with the type followed by a period followed by the
 * function name.<br />
 * For instance, a rule containing Player.KICK will result in the user being
 * kicked.<br />
 * <br />
 * <br />
 * <p/>
 * <b>Types of functions:</b><br />
 * - Player: RESET, KICK, BAN, COMMAND[command] OR COMMAND[command1;command2]
 * <i>- when using commands <b>%player%</b> will be replaced by the player name,
 * <b>&world</b> will be replaced by the player's world,
 * and <b>&check</b> will be replaced by the check that caused this rule to be
 * run</i><br />
 * <br />
 * <br />
 * <p/>
 * The Rule class itself is not an functional rule setup,
 * it is inherited and made functional by different implementations of the rule
 * parser.<br />
 * The only current Rule implementation is the
 * {@link com.citramc.anticheatrevolutions.util.rule.ConditionalRule}
 */
public class Rule {

    private static final String VARIABLE_SET_REGEX = ".*_=.*";
    private static final String FUNCTION_REGEX = ".*\\..*";

    private String ruleString;
    private Type type;

    public enum Type {
        CONDITIONAL(".*\\?.*:.*", "com.citramc.anticheatrevolutions.util.rule.ConditionalRule");

        private String regex;
        private String className;

        Type(String regex, String className) {
            this.regex = regex;
            this.className = className;
        }

        public boolean matches(String s) {
            return s.matches(regex);
        }

        public Rule createInstance(String ruleString) {
            try {
                Class<?> clazz = Class.forName(className);
                Constructor<?> ctor = clazz.getConstructor(String.class);
                return (Rule) ctor.newInstance(ruleString);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to instantiate rule: " + className);
                e.printStackTrace();
                return null;
            }
        }
    }

    public Rule(String string, Type type) {
        this.ruleString = ruleString.trim().toLowerCase();
        this.type = type;
    }

    /**
     * Check if the rule passed or failed
     *
     * @param user the user to check
     * @return true if the rule has passed, false if failed
     */
    public boolean check(User user, CheckType type) {
        // Default value
        return true;
    }

    /**
     * Load a rule by its string value
     *
     * @param string the string value to load
     * @return an instance of Rule if an implementation exists to handle this rule,
     *         null if none are found
     */
    public static Rule load(String ruleString) {
        for (Type type : Type.values()) {
            if (type.matches(ruleString)) {
                return type.createInstance(ruleString);
            }
        }
        return null;
    }

    /**
     * Get the type of rule this is
     *
     * @return a {@link com.citramc.anticheatrevolutions.util.rule.Rule.Type}
     */
    public Type getType() {
        return type;
    }

    protected String getString() {
        return ruleString;
    }

    protected SortedMap<String, Object> getVariables(User user, CheckType checkType) {
        SortedMap<String, Object> map = new TreeMap<>();
        map.put("player_name", user.getPlayer().getName());
        map.put("player_level", user.getLevel());
        map.put("player_group", user.getGroup() != null ? user.getGroup().getName().toLowerCase() : "none");
        map.put("player_gamemode", user.getPlayer().getGameMode().name());
        map.put("player_world", user.getPlayer().getWorld().getName());
        map.put("player_health", user.getPlayer().getHealth());
        map.put("check_type", checkType.name().toLowerCase());

        return map;
    }

    protected void setVariable(String variable, String value, User user) {
        switch (variable) {
            case "player_level":
                if (Utilities.isInt(value)) {
                    user.setLevel(Integer.parseInt(value));
                }
                break;
            case "player_group":
                if (Utilities.isInt(value)) {
                    for (Group group : AntiCheatRevolutions.getManager().getConfiguration().getGroups().getGroups()) {
                        if (group.getName().equalsIgnoreCase(value)) {
                            user.setLevel(group.getLevel());
                        }
                    }
                }
                break;
            case "player_gamemode":
                try {
                    GameMode mode = GameMode.valueOf(value.toUpperCase());
                    user.getPlayer().setGameMode(mode);
                } catch (IllegalArgumentException ignored) {
                }
                break;
            case "player_health":
                if (Utilities.isDouble(value)) {
                    user.getPlayer().setHealth(Double.parseDouble(value));
                }
                break;
            default:
                break;
        }
    }

    protected void doFunction(String text, CheckType type, User user) {
        if (text.toLowerCase().startsWith("player")) {
            text = text.split("\\.")[1];
            List<String> action = new ArrayList<String>();
            action.add(text);
            AntiCheatRevolutions.getManager().getUserManager().execute(user, action, type);
        }
    }

    protected boolean isFunction(String string) {
        return string.matches(FUNCTION_REGEX);
    }

    protected boolean isVariableSet(String string) {
        return string.matches(VARIABLE_SET_REGEX);
    }

    @Override
    public String toString() {
        return "Rule[type=" + type + ", ruleString=" + ruleString + "]";
    }
}
