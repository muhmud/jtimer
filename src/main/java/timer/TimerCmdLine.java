package timer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import timer.lib.MutuallyExclusiveOptionChecker;
import timer.lib.OptionFactory;

@AllArgsConstructor
public final class TimerCmdLine {
	public static final String DATE_FORMAT = "yyyy-MM-dd";

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public enum Command {
		Go(new Option("g", "go", true, "Starts the timer for the specified task")),
		Stop(new Option("s", "stop", false, "Stops the timer")),
		Check(new Option("c", "check", false, "Checks the current status of the timer")),
		Summary(OptionFactory.create("r", "summary", 2, true, "Generates a summary by day for a date range")),
		Detail(OptionFactory.create("d", "detail", 2, true,
				"Generates a detailed report by day for a date range")),
		Status(new Option("t", "status", false, "Status of the timer"));

		@Getter
		private Option option;
	}

	private static final String CMDLINE_SYNTAX = "timer [-g <task> | -s | -c | -r | -t | -d]";
	private static final Options CMDLINE_OPTIONS =
			new Options().addOption(Command.Go.getOption()).addOption(Command.Stop.getOption())
					.addOption(Command.Check.getOption()).addOption(Command.Summary.getOption())
					.addOption(Command.Detail.getOption()).addOption(Command.Status.getOption());

	public static void printHelp() {
		new HelpFormatter().printHelp(CMDLINE_SYNTAX, CMDLINE_OPTIONS);
	}

	@Getter
	private final Command command;

	@Getter
	private final String arg;

	@Getter
	private final String[] args;

	private static String getArg(CommandLine commandLine, Option option) {
		return commandLine.getOptionValue(option.getOpt());
	}

	private static String[] getArgs(CommandLine commandLine, Option option) {
		return commandLine.getOptions()[0].getValues();
	}

	public static TimerCmdLine parse(String[] args) {
		final CommandLineParser argsParser = new PosixParser();
		final CommandLine commandLine;
		try {
			commandLine = argsParser.parse(CMDLINE_OPTIONS, args);
		} catch (ParseException e) {
			return null;
		}

		final Option option = new MutuallyExclusiveOptionChecker().check(commandLine, Command.Go.getOption(),
				Command.Stop.getOption(), Command.Check.getOption(), Command.Summary.getOption(),
				Command.Detail.getOption(), Command.Status.getOption());
		if (option == null) {
			return null;
		}

		if (option != null) {
			switch (option.getOpt()) {
			case "g":
				return new TimerCmdLine(Command.Go, getArg(commandLine, option), null);
			case "s":
				return new TimerCmdLine(Command.Stop, null, null);
			case "c":
				return new TimerCmdLine(Command.Check, null, null);
			case "r": {
				final String[] commandArgs = getArgs(commandLine, option);
				if (commandArgs != null && commandArgs.length > 2) {
					return null;
				}
				return new TimerCmdLine(Command.Summary, null, commandArgs);
			}
			case "d": {
				final String[] commandArgs = getArgs(commandLine, option);
				if (commandArgs != null && commandArgs.length > 2) {
					return null;
				}
				return new TimerCmdLine(Command.Detail, null, commandArgs);
			}
			case "t":
				return new TimerCmdLine(Command.Status, null, null);
			}
		}

		return null;
	}
}
