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

import java.util.SortedMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.citramc.anticheatrevolutions.AntiCheatRevolutions;
import com.citramc.anticheatrevolutions.check.CheckType;
import com.citramc.anticheatrevolutions.util.User;

/**
 * The conditional rule follows the same syntax as the traditional CS
 * conditional operator.<br />
 * (CONDITION) ? (TRUE RESULT) : (FALSE RESULT)<br />
 * Where CONDITION is a statement that will be evaluated as either <b>TRUE</b>
 * or <b>FALSE</b> and the results are actions to be taken for either outcome.
 * <br />
 * <br />
 * <p/>
 * Should you need additional actions, the system will recursively parse the
 * following values and handle appropriately. <br />
 * For instance, (CONDITION) ? (TRUE RESULT) : (CONDITION) ? (TRUE RESULT) :
 * (FALSE RESULT) is a valid rule <br />
 * <br />
 * <p/>
 * An example of a valid Conditional Rule:<br />
 * Check_SPIDER > 0 ? Player.KICK : null<br />
 * <i>The above statement would read 'If the spider check has been failed over
 * zero times, kick the player. Otherwise, do nothing.'</i>
 * <p/>
 * To see syntax for variables and functions that you may use, see
 * {@link com.citramc.anticheatrevolutions.util.rule.Rule}
 */
public class ConditionalRule extends Rule {

	private static final ScriptEngineManager FACTORY = new ScriptEngineManager();
	private static final ScriptEngine ENGINE = setupScriptEngine();

	private static final String TRUE_DELIMITER = "\\?";
	private static final String FALSE_DELIMITER = ":";
	private static final Type TYPE = Type.CONDITIONAL;

	public ConditionalRule(String string) {
		super(string, TYPE);
	}

	@Override
	public boolean check(User user, CheckType type) {
		if (ENGINE == null) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Script engine is unavailable.");
			return true;
		}

		try {
			SortedMap<String, Object> variables = getVariables(user, type);
			variables.forEach(ENGINE::put);

			String condition = getString().split(TRUE_DELIMITER)[0];
			Boolean result = (Boolean) ENGINE.eval(condition);

			String next = getString().split(TRUE_DELIMITER)[1].split(result ? TRUE_DELIMITER : FALSE_DELIMITER)[0];
			execute(next, user, type);

			return result;
		} catch (ScriptException e) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Error evaluating rule: " + e.getMessage());
		}
		return true;
	}

	private void execute(String command, User user, CheckType type) {
		if (command.equalsIgnoreCase("null") || command.equalsIgnoreCase("none")) {
			return;
		}

		if (TYPE.matches(command)) {
			new ConditionalRule(command).check(user, type);
		} else if (isVariableSet(command)) {
			String[] parts = command.split("=");
			setVariable(parts[0], parts[1], user);
		} else if (isFunction(command)) {
			doFunction(command, type, user);
		}
	}

	private static ScriptEngine setupScriptEngine() {
		double javaVersion = Double.parseDouble(System.getProperty("java.specification.version"));
		if (javaVersion < 15) {
			return FACTORY.getEngineByName("nashorn");
		} else {
			Bukkit.getConsoleSender().sendMessage(AntiCheatRevolutions.PREFIX + ChatColor.RED +
					"Java 15+ currently does not support Nashorn JavaScript engine. Conditional rules are disabled.");
			return null;
		}
	}
}
