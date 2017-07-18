package timer;

import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import timer.exception.BadLogFileException;
import timer.exception.BadStatusFileException;
import timer.exception.NoTaskSpecifiedException;
import timer.exception.TimerAlreadyRunningException;
import timer.exception.TimerNotRunningException;
import timer.lib.DateRange;
import timer.lib.Formatted;
import timer.lib.MutuallyExclusiveOptionChecker;
import timer.lib.OptionFactory;

@AllArgsConstructor
public final class Main {
	private static final String HOME_DIRECTORY = System.getProperty("user.home");
	private static final String CURRENT_DIRECTORY = Paths.get(".").toAbsolutePath().normalize().toString();

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	private enum Command {
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

	private static final String CMDLINE_SYNTAX =
			"timer [-g <task> | -s | -c | -r <start> <end> | -t | -d <start> <end>]";
	private static final Options CMDLINE_OPTIONS =
			new Options().addOption(Command.Go.getOption()).addOption(Command.Stop.getOption())
					.addOption(Command.Check.getOption()).addOption(Command.Summary.getOption())
					.addOption(Command.Detail.getOption()).addOption(Command.Status.getOption());

	private static void error(String msg) {
		System.err.println(msg);
		System.exit(1);
	}

	private static void printHelp() {
		new HelpFormatter().printHelp(100, CMDLINE_SYNTAX, null, CMDLINE_OPTIONS, null, false);
		System.exit(0);
	}

	private static String[] getArg(CommandLine commandLine, Option option) {
		return new String[] { commandLine.getOptionValue(option.getOpt()) };
	}

	private static String[] getArgs(CommandLine commandLine, Option option, int expectedCount) {
		final String[] commandArgs = commandLine.getOptions()[0].getValues();
		if (commandArgs != null && commandArgs.length > expectedCount) {
			return null;
		}

		return commandArgs;
	}

	private static Main parse(String[] args) {
		final CommandLineParser argsParser = new PosixParser();
		final CommandLine commandLine;
		try {
			commandLine = argsParser.parse(CMDLINE_OPTIONS, args);
		} catch (org.apache.commons.cli.ParseException e) {
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
				return new Main(Command.Go, getArg(commandLine, option));
			case "s":
				return new Main(Command.Stop);
			case "c":
				return new Main(Command.Check);
			case "r":
				return new Main(Command.Summary, getArgs(commandLine, option, 2));
			case "d":
				return new Main(Command.Detail, getArgs(commandLine, option, 2));
			case "t":
				return new Main(Command.Status);
			}
		}

		return null;
	}

	@Getter
	private final Command command;

	@Getter
	private final String[] parameters;

	private Main(Command command) {
		this.command = command;
		this.parameters = null;
	}

	private String getParameter() {
		return parameters != null && parameters.length > 0 ? parameters[0] : null;
	}

	private DateRange getDateRangeParameter() {
		Date start = null, end = null;
		if (parameters != null && parameters.length > 0) {
			final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			try {
				start = dateFormat.parse(parameters[0]);
				if (parameters.length > 1) {
					end = dateFormat.parse(parameters[1]);
				}
			} catch (ParseException e) {
				error("Start/End dates must be in yyyy-MM-dd format");
			}

			if (start != null && end != null && start.getTime() > end.getTime()) {
				error("Start/End dates are wrong way round");
			}
		}

		return new DateRange(start, end);
	}

	private static void print(Collection<? extends Formatted> items) {
		for (Formatted item : items) {
			System.out.println(item.format());
		}
	}

	private static void print(Formatted item) {
		if (item != null) {
			System.out.println(item.format());
		}
	}

	public static void main(String[] args) {
		final Main timerApp = parse(args);
		if (timerApp == null) {
			printHelp();
		}

		final Timer timer = new Timer(CURRENT_DIRECTORY, HOME_DIRECTORY);

		final Command command = timerApp.getCommand();
		try {
			switch (command) {
			case Go:
				timer.start(timerApp.getParameter());
				break;
			case Stop:
				timer.stop();
				break;
			case Check:
				System.out.println(timer.check());
				break;
			case Summary: {
				final DateRange dateRange = timerApp.getDateRangeParameter();
				print(timer.summary(dateRange.getStart(), dateRange.getEnd()));
				break;
			}
			case Detail: {
				final DateRange dateRange = timerApp.getDateRangeParameter();
				print(timer.detail(dateRange.getStart(), dateRange.getEnd()));
				break;
			}
			case Status:
				print(timer.status());
				break;
			}
		} catch (BadLogFileException e) {
			error(String.format(
					"There's something wrong with the log file. Either delete %s in the current directory or try to fix it",
					Timer.LOG_FILE));
		} catch (BadStatusFileException e) {
			error(String.format("There's something wrong the status file %s in your home directory",
					Timer.STATUS_FILE));
		} catch (TimerAlreadyRunningException e) {
			error("The timer is already running");
		} catch (TimerNotRunningException e) {
			error("The timer is not running");
		} catch (NoTaskSpecifiedException e) {
			error("You need to specify a task");
		}
	}
}
